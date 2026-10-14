package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final java.util.Map<String, CachedUser> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000L; // 1 minute in-memory cache

    private record CachedUser(UserDetails userDetails, long expiry) {}

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        long now = System.currentTimeMillis();
        CachedUser cached = cache.get(email);
        if (cached != null && cached.expiry() > now) {
            return cached.userDetails();
        }

        UserDetails userDetails = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        cache.put(email, new CachedUser(userDetails, now + CACHE_TTL_MS));
        return userDetails;
    }

    public void evictUser(String email) {
        if (email != null) {
            cache.remove(email);
        }
    }
}
