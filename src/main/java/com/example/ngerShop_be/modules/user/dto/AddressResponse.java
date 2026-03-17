package com.example.ngerShop_be.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String phoneNumber;
    private String street;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private String description;
    private boolean isDefault;
}

