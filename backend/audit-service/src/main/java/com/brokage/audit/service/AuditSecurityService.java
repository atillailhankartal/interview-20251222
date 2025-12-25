package com.brokage.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("auditSecurityService")
@RequiredArgsConstructor
@Slf4j
public class AuditSecurityService {

    public boolean isOwnCustomer(UUID customerId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String customerIdClaim = jwt.getClaimAsString("customer_id");
            if (customerIdClaim == null) {
                return false;
            }
            return UUID.fromString(customerIdClaim).equals(customerId);
        } catch (Exception e) {
            log.warn("Error checking customer ownership: {}", e.getMessage());
            return false;
        }
    }
}
