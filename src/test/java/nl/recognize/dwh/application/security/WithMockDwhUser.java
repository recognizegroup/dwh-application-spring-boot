package nl.recognize.dwh.application.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockDwhUserSecurityContextFactory.class)
public @interface WithMockDwhUser {
    String username() default "";
    String password() default "";
}
