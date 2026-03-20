package com.example.ngerShop_be.modules.user.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import com.example.ngerShop_be.common.exception.BadRequestException;
import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.constants.UserStatus;
import com.example.ngerShop_be.modules.user.dto.*;
import com.example.ngerShop_be.modules.user.entity.Address;
import com.example.ngerShop_be.modules.user.entity.Role;
import com.example.ngerShop_be.modules.user.entity.User;
import com.example.ngerShop_be.modules.user.repository.AddressRepository;
import com.example.ngerShop_be.modules.user.repository.RoleRepository;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AddressRepository addressRepository;
    private final AddressService addressService;

    @Value("${app.storage.avatarDir}")
    private String avatarDir;

    @Value("${app.storage.avatarUrlPrefix}")
    private String avatarUrlPrefix;

    @Value("${app.storage.avatarMaxBytes:5242880}")
    private long avatarMaxBytes;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AddressRepository addressRepository,
            AddressService addressService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.addressRepository = addressRepository;
        this.addressService = addressService;
    }

    public AdminUserResponse createAdmin(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRoles(Set.of(adminRole));
        return toAdminResponse(userRepository.save(user));
    }

    public UserResponse createUser(String email, UserRequest request) {
        User user = requireUser(email);
        applyUserUpdates(user, request);
        userRepository.save(user);

        if (request.getAddress() != null) {
            addressService.createAddress(email, request.getAddress());
        }
        return toResponse(user);
    }

    public UserResponse updateCurrentUser(String email, Long addressId, UserRequest request) {
        User user = requireUser(email);
        applyUserUpdates(user, request);
        userRepository.save(user);

        if (request.getAddress() != null) {
            if (addressId != null) {
                addressService.updateAddress(addressId, email, request.getAddress());
            } else {
                addressService.createAddress(email, request.getAddress());
            }
        }
        return toResponse(user);
    }

    public UserResponse getCurrentUser(String email) {
        User user = requireUser(email);
        return toResponse(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = requireUser(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public Page<AdminUserResponse> getAllUsers(String query, UserStatus status, Pageable pageable) {
        return userRepository.searchUsers(status, query, pageable)
                .map(this::toAdminResponse);
    }

    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toAdminResponse(user);
    }

    public AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setStatus(request.getStatus());
        userRepository.save(user);
        return toAdminResponse(user);
    }

    public AdminUserResponse resetPassword(Long userId, AdminResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return toAdminResponse(user);
    }

    public UserResponse uploadAvatar(String email, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }
        validateAvatar(avatar);
        User user = requireUser(email);

        String fileName = buildAvatarFileName(avatar.getOriginalFilename());
        Path baseDir = Paths.get(avatarDir).toAbsolutePath().normalize();
        Path target = baseDir.resolve(fileName).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BadRequestException("Invalid avatar file path");
        }

        try {
            Files.createDirectories(baseDir);
            Files.copy(avatar.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store avatar");
        }

        user.setAvatarUrl(avatarUrlPrefix + "/" + fileName);
        userRepository.save(user);
        return toResponse(user);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void applyUserUpdates(User user, UserRequest request) {
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            String nextPhone = request.getPhoneNumber();
            if (!nextPhone.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumber(nextPhone)) {
                throw new BadRequestException("Phone number already exists");
            }
            user.setPhoneNumber(nextPhone);
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
    }

    private UserResponse toResponse(User user) {
        List<AddressResponse> addresses = addressRepository.findByUser(user).stream()
                .map(this::toAddressResponse)
                .toList();
        return new UserResponse(
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getGender(),
                user.getDateOfBirth(),
                addresses
        );
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getPhoneNumber(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getZipCode(),
                address.getDescription(),
                Boolean.TRUE.equals(address.getIsDefault())
        );
    }

    private String buildAvatarFileName(String originalName) {
        String safeName = originalName == null ? "" : originalName;
        String extension = "";
        int dot = safeName.lastIndexOf('.');
        if (dot > -1 && dot < safeName.length() - 1) {
            extension = safeName.substring(dot).toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID() + extension;
    }

    private void validateAvatar(MultipartFile avatar) {
        if (avatar.getSize() > avatarMaxBytes) {
            throw new BadRequestException("Avatar file is too large");
        }
        String contentType = avatar.getContentType();
        if (contentType == null || !(contentType.equals("image/png")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/webp")
                || contentType.equals("image/gif"))) {
            throw new BadRequestException("Avatar file type is not supported");
        }
    }

    private AdminUserResponse toAdminResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getStatus(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}