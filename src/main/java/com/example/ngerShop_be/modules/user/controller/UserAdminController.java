package com.example.ngerShop_be.modules.user.controller;


import com.example.ngerShop_be.common.constants.UserStatus;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.user.dto.AdminResetPasswordRequest;
import com.example.ngerShop_be.modules.user.dto.AdminUserResponse;
import com.example.ngerShop_be.modules.user.dto.UpdateUserStatusRequest;
import com.example.ngerShop_be.modules.user.entity.User;
import com.example.ngerShop_be.modules.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<AdminUserResponse>> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(GlobalResponse.ok(
                userService.createAdmin(request.getEmail(), request.getPassword(), request.getFullName())
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<PageResponse<AdminUserResponse>>> listUsers(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "status", required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AdminUserResponse> page = userService.getAllUsers(query, status, pageable);
        return ResponseEntity.ok(GlobalResponse.ok(toPageResponse(page)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<AdminUserResponse>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(GlobalResponse.ok(userService.getUserById(userId)));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<AdminUserResponse>> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(GlobalResponse.ok(userService.updateUserStatus(userId, request)));
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<AdminUserResponse>> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        return ResponseEntity.ok(GlobalResponse.ok(userService.resetPassword(userId, request)));
    }

    @Getter
    @Setter
    public static class CreateAdminRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String fullName;
    }

    private PageResponse<AdminUserResponse> toPageResponse(Page<AdminUserResponse> page) {
        return new PageResponse<>(
                page.getContent(),           // 1. content (List)
                page.getTotalPages(),        // 2. totalPages (int)
                page.getTotalElements(),     // 3. totalElements (long) - Chỗ này bạn đang truyền nhầm Number
                page.getNumber(),            // 4. page (int)
                page.getSize(),              // 5. size (int)
                page.getNumberOfElements(),  // 6. numberOfElements (int) - Bạn đang thiếu cái này
                page.isFirst(),              // 7. isFirst (boolean)
                page.isLast(),               // 8. isLast (boolean)
                page.hasNext(),              // 9. hasNext (boolean)
                page.hasPrevious()
        );
    }
}
