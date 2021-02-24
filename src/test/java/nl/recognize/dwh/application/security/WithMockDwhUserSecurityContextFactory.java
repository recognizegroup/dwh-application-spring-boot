package nl.recognize.dwh.application.security;

import nl.recognize.dwh.application.model.DwhUser;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockDwhUserSecurityContextFactory implements WithSecurityContextFactory<WithMockDwhUser> {
    public SecurityContext createSecurityContext(WithMockDwhUser withMockDwhUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        DwhUser dwhUser = new DwhUser(withMockDwhUser.username(), withMockDwhUser.password());
        context.setAuthentication(dwhUser);

        return context;
    }
}
