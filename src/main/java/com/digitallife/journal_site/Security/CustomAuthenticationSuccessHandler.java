package com.digitallife.journal_site.Security;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserDetailService userService;

    public CustomAuthenticationSuccessHandler(UserDetailService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        if (user != null) {
            request.getSession().setAttribute("currentUserId", user.getId());
        }
        response.sendRedirect("/journal/home");
    }
}
