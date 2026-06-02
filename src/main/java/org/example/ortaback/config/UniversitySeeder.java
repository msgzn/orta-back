package org.example.ortaback.config;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.repository.UniversityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Сидер ВУЗов Казахстана для авто-привязки студентов по домену email.
 *
 * <p>Идемпотентен (как {@link CategorySeeder}): на каждом ВУЗе сначала
 * {@code findByName} — если есть, пропускаем; если нет, вставляем. Перезапуск
 * ничего не дублирует; удалённый вручную ВУЗ восстановится на следующем старте.
 *
 * <p>{@code domainPattern} = первый (основной) домен — для отображения; полный
 * набор кладётся в {@code university_domain}. Домены ГОЛЫЕ (без {@code @}),
 * матчинг суффиксный (см. {@code UniversityRepository#findAllByEmailHost}):
 * {@code @stu.kaznu.kz} попадёт в {@code kaznu.kz}.
 *
 * <p><b>ВАЖНО:</b> это домены САЙТОВ вузов. Суффикс-матч ловит студенческие
 * поддомены сайта (@stu.&lt;вуз&gt;). Но если у вуза студенческая почта на
 * ИНОМ домене, чем сайт, — добавить его в список доменов вручную.
 *
 * <p>Отключить: {@code application.seed.universities.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "application.seed.universities.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class UniversitySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UniversitySeeder.class);

    private final UniversityRepository universityRepository;

    /**
     * Канонический список вузов РК (домены сайтов). Имена — как в источнике;
     * менять без согласования с фронтом не стоит.
     */
    private static final List<Uni> UNIVERSITIES = List.of(
            new Uni("Казахский национальный университет им. аль-Фараби (КазНУ)",
                    List.of("kaznu.kz", "farabi.university")),
            new Uni("Евразийский национальный университет им. Л.Н. Гумилёва (ЕНУ)",
                    List.of("enu.kz")),
            new Uni("Satbayev University (КазНИТУ)",
                    List.of("satbayev.university")),
            new Uni("Nazarbayev University",
                    List.of("nu.edu.kz")),
            new Uni("Казахстанско-Британский технический университет (КБТУ)",
                    List.of("kbtu.edu.kz")),
            new Uni("KIMEP University",
                    List.of("kimep.kz")),
            new Uni("Казахский национальный медицинский университет им. С.Д. Асфендиярова (КазНМУ)",
                    List.of("kaznmu.edu.kz")),
            new Uni("Karaganda Buketov University (КарУ)",
                    List.of("buketov.edu.kz")),
            new Uni("Narxoz University",
                    List.of("narxoz.kz", "narxoz.edu.kz")),
            new Uni("Turan University",
                    List.of("turan.edu.kz", "turan-edu.kz")),
            new Uni("SDU University (им. Сулеймана Демиреля)",
                    List.of("sdu.edu.kz")),
            new Uni("Maqsut Narikbayev University (КАЗГЮУ)",
                    List.of("mnu.kz")),
            new Uni("Astana IT University (AITU)",
                    List.of("astanait.edu.kz")),
            new Uni("International IT University (IITU)",
                    List.of("iitu.edu.kz")));

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long before = universityRepository.count();
        for (Uni u : UNIVERSITIES) {
            ensure(u);
        }
        long after = universityRepository.count();
        long created = after - before;
        if (created > 0) {
            log.info("UniversitySeeder: ensured {} universities ({} new)", after, created);
        } else {
            log.info("UniversitySeeder: all {} universities already present", after);
        }
    }

    /** Создаёт ВУЗ, если его нет по имени. Домены — голые, нижний регистр. */
    private void ensure(Uni u) {
        if (universityRepository.findByName(u.name()).isPresent()) {
            return;
        }
        Set<String> domains = new LinkedHashSet<>();
        for (String d : u.domains()) {
            domains.add(d.trim().toLowerCase());
        }
        universityRepository.save(University.builder()
                .name(u.name())
                .domainPattern(u.domains().get(0).trim().toLowerCase())
                .domains(domains)
                .build());
    }

    private record Uni(String name, List<String> domains) {}
}
