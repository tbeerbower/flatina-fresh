package org.beerbower.flatinafresh.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.family.FamilyService;
import org.beerbower.flatinafresh.user.AppUser;
import org.beerbower.flatinafresh.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FamilyService familyService;

    /** {@code email} is the email address; accounts made before emails may give their username there instead. */
    public record LoginRequest(String email, String username, @NotBlank String password) {
    }

    /** {@code email} is null for an account made before emails: the app asks for one before going on. */
    public record UserDto(Long id, String username, String displayName, String email, boolean admin) {
        public static UserDto of(AppUser user) {
            return new UserDto(user.getId(), user.getUsername(), user.name(), user.getEmail(), user.isAdmin());
        }
    }

    public record LoginResponse(String token, UserDto user) {
        public static LoginResponse of(JwtService jwt, AppUser user) {
            return new LoginResponse(jwt.issue(user.getId()), UserDto.of(user));
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String id = request.email() != null && !request.email().isBlank() ? request.email() : request.username();
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your email");
        }
        AppUser user = find(id.trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return LoginResponse.of(jwtService, user);
    }

    private Optional<AppUser> find(String id) {
        if (id.contains("@")) {
            return users.findByEmail(AppUser.normalizeEmail(id));
        }
        List<AppUser> legacy = users.findByUsernameIgnoreCaseAndEmailIsNull(id);
        return legacy.size() == 1 ? Optional.of(legacy.get(0)) : Optional.empty();
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8) String newPassword) {
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        AppUser user = familyService.currentUser(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is wrong");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return UserDto.of(familyService.currentUser(authentication));
    }

    /** Change your name or email. Either may be left out; an email, once set, can be changed but not removed. */
    public record UpdateProfileRequest(@Size(max = 100) String name, @Email @Size(max = 254) String email) {
    }

    @PutMapping("/me")
    @Transactional
    public UserDto updateMe(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        AppUser user = familyService.currentUser(authentication);
        if (request.name() != null && !request.name().isBlank()) {
            user.rename(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            setEmail(users, user, request.email());
        }
        return UserDto.of(users.save(user));
    }

    /** Sets a (validated) email on the user, refusing one that another account already has. */
    public static void setEmail(AppUserRepository users, AppUser user, String email) {
        String normalized = AppUser.normalizeEmail(email);
        if (normalized.equals(user.getEmail())) {
            return;
        }
        if (users.existsByEmail(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another account already uses that email");
        }
        user.setEmail(normalized);
    }
}
