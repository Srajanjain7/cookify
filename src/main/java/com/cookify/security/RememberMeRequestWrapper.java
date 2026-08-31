package com.cookify.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * PersistentTokenBasedRememberMeServices.loginSuccess() only sets the
 * cookie if the servlet request PARAMETER "remember-me" is present --
 * a check written for classic form posts, which a JSON login body
 * never populates. This wrapper bridges our LoginRequest.rememberMe()
 * boolean into that expected parameter without touching Spring
 * Security's internals.
 */
public class RememberMeRequestWrapper extends HttpServletRequestWrapper {

    public RememberMeRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        if ("remember-me".equals(name)) {
            return "true";
        }
        return super.getParameter(name);
    }
}
