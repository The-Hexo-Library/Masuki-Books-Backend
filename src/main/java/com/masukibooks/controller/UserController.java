package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
// import com.masukibooks.entity.Address;
import com.masukibooks.entity.User;
import com.masukibooks.security.JwtTokenProvider;
import com.masukibooks.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    private UUID getCurrentUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<User>> getProfile(@RequestHeader("Authorization") String authHeader) {
        UUID userId = getCurrentUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", userService.getProfile(userId)));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestHeader("Authorization") String authHeader,
            @RequestBody User updates) {
        UUID userId = getCurrentUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(userId, updates)));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        UUID userId = getCurrentUserId(authHeader);
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"), passwordEncoder);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    // @GetMapping("/me/addresses")
    // @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    // public ResponseEntity<ApiResponse<List<Address>>>
    // getAddresses(@RequestHeader("Authorization") String authHeader) {
    // UUID userId = getCurrentUserId(authHeader);
    // return ResponseEntity.ok(ApiResponse.success("Addresses retrieved",
    // userService.getAddresses(userId)));
    // }

    // // @PostMapping("/me/addresses")
    // @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    // public ResponseEntity<ApiResponse<Address>>
    // addAddress(@RequestHeader("Authorization") String authHeader,
    // @RequestBody Address address) {
    // UUID userId = getCurrentUserId(authHeader);
    // return ResponseEntity.ok(ApiResponse.success("Address added",
    // userService.addAddress(userId, address)));
    // }

    // @PutMapping("/me/addresses/{addressId}")
    // @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    // public ResponseEntity<ApiResponse<Address>>
    // updateAddress(@RequestHeader("Authorization") String authHeader,
    // @PathVariable UUID addressId,
    // @RequestBody Address updates) {
    // UUID userId = getCurrentUserId(authHeader);
    // return ResponseEntity
    // .ok(ApiResponse.success("Address updated", userService.updateAddress(userId,
    // addressId, updates)));
    // }

    // @DeleteMapping("/me/addresses/{addressId}")
    // @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    // public ResponseEntity<ApiResponse<Void>>
    // deleteAddress(@RequestHeader("Authorization") String authHeader,
    // @PathVariable UUID addressId) {
    // UUID userId = getCurrentUserId(authHeader);
    // userService.deleteAddress(userId, addressId);
    // return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    // }
}
