package com.example.ngerShop_be.modules.user.repository;

import com.example.ngerShop_be.modules.user.entity.Address;
import com.example.ngerShop_be.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser(User user);
    Optional<Address> findByIdAndUser(Long id, User user);
}
