package nl.recognize.dwh.application.security;

import nl.recognize.dwh.application.model.DwhUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class DwhUserAuthenticationProvider implements AuthenticationProvider {

    private final String sharedSecret;
    private static final Logger log = LoggerFactory.getLogger(DwhUserAuthenticationProvider.class);

    public DwhUserAuthenticationProvider(@Value("${nl.recognize.dwh.application.shared.secret:874jkhads897423rHJSD32}") String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (password.equals(sharedSecret)) {
            return new DwhUser(name, password);
        }

        log.warn("Incorrect DWH shared secret, got: {} expected {}", password, sharedSecret);
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
