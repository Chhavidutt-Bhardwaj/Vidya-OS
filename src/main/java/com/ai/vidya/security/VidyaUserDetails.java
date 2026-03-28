package com.ai.vidya.security;

import com.ai.vidya.modules.user.entity.SystemUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class VidyaUserDetails implements UserDetails {

    private final UUID   userId;
    private final UUID   schoolId;   // null for SUPER_ADMIN / CHAIN_ADMIN
    private final UUID   chainId;    // null unless CHAIN_ADMIN
    private final String email;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public VidyaUserDetails(SystemUser user) {
        this.userId   = user.getId();
        this.schoolId = user.getSchoolId();
        this.chainId  = user.getChainId();
        this.email    = user.getEmail();
        this.password = user.getPasswordHash();
        this.active   = user.isActive();

        // Build authorities from roles AND individual permissions
        // Roles → ROLE_XXX prefix (for hasRole checks)
        // Permissions → raw code (for hasAuthority checks)
        Set<GrantedAuthority> roleAuthorities = user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
            .collect(Collectors.toSet());

        Set<GrantedAuthority> permissionAuthorities = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(p -> new SimpleGrantedAuthority(p.getCode()))
            .collect(Collectors.toSet());

        this.authorities = Stream.concat(
            roleAuthorities.stream(),
            permissionAuthorities.stream()
        ).collect(Collectors.toSet());
    }

    @Override public String getUsername()                    { return email; }
    @Override public String getPassword()                    { return password; }
    @Override public boolean isAccountNonExpired()           { return true; }
    @Override public boolean isCredentialsNonExpired()       { return true; }
    @Override public boolean isAccountNonLocked()            { return true; }
    @Override public boolean isEnabled()                     { return active; }

    public boolean isSuperAdmin() {
        return authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    public boolean isChainAdmin() {
        return authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_CHAIN_ADMIN"));
    }
}