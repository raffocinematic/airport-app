package it.raffo.backend.security;

import it.raffo.backend.users.AppUser;
import it.raffo.backend.users.AppUserRepository;
import it.raffo.backend.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AirportJwtAuthenticationConverterTest {

    private static final UUID USER_ID = UUID.fromString("76b7be51-6e51-4769-a577-13be66df05e9");

    private AppUserRepository repository;
    private AirportJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        converter = new AirportJwtAuthenticationConverter(repository);
    }

    @ParameterizedTest(name = "subject {0}")
    @MethodSource("invalidSubjects")
    void rejectsInvalidSubjectBeforeReadingDatabase(String description, Object subject) {
        Jwt jwt = jwt(subject, 0L, Role.OPERATORI);

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));

        verifyNoInteractions(repository);
    }

    static Stream<Arguments> invalidSubjects() {
        return Stream.of(
                Arguments.of("assente", null),
                Arguments.of("vuoto", ""),
                Arguments.of("con soli spazi", "   "),
                Arguments.of("numerico", 42),
                Arguments.of("non UUID", "non-un-uuid")
        );
    }

    @ParameterizedTest(name = "authVersion {0}")
    @MethodSource("invalidAuthVersions")
    void rejectsInvalidAuthVersion(String description, Object authVersion) {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user(true, 0L, Role.OPERATORI)));
        Jwt jwt = jwt(USER_ID.toString(), authVersion, Role.OPERATORI);

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    static Stream<Arguments> invalidAuthVersions() {
        return Stream.of(
                Arguments.of("assente", null),
                Arguments.of("stringa", "0"),
                Arguments.of("Integer negativa", -1),
                Arguments.of("Long negativa", -1L),
                Arguments.of("decimale frazionaria", 0.5),
                Arguments.of("decimale senza parte frazionaria", 0.0),
                Arguments.of("booleana", true)
        );
    }

    @ParameterizedTest(name = "{0}: ruolo DB={2}, ruolo JWT={3}")
    @MethodSource("validAuthVersionsAndRoles")
    void acceptsValidAuthVersionAndUsesDatabaseRole(
            String description, Object authVersion, Role databaseRole, Role tokenRole
    ) {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user(true, 0L, databaseRole)));
        Jwt jwt = jwt(USER_ID.toString(), authVersion, tokenRole);

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertTrue(authentication.isAuthenticated());
        assertEquals(USER_ID.toString(), authentication.getName());
        assertEquals(1, authentication.getAuthorities().size());
        assertEquals(
                "ROLE_" + databaseRole.name(),
                authentication.getAuthorities().iterator().next().getAuthority()
        );
    }

    static Stream<Arguments> validAuthVersionsAndRoles() {
        return Stream.of(
                Arguments.of("Integer", 0, Role.ADMIN, Role.OPERATORI),
                Arguments.of("Integer", 0, Role.OPERATORI, Role.ADMIN),
                Arguments.of("Long", 0L, Role.ADMIN, Role.OPERATORI),
                Arguments.of("Long", 0L, Role.OPERATORI, Role.ADMIN)
        );
    }

    @ParameterizedTest(name = "versione JWT={0}, versione DB={1}")
    @MethodSource("differentAuthVersions")
    void rejectsAuthVersionDifferentFromDatabase(long tokenVersion, long databaseVersion) {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user(true, databaseVersion, Role.OPERATORI)));
        Jwt jwt = jwt(USER_ID.toString(), tokenVersion, Role.OPERATORI);

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    static Stream<Arguments> differentAuthVersions() {
        return Stream.of(
                Arguments.of(0L, 1L),
                Arguments.of(1L, 0L)
        );
    }

    @Test
    void rejectsMissingUser() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());
        Jwt jwt = jwt(USER_ID.toString(), 0L, Role.OPERATORI);

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void rejectsInactiveUser() {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user(false, 0L, Role.OPERATORI)));
        Jwt jwt = jwt(USER_ID.toString(), 0L, Role.OPERATORI);

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    private AppUser user(boolean active, long authVersion, Role role) {
        return new AppUser(
                USER_ID,
                "Operatore",
                "Test",
                "operatore@airport.test",
                "hash-non-usato-in-questo-test",
                role,
                active,
                authVersion,
                Instant.parse("2026-10-16T00:00:00Z")
        );
    }

    // Null omette il claim; gli altri valori mantengono il tipo passato dal test.
    // Questo Jwt in memoria esercita il converter, senza verificare una firma.
    private Jwt jwt(Object subject, Object authVersion, Role tokenRole) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("role", tokenRole.name())
                .claims(claims -> {
                    if (subject != null) {
                        claims.put("sub", subject);
                    }
                    if (authVersion != null) {
                        claims.put("authVersion", authVersion);
                    }
                })
                .build();
    }
}
