package com.brokage.customer.entity;

import com.brokage.common.entity.BaseEntity;
import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_email", columnList = "email", unique = true),
        @Index(name = "idx_customer_identity_number", columnList = "identity_number", unique = true),
        @Index(name = "idx_customer_tier", columnList = "tier"),
        @Index(name = "idx_customer_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "identity_number", nullable = false, unique = true, length = 11)
    private String identityNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    @Builder.Default
    private CustomerTier tier = CustomerTier.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "keycloak_user_id", length = 36)
    private String keycloakUserId;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
