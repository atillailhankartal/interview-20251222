package com.brokage.customer.dto;

import com.brokage.common.enums.CustomerTier;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Identity number is required")
    @Size(min = 11, max = 11, message = "Identity number must be exactly 11 characters")
    @Pattern(regexp = "^[0-9]+$", message = "Identity number must contain only digits")
    private String identityNumber;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    private CustomerTier tier;
}
