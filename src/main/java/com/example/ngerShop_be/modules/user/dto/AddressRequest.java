package com.example.ngerShop_be.modules.user.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    @NotBlank
    @Size(max = 50)
    private String phoneNumber;
    @NotBlank
    @Size(max = 255)
    private String street;
    @NotBlank
    @Size(max = 100)
    private String city;
    @NotBlank
    @Size(max = 100)
    private String state;
    @NotBlank
    @Size(max = 100)
    private String country;
    @NotBlank
    @Size(max = 20)
    private String zipCode;
    @Size(max = 500)
    private String description;
    private boolean isDefault;

    @JsonProperty("isDefault")
    public boolean isDefault() {
        return isDefault;
    }

    @JsonSetter("isDefault")
    @JsonAlias("default")
    public void setIsDefault(boolean value) {
        this.isDefault = value;
    }
}
