package com.masukibooks.service;

import com.masukibooks.entity.User;
// import com.masukibooks.entity.Address;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
// import com.masukibooks.repository.AddressRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    // private final AddressRepository addressRepository;

    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User updateProfile(UUID userId, User updates) {
        User user = getProfile(userId);
        if (updates.getFirstName() != null)
            user.setFirstName(updates.getFirstName());
        if (updates.getLastName() != null)
            user.setLastName(updates.getLastName());
        if (updates.getProfession() != null)
            user.setProfession(updates.getProfession());
        if (updates.getPreferredLanguage() != null)
            user.setPreferredLanguage(updates.getPreferredLanguage());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword,
            org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User user = getProfile(userId);
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.save(user);
    }

    // public List<Address> getAddresses(UUID userId) {
    // return addressRepository.findByUserUserId(userId);
    // }

    // @Transactional
    // public Address addAddress(UUID userId, Address address) {
    // User user = getProfile(userId);
    // address.setUser(user);
    // return addressRepository.save(address);
    // }

    @Transactional
    // public Address updateAddress(UUID userId, UUID addressId, Address updates) {
    // Address address = addressRepository.findById(addressId)
    // .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    // if (!address.getUser().getUserId().equals(userId)) {
    // throw new BusinessException("Address does not belong to this user");
    // }
    // if (updates.getAddressLine1() != null)
    // address.setAddressLine1(updates.getAddressLine1());
    // if (updates.getAddressLine2() != null)
    // address.setAddressLine2(updates.getAddressLine2());
    // if (updates.getCity() != null) address.setCity(updates.getCity());
    // if (updates.getState() != null) address.setState(updates.getState());
    // if (updates.getZipCode() != null) address.setZipCode(updates.getZipCode());
    // if (updates.getCountry() != null) address.setCountry(updates.getCountry());
    // return addressRepository.save(address);
    // }

    // @Transactional
    // public void deleteAddress(UUID userId, UUID addressId) {
    // Address address = addressRepository.findById(addressId)
    // .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    // if (!address.getUser().getUserId().equals(userId)) {
    // throw new BusinessException("Address does not belong to this user");
    // }
    // addressRepository.delete(address);
    // }

    // ---- Admin user management ----

    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User updateUserStatus(UUID userId, String status) {
        User user = getUserById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }
}
