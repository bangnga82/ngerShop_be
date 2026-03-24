package com.example.ngerShop_be.modules.user.service;

import java.util.List;

import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.modules.user.dto.AddressRequest;
import com.example.ngerShop_be.modules.user.dto.AddressResponse;
import com.example.ngerShop_be.modules.user.entity.Address;
import com.example.ngerShop_be.modules.user.entity.User;
import com.example.ngerShop_be.modules.user.repository.AddressRepository;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AddressService {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressResponse> getOwnAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return addressRepository.findByUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Address address = toEntity(request);
        address.setUser(user);
        if (request.isDefault()) {
            unsetDefaultForUser(user);
        }
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(Long addressId, Long userId, AddressRequest update) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Address existing = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        existing.setPhoneNumber(update.getPhoneNumber());
        existing.setStreet(update.getStreet());
        existing.setCity(update.getCity());
        existing.setState(update.getState());
        existing.setCountry(update.getCountry());
        existing.setZipCode(update.getZipCode());
        existing.setDescription(update.getDescription());
        if (update.isDefault()) {
            unsetDefaultForUser(existing.getUser());
            existing.setIsDefault(true);
        } else {
            existing.setIsDefault(false);
        }
        return toResponse(addressRepository.save(existing));
    }

    public AddressResponse getAddressById(Long addressId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        return toResponse(address);
    }

    public void deleteAddress(Long addressId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    private AddressResponse toResponse(Address address) {
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

    private Address toEntity(AddressRequest request) {
        Address address = new Address();
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setZipCode(request.getZipCode());
        address.setDescription(request.getDescription());
        address.setIsDefault(request.isDefault());
        return address;
    }

    private void unsetDefaultForUser(User user) {
        List<Address> addresses = addressRepository.findByUser(user);
        for (Address addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        }
    }
}
