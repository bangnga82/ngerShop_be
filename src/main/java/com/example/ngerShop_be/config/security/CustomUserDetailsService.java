package com.example.ngerShop_be.config.security;

import com.example.ngerShop_be.common.constants.UserStatus;
import com.example.ngerShop_be.modules.user.entity.User;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = resolveUser(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new DisabledException("User account is locked");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(
                        user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                                .collect(Collectors.toSet())
                )
                .build();
    }

    private Optional<User> resolveUser(String username) {
        if (username != null && username.matches("\\d+")) {
            try {
                long id = Long.parseLong(username);
                return userRepository.findByIdWithRoles(id);
            } catch (NumberFormatException ignored) {
                // Fallback to email/username lookup
            }
        }
        return userRepository.findByEmailWithRoles(username)
                .or(() -> userRepository.findByUsernameWithRoles(username));
    }
}
