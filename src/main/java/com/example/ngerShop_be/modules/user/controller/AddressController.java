package com.example.ngerShop_be.modules.user.controller;

import com.example.ngerShop_be.common.response.ApiResponse;
import com.example.ngerShop_be.modules.user.dto.AddressRequest;
import com.example.ngerShop_be.modules.user.dto.AddressResponse;
import com.example.ngerShop_be.modules.user.service.AddressService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getOwnAddresses(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getOwnAddresses(requireEmail(auth))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @RequestBody AddressRequest address,
            Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.createAddress(requireEmail(auth), address)));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @PathVariable Long addressId,
            Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddressById(addressId, requireEmail(auth))));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long addressId,
            @RequestBody AddressRequest address,
            Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.updateAddress(addressId, requireEmail(auth), address)));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long addressId,
            Authentication auth
    ) {
        addressService.deleteAddress(addressId, requireEmail(auth));
        return ResponseEntity.ok(ApiResponse.ok(null, "Deleted"));
    }

    private String requireEmail(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return auth.getName();
    }
}
