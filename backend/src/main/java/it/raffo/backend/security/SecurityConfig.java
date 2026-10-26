package it.raffo.backend.security;

import jakarta.servlet.DispatcherType;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.time.Clock;

import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.ObjectMapper;


/**
 * Configura la sicurezza HTTP del backend:
 * autenticazione tramite JWT Bearer, autorizzazione per ruolo,
 * gestione degli errori, CORS e limitazione dei tentativi di login.
 * <p>
 * Le API usano autenticazione stateless e permessi espliciti:
 * le richieste non autorizzate da una regola vengono negate.
 */
@Configuration
@EnableConfigurationProperties({
        CorsProperties.class,
        LoginRateLimitProperties.class
})
public class SecurityConfig {

    private final AirportJwtAuthenticationConverter airportJwtAuthenticationConverter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
            AirportJwtAuthenticationConverter airportJwtAuthenticationConverter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.airportJwtAuthenticationConverter = airportJwtAuthenticationConverter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    /**
     * Costruisce la catena di filtri applicata alle richieste HTTP.
     * HttpSecurity configura i controlli; build() produce la catena.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginRateLimiter rateLimiter,
                                                   ObjectMapper objectMapper,
                                                   Clock clock) throws Exception {

        // Registriamo il filtro soltanto nella catena Spring Security,
        // evitando una registrazione aggiuntiva come filtro del servlet container.
        LoginRateLimitFilter loginRateLimitFilter =
                new LoginRateLimitFilter(
                        rateLimiter,
                        objectMapper,
                        clock
                );

        return http
                .cors(Customizer.withDefaults())
                // Eseguiamo il controllo dei tentativi dopo CORS:
                // anche le risposte 429 devono avere gli header CORS necessari.
                .addFilterAfter(loginRateLimitFilter, CorsFilter.class)
                // Il client invia esplicitamente il Bearer nell'header Authorization;
                // l'autenticazione non si basa su cookie inviati automaticamente.
                // Rivedere questa scelta se verrà introdotta autenticazione tramite cookie.
                .csrf(AbstractHttpConfigurer::disable)
                // Spring Security non conserva l'autenticazione in HttpSession:
                // ogni richiesta protetta deve presentare un Bearer valido.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // quando si verifica un errore, il server può effettuare un passaggio interno per costruire
                        // la risposta, così identifichiamo quel passaggio
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/change-password"
                        ).authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/operatori/**").hasRole("OPERATORI")

                        // Il catalogo delle destinazioni è consultabile dagli ADMIN.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/destination-cities"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/routes"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/routes"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/routes/*"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/routes/*/status"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/routes/*"
                        ).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(airportJwtAuthenticationConverter))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
        SecretKey secretKey = jwtSecretKey(secret);
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        SecretKey secretKey = jwtSecretKey(secret);
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey jwtSecretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Definisce la politica CORS delle API usando le origini configurate.
     * CORS regola l'accesso cross-origin dei browser;
     * autenticazione e autorizzazione restano controlli separati.
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            CorsProperties properties
    ) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(properties.allowedOrigins());

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );

        configuration.setExposedHeaders(
                List.of("Retry-After")
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;

    }

    // Fornisce l'orario UTC ai componenti che ricevono Clock: nei test può essere sostituito con un Clock fisso.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
