package com.brokage.notification.entity;

import com.brokage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "notification_type", "channel"}),
       indexes = {
        @Index(name = "idx_preference_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "notification_type", nullable = false, length = 100)
    private String notificationType;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
