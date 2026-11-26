package org.beerbower.flatinafresh.family;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.user.AppUser;
import org.beerbower.flatinafresh.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The signed-in user's family: its members, its name, and invite links for new members. */
@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {
    static final int INVITE_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamilyService familyService;
    private final FamilyRepository families;
    private final FamilyInviteRepository invites;
    private final AppUserRepository users;

    public record MemberDto(Long id, String username, String displayName, String email, boolean you) {
    }

    public record InviteDto(Long id, String token, String createdBy, LocalDateTime createdAt,
                            LocalDateTime expiresAt) {
    }

    public record FamilyDto(Long id, String name, List<MemberDto> members, List<InviteDto> invites) {
    }

    public record RenameRequest(@NotBlank @Size(max = 200) String name) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public FamilyDto get(Authentication authentication) {
        return dto(familyService.currentUser(authentication));
    }

    @PutMapping
    @Transactional
    public FamilyDto rename(Authentication authentication, @Valid @RequestBody RenameRequest request) {
        AppUser me = familyService.currentUser(authentication);
        family(me).setName(request.name().trim());
        return dto(me);
    }

    /** A new single-use invite link, good for {@value #INVITE_DAYS} days. */
    @PostMapping("/invites")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InviteDto invite(Authentication authentication) {
        AppUser me = familyService.currentUser(authentication);
        LocalDateTime now = LocalDateTime.now(familyService.clock());
        FamilyInvite invite = new FamilyInvite();
        invite.setFamilyId(me.getFamilyId());
        invite.setToken(newToken());
        invite.setCreatedBy(me.getId());
        invite.setCreatedAt(now);
        invite.setExpiresAt(now.plusDays(INVITE_DAYS));
        invite = invites.save(invite);
        return new InviteDto(invite.getId(), invite.getToken(), name(me), invite.getCreatedAt(), invite.getExpiresAt());
    }

    @DeleteMapping("/invites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void revoke(Authentication authentication, @PathVariable Long id) {
        Long familyId = familyService.currentFamilyId(authentication);
        FamilyInvite invite = invites.findById(id)
                .filter(i -> i.getFamilyId().equals(familyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));
        invites.delete(invite);
    }

    /** Takes someone out of the family. They keep their account, in a new family of their own. */
    @DeleteMapping("/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void removeMember(Authentication authentication, @PathVariable Long id) {
        AppUser me = familyService.currentUser(authentication);
        AppUser member = users.findById(id)
                .filter(u -> u.getFamilyId().equals(me.getFamilyId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not in your family"));
        if (member.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't remove yourself");
        }
        familyService.moveToOwnFamily(member);
    }

    private FamilyDto dto(AppUser me) {
        Family family = family(me);
        List<AppUser> members = users.findByFamilyIdOrderByCreatedAt(family.getId());
        Map<Long, AppUser> byId = members.stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        List<InviteDto> open = invites.findByFamilyIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAt(
                        family.getId(), LocalDateTime.now(familyService.clock())).stream()
                .map(i -> new InviteDto(i.getId(), i.getToken(),
                        i.getCreatedBy() != null && byId.containsKey(i.getCreatedBy()) ? name(byId.get(i.getCreatedBy())) : null,
                        i.getCreatedAt(), i.getExpiresAt()))
                .toList();
        return new FamilyDto(family.getId(), family.getName(),
                members.stream().map(u -> new MemberDto(u.getId(), u.getUsername(), name(u), u.getEmail(),
                        u.getId().equals(me.getId()))).toList(),
                open);
    }

    private Family family(AppUser me) {
        return families.findById(me.getFamilyId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    static String name(AppUser u) {
        return u.name();
    }

    static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
