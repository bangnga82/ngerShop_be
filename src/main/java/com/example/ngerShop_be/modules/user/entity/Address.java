package com.example.ngerShop_be.modules.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    private String street;
    private String city;
    private String state;
    private String country;

    @Column(name = "zip_code")
    private String zipCode;

    private String description;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;
}