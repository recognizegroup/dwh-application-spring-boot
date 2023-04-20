package nl.recognize.dwh.application.model;

import nl.recognize.dwh.application.security.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DwhUser implements Authentication {
    private boolean authenticated;
    private final String username;
    private final String password;

    public DwhUser(String username, String password) {
        this.username = username;
        this.password = password;
        this.authenticated = true;
    }

    /**
     * @return array
     */
    public List<String> getRoles() {
        return Collections.singletonList(Role.ROLE_DWH_BRIDGE);
    }

    public String getUuid() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getCredentials() {
        return password;
    }

    @Override
    public String getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return getUsername();
    }
}
