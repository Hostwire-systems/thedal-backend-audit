
package com.thedal.thedal_app;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.thedal.thedal_app.oauth2login.OAuth2LoginSuccessHandler;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@EnableWebSecurity
@Profile("!reporting")
public class ConfigurationSecurity{

        @Value("${thedal.server.url}")
        private String serverUrl;

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        
        @Autowired
        private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

        // CORS configuration
        // @Bean
        // public CorsConfigurationSource corsConfigurationSource() {
        //         CorsConfiguration configuration = new CorsConfiguration();
        //         configuration.setAllowedOrigins(// Allowing all origins
        //                         Arrays.asList("*"));
        //         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE","Patch"));
        //         configuration.setAllowCredentials(false);
        //         configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        //         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //         source.registerCorsConfiguration("/**", configuration);
        //         return source;
        // }
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(// Allowing all origins
                                Arrays.asList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE","Patch","PROPFIND"));
                configuration.setAllowCredentials(false);
                configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
        // @Bean
        // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //     http
        //         .cors(cors -> cors.configurationSource(corsConfigurationSource())) // enabling CORS
        //         .csrf(AbstractHttpConfigurer::disable)
        //         .authorizeHttpRequests(authorizeRequests -> authorizeRequests
        //             .requestMatchers(
        //                 "/v3/api-docs/**",
        //                 "/swagger-ui/**",
        //                 "/configuration/ui/**",
        //                 "/swagger-resources/**",
        //                 "/configuration/security/**",
        //                 "/swagger-ui.html/**",
        //                 "/webjars/**",
        //                 "/", "/login", "/oauth2/**","/auth/two-factor/otp/**",
        //                 "/auth/ping", "/auth/signup", "/auth/login","/auth/oauth/signup",
        //                 "/auth/two-factor/email-verify/**",
        //                 "/auth/signup/oauth-complete", "/oauth/complete-signup","/auth/google-login")
        //             .permitAll()
        //             .anyRequest().authenticated())
        //             .oauth2Login(oauth2 -> oauth2
        //             .loginPage("/login")
        //             .successHandler(oAuth2LoginSuccessHandler))
        //         .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        //     return http.build();
        // }

        @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/configuration/security/**",
                    "/swagger-ui.html/**",
                    "/webjars/**",
                    "/",
                    "/login",
                    "/oauth2/**",
                    "/auth/two-factor/otp/**",
                    "/auth/ping",
                    "/auth/signup",
                    "/auth/login",
                    "/auth/oauth/signup",
                    "/auth/two-factor/email-verify/**",
                    "/auth/signup/oauth-complete",
                    "/oauth/complete-signup",
                    "/auth/google-login",
                    "/auth/reset-password",
                    "/auth/volunteer/otp/verify",
                    "/auth/volunteer/otp/invoke",
                    "/auth/verify-volunteer-otp",
                    "/api/cpanel/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oAuth2LoginSuccessHandler)
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessHandler(oidcLogoutSuccessHandler(null)) // Pass clientRegistrationRepository
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

        @Bean
        public OpenAPI customOpenAPI() {
                // swagger configuration for adding JWT auth header
                final String securitySchemeName = "bearerAuth";
                return new OpenAPI()
                                .addServersItem(new Server().url("http://localhost:8080"))
                                .addServersItem(new Server().url("https://thedal-api.hostwire.cloud"))
				.addServersItem(new Server().url("http://43.205.166.185:8080"))
                .addServersItem(new Server().url("https://thedal-app.1q3ff9z04yrb.us-south.codeengine.appdomain.cloud"))
                                .addServersItem(new Server().url("https://thedal-staging.hostwire.cloud"))
			        .addServersItem(new Server().url("https://thedal-backend-production.up.railway.app"))
			        .addServersItem(new Server().url("https://api.thedal.co.in"))
			        .addServersItem(new Server().url("https://rk84ssowco4kgw0w4owwcwgo.82.208.23.57.sslip.io"))
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(
                                                new Components()
                                                                .addSecuritySchemes(securitySchemeName,
                                                                                new SecurityScheme()
                                                                                                .name(securitySchemeName)
                                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                                .scheme("bearer")
                                                                                                .bearerFormat("JWT")));
        }
        
//    	@Bean
//    	public FilterRegistrationBean<AccountFilter> accountFilter(){
//    		FilterRegistrationBean<AccountFilter> registrationBean=new FilterRegistrationBean<>();
//    		registrationBean.setFilter(new AccountFilter());
//    		registrationBean.addUrlPatterns("*");
//    		return registrationBean;
//    	}

    @Bean
    public OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        if (clientRegistrationRepository == null) return null;
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("http://localhost:5173");
        return handler;
    }

}
