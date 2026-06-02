package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.persistence.repository.NotificationRepository;
import org.example.ortaback.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Планировщик in-app напоминаний (ТЗ §4.2, {@code EVENT_REMINDER}).
 *
 * <p>Периодически находит {@code ACTIVE}-события, стартующие в ближайшем окне,
 * и шлёт напоминание креатору и каждому {@code JOINED}-участнику.
 * Идемпотентность — через {@code NotificationRepository.existsBy...}, поэтому
 * повторные сканы того же окна не плодят дубликаты.
 *
 * <p>Выключается флагом {@code app.notifications.scheduler.enabled=false}
 * (по умолчанию включён). Интервал скана — {@code app.notifications.reminder-scan-ms}
 * (по умолчанию 5 минут).
 */
@Component
@ConditionalOnProperty(prefix = "app.notifications.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    /** За сколько минут до старта напоминаем. */
    private static final int REMINDER_LOOKAHEAD_MINUTES = 60;

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * {@code @Transactional} держит persistence context открытым на время
     * доступа к LAZY {@code event.getCreator()} и вложенных
     * {@code notificationService.create(...)} (они присоединяются к этой же tx).
     */
    @Scheduled(fixedDelayString = "${app.notifications.reminder-scan-ms:300000}")
    @Transactional
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusMinutes(REMINDER_LOOKAHEAD_MINUTES);

        List<Event> startingSoon = eventRepository.findActiveStartingBetween(now, horizon);
        if (startingSoon.isEmpty()) {
            return;
        }

        int sent = 0;
        for (Event event : startingSoon) {
            List<User> recipients = new ArrayList<>(participantRepository
                    .findUsersByEventIdAndStatus(event.getId(), ParticipantStatus.JOINED));

            User creator = event.getCreator();
            if (creator != null && recipients.stream()
                    .noneMatch(u -> Objects.equals(u.getId(), creator.getId()))) {
                recipients.add(creator);
            }

            String text = "Скоро начнётся: «" + event.getTitle() + "»";
            for (User recipient : recipients) {
                boolean already = notificationRepository.existsByRecipientIdAndEventIdAndType(
                        recipient.getId(), event.getId(), NotificationType.EVENT_REMINDER);
                if (!already) {
                    notificationService.create(recipient, NotificationType.EVENT_REMINDER, null, event, text);
                    sent++;
                }
            }
        }

        if (sent > 0) {
            log.debug("EVENT_REMINDER: отправлено {} напоминаний для {} событий",
                    sent, startingSoon.size());
        }
    }
}
