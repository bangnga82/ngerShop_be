package com.example.ngerShop_be.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserResponse {
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private Boolean gender;
    private LocalDate dateOfBirth;
    private List<AddressResponse> addresses;
}
