package com.cookify.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CsrfToken loading is deferred -- merely declaring the parameter does
 * NOT write the cookie. Calling getToken() forces the repository to
 * actually generate and save it. The frontend calls this once on load,
 * then echoes the cookie value back in an X-XSRF-TOKEN header on every
 * state-changing request. Also call this again after login/logout,
 * since Spring Security rotates the token on both.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public void primeCsrfCookie(CsrfToken token) {
        token.getToken();
    }
}
