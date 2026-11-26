package org.beerbower.flatinafresh.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.family.FamilyService;
import org.beerbower.flatinafresh.security.AuthController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Admin-only user management: list, add, change name/email/password/admin flag, remove. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final FamilyService familyService;

    public record UserDto(Long id, String username, String displayName, String email, boolean admin,
                          LocalDateTime createdAt) {
        static UserDto of(AppUser u) {
            return new UserDto(u.getId(), u.getUsername(), u.name(), u.getEmail(), u.isAdmin(), u.getCreatedAt());
        }
    }

    /** {@code username} is the name the app calls them; they sign in with {@code email}. */
    public record CreateUserRequest(@NotBlank @Email @Size(max = 254) String email,
                                    @NotBlank @Size(max = 100) String username,
                                    @NotBlank @Size(min = 8) String password, boolean admin) {
    }

    public record UpdateUserRequest(@Size(max = 100) String name, @Email @Size(max = 254) String email, Boolean admin,
                                    @Size(min = 8) String password) {
    }

    @GetMapping
    public List<UserDto> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(u -> u.name().toLowerCase()))
                .map(UserDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        AppUser user = new AppUser();
        AuthController.setEmail(users, user, request.email());
        user.rename(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAdmin(request.admin());
        // Added here, a user starts in a family of their own; family members are added with invite links.
        return UserDto.of(familyService.startFamily(user));
    }

    @PutMapping("/{id}")
    @Transactional
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                          Authentication authentication) {
        AppUser user = find(id);
        if (request.name() != null && !request.name().isBlank()) {
            familyService.rename(user, request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            AuthController.setEmail(users, user, request.email());
        }
        if (request.admin() != null) {
            if (!request.admin() && user.getId().equals(FamilyService.userId(authentication))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't remove your own admin access");
            }
            user.setAdmin(request.admin());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserDto.of(user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id, Authentication authentication) {
        AppUser user = find(id);
        if (user.getId().equals(FamilyService.userId(authentication))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't remove your own account");
        }
        users.delete(user);
        users.flush();
        familyService.deleteIfEmpty(user.getFamilyId());
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
