package com.digitallife.journal_site.Security;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDto;
import com.digitallife.journal_site.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistrationController(UserRepository userRepo,
                                  PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "registration";  // resolves to registration.html
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("userDto") UserDto userDto) {
        // check for existing username
        if (userRepo.existsByUsername(userDto.getUsername())) {
            return "redirect:/register?error";
        }
        // create & save new user
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole("USER");
        userRepo.save(user);
        return "redirect:/login?registered";
    }
}
