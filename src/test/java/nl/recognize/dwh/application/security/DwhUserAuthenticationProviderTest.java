package nl.recognize.dwh.application.security;

import nl.recognize.dwh.application.model.DwhUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

class DwhUserAuthenticationProviderTest {
    @Test
    void authenticateCorrectSharedSecret() {
        DwhUserAuthenticationProvider provider = new DwhUserAuthenticationProvider("geheimpje");
        Authentication input = new UsernamePasswordAuthenticationToken("blaat", "geheimpje");
        Authentication output = provider.authenticate(input);

        assertThat(output, instanceOf(DwhUser.class));
        DwhUser user = (DwhUser) output;
        assertEquals("blaat", user.getUsername());
        assertEquals("geheimpje", user.getPassword());
        assertTrue(user.isAuthenticated());
    }

    @Test
    void authenticateWrongSharedSecret() {
        DwhUserAuthenticationProvider provider = new DwhUserAuthenticationProvider("geheimpje");
        Authentication input = new UsernamePasswordAuthenticationToken("blaat", "Welkom2020");
        Authentication output = provider.authenticate(input);
        assertNull(output);
    }
}