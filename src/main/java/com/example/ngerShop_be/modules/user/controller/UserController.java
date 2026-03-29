package com.example.ngerShop_be.modules.user.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.MessageResponse;
import com.example.ngerShop_be.common.exception.BadRequestException;
import com.example.ngerShop_be.modules.user.dto.ChangePasswordRequest;
import com.example.ngerShop_be.modules.user.dto.UserRequest;
import com.example.ngerShop_be.modules.user.dto.UserResponse;
import com.example.ngerShop_be.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(GlobalResponse.ok(userService.createUser(requireUserId(auth), request)));
    }

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<UserResponse>> getMe(Authentication auth) {
        return ResponseEntity.ok(GlobalResponse.ok(userService.getCurrentUser(requireUserId(auth))));
    }

    @PutMapping
    public ResponseEntity<GlobalResponse<UserResponse>> updateCurrentUser(
            @RequestParam(name = "addressId", required = false) Long addressId,
            @Valid @RequestBody UserRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                GlobalResponse.ok(userService.updateCurrentUser(requireUserId(auth), addressId, request))
        );
    }

    @PutMapping("/upload")
    public ResponseEntity<GlobalResponse<UserResponse>> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            Authentication auth
    ) {
        return ResponseEntity.ok(GlobalResponse.ok(userService.uploadAvatar(requireUserId(auth), avatar)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<GlobalResponse<MessageResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth
    ) {
        userService.changePassword(requireUserId(auth), request);
        return ResponseEntity.ok(GlobalResponse.ok(new MessageResponse("Password updated")));
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthorized");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid user id");
        }
    }
}
