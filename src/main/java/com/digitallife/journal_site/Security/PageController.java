package com.digitallife.journal_site.Security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    // e.g. home page only
    @GetMapping("/")
    public String home() {
        return "home";  // home.html
    }

    // ← NO @GetMapping("/login") here!
}
