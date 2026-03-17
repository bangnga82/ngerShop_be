package com.example.ngerShop_be.modules.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    private String phoneNumber;
    private String street;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private String description;
    private boolean isDefault;
}