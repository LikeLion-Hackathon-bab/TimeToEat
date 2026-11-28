package com.example.timetoeat.domain.notification.repository;

import com.example.timetoeat.domain.notification.entity.Notification;
import com.example.timetoeat.domain.notification.entity.NotificationCategory;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdAndCategoryOrderByIdDesc(Long receiverId, NotificationCategory category, Pageable pageable);

    /** 동네 소식: 전체 공개(receiver null) + 나에게 온 소식 */
    @Query("""
            select n from Notification n
            where n.category = com.example.timetoeat.domain.notification.entity.NotificationCategory.LOCAL_NEWS
              and (n.receiverId is null or n.receiverId = :receiverId)
              and n.type in :types
            order by n.id desc
            """)
    List<Notification> findLocalNews(@Param("receiverId") Long receiverId,
                                     @Param("types") Collection<NotificationType> types,
                                     Pageable pageable);

    long countByReceiverIdAndCategoryAndReadFalse(Long receiverId, NotificationCategory category);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    boolean existsByDedupKey(String dedupKey);

    /** 내 일반 알림 전부 읽음(동네 소식 제외). */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.read = true, n.readAt = :now
            where n.receiverId = :receiverId
              and n.category = com.example.timetoeat.domain.notification.entity.NotificationCategory.ALARM
              and n.read = false
            """)
    int markAllAlarmsRead(@Param("receiverId") Long receiverId, @Param("now") LocalDateTime now);
}
