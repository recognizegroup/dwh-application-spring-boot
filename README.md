# Recognize DWH Application Bundle - Spring boot version

This is a Spring boot variant, based upon the dwh-application-symfony-bundle
For more information, see that bundle

TODO: copied Adapt to Spring Boot style. 


## Installation
### Security
Add the following in your @EnableWebSecurity config class:
```java

    @Autowired
    private DwhBasicAuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    private DwhAuthenticationFilter dwhAuthenticationFilter;


@Override
protected void configure(HttpSecurity http) throws Exception {
     ....
            .and()
            .httpBasic()
            .authenticationEntryPoint(authenticationEntryPoint);
        }

    @Bean
    public void configureGlobal(AuthenticationManagerBuilder auth) { 
            auth.inMemoryAuthentication()
            .withUser("admin")
            .password("{noop}password")
            .roles(Role.ROLE_DWH_BRIDGE);
        }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }     
```
```
Ensure authentication for DWH-API paths:
    @RolesAllowed(ROLE.ROLE_DWH_BRIDGE)
```

### Configuration
The encrypted token requires a token that is encrypted with the specified encryption.
```yaml
recognize_dwh:
    protocol_version: 1.0.0
    specification_version: 1.0.0
    encryption: bcrypt
    encrypted_token: $2y$12$ADbwlXKfMjsHKayFlBSuLuu02FkrtgzdNWfCOrzWrCR8zkSoNsUfG
```
