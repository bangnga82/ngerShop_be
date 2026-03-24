package com.example.ngerShop_be.modules.user.repository;

import com.example.ngerShop_be.common.constants.UserStatus;
import com.example.ngerShop_be.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    @Query("""
        select u from User u
        left join fetch u.roles
        where u.email = :email
        """)
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("""
        select u from User u
        left join fetch u.roles
        where u.username = :username
        """)
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("""
        select u from User u
        left join fetch u.roles
        where u.id = :id
        """)
    Optional<User> findByIdWithRoles(@Param("id") Long id);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
        select u from User u
        where (:status is null or u.status = :status)
        and (
            :query is null or :query = '' or
            lower(u.email) like lower(concat('%', :query, '%')) or
            lower(u.fullName) like lower(concat('%', :query, '%')) or
            lower(u.phoneNumber) like lower(concat('%', :query, '%'))
        )
        """)
    Page<User> searchUsers(
            @Param("status") UserStatus status,
            @Param("query") String query,
            Pageable pageable
    );
}
