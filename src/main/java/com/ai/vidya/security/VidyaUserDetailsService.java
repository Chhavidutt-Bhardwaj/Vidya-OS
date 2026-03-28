package com.ai.vidya.security;

import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.ai.vidya.config.CacheConfig.CACHE_USER_DETAILS;

@Service
@RequiredArgsConstructor
public class VidyaUserDetailsService implements UserDetailsService {

    private final SystemUserRepository userRepository;

    /**
     * Called by Spring Security during form login (username = email).
     * Roles & permissions are fetched eagerly here — they'll be in the JWT.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        SystemUser user = userRepository.findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        // Initialise lazy collections while in transaction
        user.getRoles().forEach(r -> r.getPermissions().size());
        return new VidyaUserDetails(user);
    }

    /**
     * Called by JwtAuthFilter on every request — result cached in Redis.
     * Cache key = userId string, TTL = 60 min (see CacheConfig).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_USER_DETAILS, key = "#userId.toString()")
    public VidyaUserDetails loadUserById(UUID userId) {
        SystemUser user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        user.getRoles().forEach(r -> r.getPermissions().size());
        return new VidyaUserDetails(user);
    }
}