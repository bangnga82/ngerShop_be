package com.example.ngerShop_be.modules.notification.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.notification.dto.NotificationResponse;
import com.example.ngerShop_be.modules.notification.service.NotificationService;
import com.example.ngerShop_be.modules.notification.service.NotificationSseService;
import com.example.ngerShop_be.common.exception.BadRequestException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;
    NotificationSseService notificationSseService;

    @GetMapping
    public GlobalResponse<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication auth
    ) {
        Long userId = requireUserId(auth);
        String[] sortParams = sort.split(",", 2);
        String sortField = sortParams[0].trim();

        if (!sortField.equals("createdAt")) {
            throw new IllegalArgumentException("Invalid sort field. Allowed fields are: createdAt");
        }

        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));

        PageResponse<NotificationResponse> response = notificationService.getNotifications(userId, pageRequest);
        return GlobalResponse.ok(response);
    }

    @GetMapping("/unread-count")
    public GlobalResponse<Long> getUnreadCount(Authentication auth) {
        Long userId = requireUserId(auth);
        long count = notificationService.getUnreadCount(userId);
        return GlobalResponse.ok(count);
    }

    @PutMapping("/{id}/read")
    public GlobalResponse<String> markRead(@PathVariable Long id, Authentication auth) {
        Long userId = requireUserId(auth);
        notificationService.markRead(userId, id);
        return GlobalResponse.ok("Marked as read");
    }

    @PutMapping("/read-all")
    public GlobalResponse<String> markAllRead(Authentication auth) {
        Long userId = requireUserId(auth);
        notificationService.markAllRead(userId);
        return GlobalResponse.ok("Marked all as read");
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        Long userId = requireUserId(auth);
        return notificationSseService.subscribe(userId);
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
