# Recognize DWH Application Bundle - Spring boot version

This is a Spring boot variant, based upon the dwh-application-symfony-bundle
For more information, see that bundle.

## defining your DWH mapping

Define a servce which extends the AbstractEntityLoader. This loader will be injected automatically inside the DWH context


## Installation
### Autowiring
Add the package nl.recognize.dwh to your component scan definition.
E.g.:
@ComponentScan(basePackages = ["nl.recognize.paula", "nl.recognize.dwh"])

### Security
Add the following in your @EnableWebSecurity config class:
```java

    @Autowired
    private DwhBasicAuthenticationEntryPoint authenticationEntryPoint;


    @Configuration
    @Order(0)
    inner class DwhApiWebSecurityConfigurationAdapter : WebSecurityConfigurerAdapter() {

            override fun configure(http: HttpSecurity) {
                http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .antMatcher("/api/dwh/**")
                .authorizeRequests { authorize ->
                authorize
                .anyRequest().authenticated()
            }
        .httpBasic(withDefaults())
        }

protecetd void configure(AuthentiAuthenticationManagerBuilder auth) {
            .....
            auth.inMemoryAuthentication()
            .withUser("admin")
            .password("{noop}password")
            .roles(Role.DWH_BRIDGE);
            }a
    

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }     
```ik
```

### Configuration
The shared secret is the key from the DWH bridge for this application.
The specification version is your own specification version
bsae url is the base URL where the api is hosted, e.g.: https://www.volkerwesselswave.nl

Specify the following parameters in your application configuration:
nl.recognize.dwh.application.shared.secret: <shared secret>
nl.recognize.dwh.application.protocol.version: 1.0.0
nl.recognize.dwh.application.specification.version: 1.0.0
nl.recognize.dwh.application.server.base.url: <base url>
