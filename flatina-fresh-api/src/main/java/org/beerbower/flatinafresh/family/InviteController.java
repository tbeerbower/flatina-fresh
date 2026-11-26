package org.beerbower.flatinafresh.family;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.security.AuthController;
import org.beerbower.flatinafresh.security.JwtService;
import org.beerbower.flatinafresh.user.AppUser;
import org.beerbower.flatinafresh.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Public endpoints behind an invite link: see which family it is for, then create an account in that family.
 * The token is the only credential, so it is single use and expires.
 */
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {
    private final FamilyInviteRepository invites;
    private final FamilyRepository families;
    private final AppUserRepository users;
    private final FamilyService familyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public record InviteInfo(String familyName, String invitedBy, LocalDateTime expiresAt) {
    }

    public record AcceptRequest(@NotBlank @Size(max = 100) String username, @Size(max = 200) String displayName,
                                @NotBlank @Size(min = 8) String password) {
    }

    @GetMapping("/{token}")
    @Transactional(readOnly = true)
    public InviteInfo get(@PathVariable String token) {
        FamilyInvite invite = open(token);
        Family family = families.findById(invite.getFamilyId()).orElseThrow(this::gone);
        String invitedBy = invite.getCreatedBy() == null ? null
                : users.findById(invite.getCreatedBy()).map(FamilyController::name).orElse(null);
        return new InviteInfo(family.getName(), invitedBy, invite.getExpiresAt());
    }

    /** Creates the account in the invite's family and signs it in. */
    @PostMapping("/{token}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AuthController.LoginResponse accept(@PathVariable String token, @Valid @RequestBody AcceptRequest request) {
        FamilyInvite invite = open(token);
        String username = request.username().trim();
        if (users.findByUsernameIgnoreCase(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is taken");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? username : request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFamilyId(invite.getFamilyId());
        user = users.save(user);
        invite.setUsedAt(LocalDateTime.now(familyService.clock()));
        invite.setUsedBy(user.getId());
        return new AuthController.LoginResponse(jwtService.issue(user.getUsername()), AuthController.UserDto.of(user));
    }

    private FamilyInvite open(String token) {
        return invites.findByToken(token)
                .filter(i -> i.isOpen(LocalDateTime.now(familyService.clock())))
                .orElseThrow(this::gone);
    }

    private ResponseStatusException gone() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "This invite link has expired or was already used. Ask for a new one.");
    }
}
