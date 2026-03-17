package com.example.ngerShop_be.modules.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRequest {
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @Pattern(regexp = "^(\\+84|0)[1-9]\\d{8}$", message = "Invalid phone number")
    private String phoneNumber;

    private Boolean gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Valid
    private AddressRequest address;
}