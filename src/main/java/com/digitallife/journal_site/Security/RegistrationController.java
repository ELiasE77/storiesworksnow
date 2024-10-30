package com.digitallife.journal_site.Security;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// used for storing new users to the database and sending the encoded password to the database
@RestController
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * post point which registers new users to the database
     *
     * @param user the new user to be added
     * @return the user which needs to be saved
     */
    @PostMapping("/register/user")
    public User createUser(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}
