package com.invoice.controller;

import com.invoice.dto.response.ApiResponse;
import com.invoice.dto.response.UserSummaryResponse;
import com.invoice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only user management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "[ADMIN] List all users")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "[ADMIN] Update a user's role (ADMIN | USER)")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateRole(
            @PathVariable Integer id,
            @RequestParam String role) {
        return ResponseEntity.ok(ApiResponse.success("Role updated",
                userService.updateUserRole(id, role)));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "[ADMIN] Delete a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}

