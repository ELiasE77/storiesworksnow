package com.digitallife.journal_site.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * service method used to access and update the userRepository (used by controller to access repository)
 */
@Service
public class UserDetailService implements UserDetailsService {

    @Autowired
    private UserRepository repository;


    /**
     * find the user object based on the username
     *
     * @param username username of the object to find
     * @return the User Object based on the username
     */
    public User findByUsername(String username) {
        Optional<User> user = repository.findByUsername(username);
        return user.orElse(null);  // Return the user if present, otherwise return null
    }
    public User findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * method was copied from a tutorial (https://www.youtube.com/watch?v=9J-b6OlPy24)
     *
     * @param username username to load from the database
     * @return UserDetails of the current user
     * @throws UsernameNotFoundException when username is not found throw this
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = repository.findByUsername(username);
        if (user.isPresent()) {
            var userObj = user.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(userObj.getUsername())
                    .password(userObj.getPassword())
                    .roles(getRoles(userObj))
                    .build();
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    /**
     * get the role of the current user (if none is provided it will be a regular user)
     *
     * @param user the user which you want to get the role from
     * @return the role of the user
     */
    private String[] getRoles(User user) {
        if (user.getRole() == null) {
            return new String[]{"USER"};
        }
        return user.getRole().split(",");
    }

    /**
     * save the user to the database
     *
     * @param user the user to be saved to the database
     */
    public void saveUser(User user) {
        repository.save(user);
    }
}
