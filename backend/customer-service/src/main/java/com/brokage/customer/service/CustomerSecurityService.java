package com.brokage.customer.service;

import com.brokage.customer.entity.Customer;
import com.brokage.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("customerSecurityService")
@RequiredArgsConstructor
public class CustomerSecurityService {

    private final CustomerRepository customerRepository;

    public boolean isOwnCustomer(UUID customerId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        String keycloakUserId = jwt.getSubject();

        return customerRepository.findById(customerId)
                .map(customer -> keycloakUserId.equals(customer.getKeycloakUserId()))
                .orElse(false);
    }
}
