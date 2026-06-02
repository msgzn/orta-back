package org.example.ortaback.config;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.geo.GeoUtils;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventObservation;
import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.EventReview;
import org.example.ortaback.persistence.models.TrustScoreEvent;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.UserFollow;
import org.example.ortaback.persistence.models.UserInterest;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.ReviewType;
import org.example.ortaback.persistence.models.enums.Sentiment;
import org.example.ortaback.persistence.models.enums.TrustReason;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.repository.CategoryRepository;
import org.example.ortaback.persistence.repository.EventObservationRepository;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.persistence.repository.EventReviewRepository;
import org.example.ortaback.persistence.repository.TrustScoreEventRepository;
import org.example.ortaback.persistence.repository.UniversityRepository;
import org.example.ortaback.persistence.repository.UserFollowRepository;
import org.example.ortaback.persistence.repository.UserInterestRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Сидер ДЕМО-данных для презентаций: студенты Алматы, события на карте,
 * участники, наблюдатели, чаты, подписки и отзывы/репутация — всё через
 * настоящий доменный слой (сущности + репозитории + {@link ChatService}),
 * без сырых INSERT'ов в обход модели.
 *
 * <p><b>Opt-in</b>: в отличие от {@link CategorySeeder}/{@link UniversitySeeder}
 * (которые засеивают справочники всегда), демо-данные включаются ТОЛЬКО явно —
 * {@code application.seed.demo.enabled=true}. По умолчанию выключено, поэтому
 * прод/тесты не загрязняются.
 *
 * <p>Запускается на {@link ApplicationReadyEvent} — гарантированно ПОСЛЕ всех
 * {@code ApplicationRunner}'ов, значит категории и вузы к этому моменту уже
 * засижены.
 *
 * <p><b>Идемпотентность</b>: если первый демо-аккаунт уже существует, сидер
 * молча выходит — повторный старт ничего не дублирует.
 *
 * <p>Все демо-пользователи имеют общий пароль (см. {@link #DEMO_PASSWORD}) —
 * залогиниться можно любым email'ом, который печатается в лог на старте.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "application.seed.demo.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Общий пароль всех демо-аккаунтов (логин — email). */
    private static final String DEMO_PASSWORD = "Orta2026!";

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final EventObservationRepository observationRepository;
    private final UserFollowRepository followRepository;
    private final EventReviewRepository reviewRepository;
    private final TrustScoreEventRepository trustScoreEventRepository;
    private final UserInterestRepository interestRepository;
    private final ChatService chatService;
    private final PasswordEncoder passwordEncoder;

    // Детерминированный рандом — один и тот же демо-набор при каждом чистом старте.
    private final Random random = new Random(42);

    // -----------------------------------------------------------------------
    //  Точка входа
    // -----------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (userRepository.findByEmail(USERS.get(0).email()).isPresent()) {
            log.info("DemoDataSeeder: демо-данные уже присутствуют — пропускаю");
            return;
        }
        if (categoryRepository.count() == 0) {
            log.warn("DemoDataSeeder: нет категорий (CategorySeeder выключен?) — отменяю демо-сид");
            return;
        }

        Map<String, Category> catByName = new LinkedHashMap<>();
        for (Category c : categoryRepository.findAll()) {
            catByName.put(c.getName(), c);
        }
        Map<String, University> uniByName = new LinkedHashMap<>();
        for (University u : universityRepository.findAll()) {
            uniByName.put(u.getName(), u);
        }

        List<User> users = seedUsers(uniByName);
        seedInterests(users, catByName);
        Map<Event, List<User>> joinedByEvent = seedEvents(users, catByName);
        seedFollows(users);
        seedChat(joinedByEvent);
        seedReviewsAndReputation(joinedByEvent);

        log.info("DemoDataSeeder: готово — {} пользователей, {} событий. Демо-вход: {} / {}",
                users.size(), joinedByEvent.size(), USERS.get(0).email(), DEMO_PASSWORD);
    }

    // -----------------------------------------------------------------------
    //  Пользователи
    // -----------------------------------------------------------------------

    private List<User> seedUsers(Map<String, University> uniByName) {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        List<User> created = new ArrayList<>();
        for (DemoUser du : USERS) {
            User u = User.builder()
                    .firstName(du.first())
                    .lastName(du.last())
                    .nickname(du.nick())
                    .email(du.email())
                    .hashedPassword(hash)
                    .phoneNumber(du.phone())
                    .avatarUrl("https://i.pravatar.cc/300?img=" + du.avatar())
                    .university(uniByName.get(du.uni()))
                    .trustScore(du.trust())
                    .role(UserRole.USER)
                    .language("ru")
                    .lastKnownLocation(GeoUtils.point(du.lat(), du.lng()))
                    .locationUpdatedAt(LocalDateTime.now())
                    .build();
            created.add(userRepository.save(u));
        }
        return created;
    }

    private void seedInterests(List<User> users, Map<String, Category> catByName) {
        String[] roots = {
                "Спорт", "Интеллектуальные активности", "Творческие активности",
                "Образовательные встречи", "Досуг", "Путешествия и походы",
                "Кулинария и совместные ужины", "Волонтёрство", "Онлайн-игры"
        };
        for (int i = 0; i < users.size(); i++) {
            int[] picks = {i % roots.length, (i + 3) % roots.length, (i + 5) % roots.length};
            for (int p : picks) {
                Category c = catByName.get(roots[p]);
                if (c == null) {
                    continue;
                }
                // (user, category) уникальна — picks подобраны различными.
                interestRepository.save(UserInterest.builder()
                        .user(users.get(i)).category(c).build());
            }
        }
    }

    // -----------------------------------------------------------------------
    //  События + участники + наблюдатели
    // -----------------------------------------------------------------------

    private Map<Event, List<User>> seedEvents(List<User> users, Map<String, Category> catByName) {
        Category fallback = categoryRepository.findAll().get(0);
        LocalDateTime now = LocalDateTime.now();
        Map<Event, List<User>> joinedByEvent = new LinkedHashMap<>();

        for (DemoEvent de : EVENTS) {
            Venue v = VENUES[de.venue()];
            User creator = users.get(de.creator());
            Category category = catByName.getOrDefault(de.category(), fallback);

            Event event = eventRepository.save(Event.builder()
                    .creator(creator)
                    .category(category)
                    .status(de.status())
                    .title(de.title())
                    .description(de.description())
                    .placeName(v.name())
                    .address(v.address())
                    .location(GeoUtils.point(v.lat(), v.lng()))
                    .locationPrecision(LocationPrecision.EXACT)
                    .minParticipants(Math.min(3, de.maxParticipants()))
                    .maxParticipants(de.maxParticipants())
                    .startTime(now.plusHours(de.startOffsetHours()))
                    .timeDuration(de.durationMin())
                    .pricePerPerson(de.price() == null ? null : new BigDecimal(de.price()))
                    .coverImageUrl("https://picsum.photos/seed/orta-ev-" + de.coverSeed() + "/800/450")
                    .startedAt(de.status() == EventStatus.DONE ? now.plusHours(de.startOffsetHours()) : null)
                    .build());

            // Состав: создатель + случайное подмножество остальных, все JOINED.
            List<User> others = new ArrayList<>(users);
            others.remove(creator);
            java.util.Collections.shuffle(others, random);
            int extra = Math.min(de.joined() - 1, others.size());

            List<User> joined = new ArrayList<>();
            joined.add(creator);
            participantRepository.save(EventParticipant.builder()
                    .event(event).user(creator).status(ParticipantStatus.JOINED).build());
            for (int i = 0; i < extra; i++) {
                User u = others.get(i);
                joined.add(u);
                participantRepository.save(EventParticipant.builder()
                        .event(event).user(u).status(ParticipantStatus.JOINED).build());
            }

            // Наблюдатели: пара пользователей вне состава следят за предстоящим событием.
            if (de.status() == EventStatus.ACTIVE) {
                int observers = Math.min(3, others.size() - extra);
                for (int i = 0; i < observers; i++) {
                    observationRepository.save(EventObservation.builder()
                            .event(event).user(others.get(extra + i)).build());
                }
            }

            joinedByEvent.put(event, joined);
        }
        return joinedByEvent;
    }

    // -----------------------------------------------------------------------
    //  Подписки (соц-граф)
    // -----------------------------------------------------------------------

    private void seedFollows(List<User> users) {
        int n = users.size();
        for (int i = 0; i < n; i++) {
            // Каждый подписан на 4 «соседей» по кругу — связный, но не полный граф.
            for (int step : new int[]{1, 2, 4, 7}) {
                User follower = users.get(i);
                User followee = users.get((i + step) % n);
                if (!follower.equals(followee)) {
                    followRepository.save(UserFollow.builder()
                            .follower(follower).followee(followee).build());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Чат (через ChatService → корректные маркеры прочтения и unread-бейджи)
    // -----------------------------------------------------------------------

    private void seedChat(Map<Event, List<User>> joinedByEvent) {
        List<String> lines = List.of(
                "Всем привет! Кто идёт?",
                "Я в деле 🙌",
                "Во сколько собираемся у входа?",
                "Давайте за 15 минут до начала",
                "Окей, буду вовремя",
                "Можно с другом?",
                "Конечно, мест хватает",
                "Супер, до встречи!"
        );
        int seededChats = 0;
        for (Map.Entry<Event, List<User>> e : joinedByEvent.entrySet()) {
            Event event = e.getKey();
            List<User> joined = e.getValue();
            // Только активные события с достаточным составом, и не все подряд.
            if (event.getStatus() != EventStatus.ACTIVE || joined.size() < 4 || seededChats >= 5) {
                continue;
            }
            int speakers = Math.min(4, joined.size());
            int count = 5 + random.nextInt(3); // 5..7 сообщений
            for (int i = 0; i < count; i++) {
                User sender = joined.get(i % speakers);
                chatService.sendMessage(event.getId(), lines.get(i % lines.size()), sender);
            }
            seededChats++;
        }
    }

    // -----------------------------------------------------------------------
    //  Отзывы + история репутации (для прошедших DONE-событий)
    // -----------------------------------------------------------------------

    private void seedReviewsAndReputation(Map<Event, List<User>> joinedByEvent) {
        for (Map.Entry<Event, List<User>> e : joinedByEvent.entrySet()) {
            Event event = e.getKey();
            if (event.getStatus() != EventStatus.DONE) {
                continue;
            }
            List<User> joined = e.getValue();
            User promoter = joined.get(0);
            int reviewed = Math.min(5, joined.size() - 1);

            // Площадка (VENUE) — один отзыв от первого участника.
            if (joined.size() > 1) {
                reviewRepository.save(EventReview.builder()
                        .event(event).reviewer(joined.get(1)).target(null)
                        .type(ReviewType.VENUE).starRating(5)
                        .comment("Отличная локация, всё удобно").sentiment(Sentiment.POSITIVE)
                        .trustDelta(0).build());
            }

            for (int i = 1; i <= reviewed; i++) {
                User participant = joined.get(i);
                // Один участник в первом DONE-событии — «не пришёл» (демонстрируем минус).
                boolean noShow = (i == 1 && event.getTitle().contains("прошедш"));

                // Промоутер оценивает участника.
                reviewRepository.save(EventReview.builder()
                        .event(event).reviewer(promoter).target(participant)
                        .type(ReviewType.PARTICIPANT_BY_PROMOTER)
                        .came(!noShow)
                        .comment(noShow ? "Не пришёл, не предупредил" : "Активный участник, спасибо!")
                        .sentiment(noShow ? Sentiment.NEGATIVE : Sentiment.POSITIVE)
                        .trustDelta(noShow ? -10 : 5)
                        .build());
                trustScoreEventRepository.save(TrustScoreEvent.builder()
                        .user(participant).event(event)
                        .delta(noShow ? -10 : 5)
                        .reason(noShow ? TrustReason.PARTICIPANT_NO_SHOW : TrustReason.POSITIVE_REVIEW)
                        .build());

                // Участник оценивает промоутера (пришедшие — положительно).
                if (!noShow) {
                    reviewRepository.save(EventReview.builder()
                            .event(event).reviewer(participant).target(promoter)
                            .type(ReviewType.PROMOTER_BY_PARTICIPANT)
                            .came(true)
                            .comment("Хорошо организовано, рекомендую!")
                            .sentiment(Sentiment.POSITIVE)
                            .trustDelta(5)
                            .build());
                    trustScoreEventRepository.save(TrustScoreEvent.builder()
                            .user(promoter).event(event)
                            .delta(5).reason(TrustReason.POSITIVE_REVIEW)
                            .build());
                }
            }
        }
    }

    // =======================================================================
    //  Демо-данные (статические таблицы)
    // =======================================================================

    private static final String KAZNU    = "Казахский национальный университет им. аль-Фараби (КазНУ)";
    private static final String SATBAYEV  = "Satbayev University (КазНИТУ)";
    private static final String KBTU      = "Казахстанско-Британский технический университет (КБТУ)";
    private static final String AITU      = "Astana IT University (AITU)";
    private static final String KIMEP     = "KIMEP University";
    private static final String IITU      = "International IT University (IITU)";
    private static final String NARXOZ    = "Narxoz University";
    private static final String KAZGUU    = "Maqsut Narikbayev University (КАЗГЮУ)";
    private static final String NU         = "Nazarbayev University";

    private record DemoUser(String first, String last, String nick, String email, String phone,
                            String uni, float trust, int avatar, double lat, double lng) {}

    private static final List<DemoUser> USERS = List.of(
            new DemoUser("Айгерим", "Нурланова", "aigerim",  "aigerim.nurlanova@kaznu.kz",   "+77010000001", KAZNU,   88f, 5,  43.238, 76.945),
            new DemoUser("Данияр",  "Сапаров",   "dan_sap",  "daniyar.saparov@satbayev.university", "+77010000002", SATBAYEV, 75f, 12, 43.236, 76.928),
            new DemoUser("Алина",   "Ким",       "alina_k",  "alina.kim@kbtu.edu.kz",        "+77010000003", KBTU,    92f, 9,  43.218, 76.927),
            new DemoUser("Тимур",   "Әбіл",      "timur_a",  "timur.abil@astanait.edu.kz",   "+77010000004", AITU,    67f, 13, 43.252, 76.957),
            new DemoUser("Жанель",  "Қуаныш",    "zhanel",   "zhanel.kuanysh@kimep.kz",      "+77010000005", KIMEP,   80f, 16, 43.211, 76.892),
            new DemoUser("Ислам",   "Тулеген",   "islam_t",  "islam.tulegen@iitu.edu.kz",    "+77010000006", IITU,    71f, 15, 43.262, 76.940),
            new DemoUser("Камила",  "Ахметова",  "kamila",   "kamila.akhmetova@narxoz.kz",   "+77010000007", NARXOZ,  95f, 20, 43.233, 76.956),
            new DemoUser("Нурлан",  "Жумабек",   "nurlan_zh","nurlan.zhumabek@kaznu.kz",     "+77010000008", KAZNU,   60f, 33, 43.199, 76.887),
            new DemoUser("Дильназ", "Серик",     "dilnaz",   "dilnaz.serik@mnu.kz",          "+77010000009", KAZGUU,  84f, 24, 43.240, 76.915),
            new DemoUser("Арман",   "Болат",     "arman_b",  "arman.bolat@satbayev.university", "+77010000010", SATBAYEV, 78f, 51, 43.300, 76.890),
            new DemoUser("Аружан",  "Маратова",  "aruzhan",  "aruzhan.maratova@kbtu.edu.kz", "+77010000011", KBTU,    90f, 25, 43.221, 76.918),
            new DemoUser("Ерасыл",  "Қасым",     "erasyl",   "yerassyl.kasym@astanait.edu.kz", "+77010000012", AITU,    55f, 53, 43.232, 76.974),
            new DemoUser("Мадина",  "Оспан",     "madina_o", "madina.ospan@kimep.kz",        "+77010000013", KIMEP,   86f, 26, 43.157, 77.058),
            new DemoUser("Бекзат",  "Нұрлан",    "bekzat",   "bekzat.nurlan@iitu.edu.kz",    "+77010000014", IITU,    73f, 60, 43.258, 76.953),
            new DemoUser("Сабина",  "Жақсыбек",  "sabina",   "sabina.zhaksybek@kaznu.kz",    "+77010000015", KAZNU,   81f, 32, 43.260, 76.945),
            new DemoUser("Олжас",   "Тоқтар",    "olzhas",   "olzhas.toktar@nu.edu.kz",      "+77010000016", NU,      68f, 68, 43.224, 76.927)
    );

    private record Venue(String name, String address, double lat, double lng) {}

    private static final Venue[] VENUES = {
            new Venue("Парк Первого Президента", "пр. Аль-Фараби, Алматы", 43.199, 76.887),
            new Venue("Центральный стадион", "ул. Сатпаева 29, Алматы", 43.236, 76.928),
            new Venue("Almaty Arena", "ул. Момышулы, Алматы", 43.300, 76.890),
            new Venue("ЦПКиО (Парк Горького)", "ул. Гагарина 1, Алматы", 43.252, 76.957),
            new Venue("Кок-Тобе", "Кок-Тобе, Алматы", 43.232, 76.974),
            new Venue("Медеу", "Медеу, Алматы", 43.157, 77.058),
            new Venue("Шымбулак", "Шымбулак, Алматы", 43.127, 77.087),
            new Venue("Esentai Mall", "пр. Аль-Фараби 77/8, Алматы", 43.218, 76.927),
            new Venue("Dostyk Plaza", "пр. Достык 111, Алматы", 43.233, 76.956),
            new Venue("Кампус КазНУ", "пр. Аль-Фараби 71, Алматы", 43.211, 76.892),
            new Venue("Площадь Республики", "пл. Республики, Алматы", 43.239, 76.945),
            new Venue("Ботанический сад", "ул. Тимирязева 36Д, Алматы", 43.221, 76.918),
            new Venue("Mega Park", "ул. Макатаева 127, Алматы", 43.262, 76.940),
            new Venue("AlmaU", "пр. Абая 47, Алматы", 43.240, 76.915),
            new Venue("Арбат (Жибек Жолы)", "ул. Жибек Жолы, Алматы", 43.260, 76.945)
    };

    private record DemoEvent(String title, String category, int venue, int creator, int joined,
                             EventStatus status, long startOffsetHours, int durationMin,
                             int maxParticipants, String price, int coverSeed, String description) {}

    private static final List<DemoEvent> EVENTS = List.of(
            new DemoEvent("Футбол 5x5 на стадионе", "Футбол", 1, 0, 8, EventStatus.ACTIVE, 48, 90, 14, null, 101,
                    "Играем 5 на 5, нужны ещё игроки. Уровень любой, главное — настрой!"),
            new DemoEvent("Утренняя пробежка в парке", "Бег", 0, 1, 6, EventStatus.ACTIVE, 20, 60, 20, null, 102,
                    "Лёгкая пробежка 5 км по парку. Темп комфортный, берём воду."),
            new DemoEvent("Турнир по баскетболу 3x3", "Баскетбол", 2, 2, 9, EventStatus.ACTIVE, 72, 120, 12, "1000", 103,
                    "Стритбол-турнир. Собираем команды по 3 человека, призы победителям."),
            new DemoEvent("Шахматный клуб: блиц-турнир", "Шахматы", 8, 3, 5, EventStatus.ACTIVE, 50, 180, 16, null, 104,
                    "Блиц 5+0, швейцарка на 7 туров. Новичкам тоже рады."),
            new DemoEvent("Квиз: проверь свой мозг", "Квизы", 7, 4, 12, EventStatus.ACTIVE, 96, 120, 30, "1500", 105,
                    "Командный квиз на 7 раундов. Собирай команду 4–6 человек."),
            new DemoEvent("Фотопрогулка по Кок-Тобе", "Фотопрогулки", 4, 5, 4, EventStatus.ACTIVE, 120, 150, 10, null, 106,
                    "Снимаем закат над городом. Любая камера, даже телефон."),
            new DemoEvent("Хакатон выходного дня", "Хакатоны", 13, 6, 14, EventStatus.ACTIVE, 144, 600, 40, null, 107,
                    "48 часов, команды до 4 человек. Тема — городские сервисы."),
            new DemoEvent("English Speaking Club", "Языковые клубы", 3, 1, 7, EventStatus.ACTIVE, 46, 90, 15, null, 108,
                    "Разговорный английский, уровень B1+. Тема недели — путешествия."),
            new DemoEvent("CS2: 5x5 в клубе", "CS2", 12, 7, 9, EventStatus.ACTIVE, 28, 180, 10, "2000", 109,
                    "Катаем 5х5, нужен ещё стак. Discord обязателен."),
            new DemoEvent("Совместный ужин: готовим пасту", "Кулинария и совместные ужины", 8, 8, 5, EventStatus.ACTIVE, 76, 180, 8, "3000", 110,
                    "Готовим карбонару вместе, продукты делим. Уютный вечер."),
            new DemoEvent("Велопрогулка по терренкуру", "Прогулки", 5, 9, 6, EventStatus.ACTIVE, 100, 200, 20, null, 111,
                    "Катаемся от Медеу вверх. Свой велосипед и шлем."),
            new DemoEvent("Настолки: Мафия и не только", "Настольные игры", 7, 10, 8, EventStatus.ACTIVE, 52, 180, 16, null, 112,
                    "Мафия, Codenames, Каркассон. Чай и печеньки от организаторов."),
            new DemoEvent("Теннис: парные игры", "Теннис", 1, 2, 4, EventStatus.ACTIVE, 70, 120, 8, "2500", 113,
                    "Парный теннис, нужны игроки уровня средний+. Корты бронируем."),
            new DemoEvent("Поход на Бутаковку", "Путешествия и походы", 6, 11, 10, EventStatus.ACTIVE, 168, 480, 25, null, 114,
                    "Однодневный поход к водопаду. Удобная обувь и перекус с собой."),
            new DemoEvent("Волонтёрство: уборка парка", "Волонтёрство", 0, 12, 14, EventStatus.ACTIVE, 110, 240, 50, null, 115,
                    "Собираем мусор, перчатки и мешки выдаём. После — общий чай."),
            new DemoEvent("Концерт инди-групп", "Концерты", 14, 13, 16, EventStatus.ACTIVE, 150, 180, 60, "4000", 116,
                    "Вечер живой музыки, выступают 4 местные группы."),
            new DemoEvent("Утренняя йога (прошедшее)", "Прогулки", 3, 4, 8, EventStatus.DONE, -72, 60, 15, null, 117,
                    "Спокойная практика на свежем воздухе. Коврики свои."),
            new DemoEvent("Футбольный матч (прошедший)", "Футбол", 1, 0, 12, EventStatus.DONE, -120, 90, 14, null, 118,
                    "Товарищеский матч 6х6. Было жарко!"),
            new DemoEvent("Квиз-вечер (прошедший)", "Квизы", 7, 4, 16, EventStatus.DONE, -168, 120, 30, "1500", 119,
                    "Большой квиз на 12 команд. Спасибо всем за участие!"),
            new DemoEvent("Лыжный выезд на Шымбулак (отменён)", "Путешествия и походы", 6, 9, 3, EventStatus.CANCELED, 96, 300, 20, "5000", 120,
                    "Выезд отменён из-за погоды, перенесём на следующую неделю.")
    );
}
