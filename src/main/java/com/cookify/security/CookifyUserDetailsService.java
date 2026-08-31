package com.cookify.security;

import com.cookify.model.User;
import com.cookify.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Resolves the login identifier against username, email, OR phone --
 * the assignment's Login pseudocode accepts any of the three
 * ("UserID / Email / Phone Number").
 */
@Service
public class CookifyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CookifyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmailOrPhone(identifier, identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Error: User not found"));
        return new CookifyUserDetails(user);
    }
}
