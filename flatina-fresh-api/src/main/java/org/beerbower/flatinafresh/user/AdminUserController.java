package org.beerbower.flatinafresh.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Admin-only user management: list, add, reset password, change admin flag, remove. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public record UserDto(Long id, String username, String displayName, boolean admin, LocalDateTime createdAt) {
        static UserDto of(AppUser u) {
            return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.isAdmin(), u.getCreatedAt());
        }
    }

    public record CreateUserRequest(@NotBlank @Size(max = 100) String username, String displayName,
                                    @NotBlank @Size(min = 8) String password, boolean admin) {
    }

    public record UpdateUserRequest(String displayName, Boolean admin, @Size(min = 8) String password) {
    }

    @GetMapping
    public List<UserDto> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(u -> u.getUsername().toLowerCase()))
                .map(UserDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        String username = request.username().trim();
        if (users.findByUsernameIgnoreCase(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is taken");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? username : request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAdmin(request.admin());
        return UserDto.of(users.save(user));
    }

    @PutMapping("/{id}")
    @Transactional
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                          Authentication authentication) {
        AppUser user = find(id);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.admin() != null) {
            if (!request.admin() && user.getUsername().equalsIgnoreCase(authentication.getName())) {
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
        if (user.getUsername().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't remove your own account");
        }
        users.delete(user);
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
