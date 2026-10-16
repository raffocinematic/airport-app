package it.raffo.backend.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Testcontainers
class AppUserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void incrementsAuthVersionForExistingUser() {
        UUID userId = UUID.randomUUID();

        AppUser user = new AppUser(
                userId,
                "Operatore",
                "Test",
                "operatore@airport.test",
                "hash-non-usato-in-questo-test",
                Role.OPERATORI,
                true,
                5L,
                Instant.now()
        );

        repository.saveAndFlush(user);

        int updatedRows = repository.incrementAuthVersion(userId);

        AppUser reloadedUser = repository.findById(userId)
                .orElseThrow();

        assertEquals(1, updatedRows);
        assertEquals(6L, reloadedUser.getAuthVersion());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void incrementsAuthVersionWithoutLosingConcurrentUpdates() throws Exception {
        UUID userId = UUID.randomUUID();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        transaction.setTimeout(30);

        transaction.executeWithoutResult(status ->
                repository.saveAndFlush(createUser(userId, 5L))
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        try {
            Future<Integer> first = executor.submit(() ->
                    incrementInTransaction(userId, barrier)
            );

            Future<Integer> second = executor.submit(() ->
                    incrementInTransaction(userId, barrier)
            );

            assertEquals(1, first.get(30, TimeUnit.SECONDS).intValue());
            assertEquals(1, second.get(30, TimeUnit.SECONDS).intValue());

            Long finalVersion = transaction.execute(status ->
                    repository.findById(userId)
                            .orElseThrow()
                            .getAuthVersion()
            );

            assertEquals(7L, finalVersion.longValue());
        } finally {
            executor.shutdownNow();

            boolean workersStopped =
                    executor.awaitTermination(30, TimeUnit.SECONDS);

            assertTrue(workersStopped, "I lavoratori devono terminare");

            transaction.executeWithoutResult(status ->
                    repository.deleteById(userId)
            );
        }
    }

    @Test
    void returnsZeroWhenUserDoesNotExist() {
        UUID missingUserId = UUID.randomUUID();

        int updatedRows = repository.incrementAuthVersion(missingUserId);

        assertEquals(0, updatedRows);
    }

    @Test
    void changesPasswordAndIncrementsAuthVersionForExistingUser() {
        UUID userId = UUID.randomUUID();

        AppUser user = createUser(userId, 5L);
        repository.saveAndFlush(user);

        String currentPasswordHash = user.getPasswordHash();
        String newPasswordHash = "nuovo-hash-di-test";

        int updatedRows = repository.changePasswordIfUnchanged(
                userId,
                5L,
                currentPasswordHash,
                newPasswordHash
        );

        AppUser reloadedUser = repository.findById(userId)
                .orElseThrow();

        assertEquals(1, updatedRows);
        assertEquals(newPasswordHash, reloadedUser.getPasswordHash());
        assertEquals(6L, reloadedUser.getAuthVersion());
    }

    // ---------------------------------------------------------------------------------------------

    private AppUser createUser(UUID userId, long authVersion) {
        return new AppUser(
                userId,
                "Operatore",
                "Test",
                "operatore-" + userId + "@airport.test",
                "hash-non-usato-in-questo-test",
                Role.OPERATORI,
                true,
                authVersion,
                Instant.now()
        );
    }

    private int incrementInTransaction(UUID userId, CyclicBarrier barrier) {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        transaction.setTimeout(30);

        return transaction.execute(status -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Lavoratore interrotto", exception
                );
            } catch (BrokenBarrierException | TimeoutException exception) {
                throw new IllegalStateException(
                        "Sincronizzazione dei lavoratori fallita", exception
                );
            }

            return repository.incrementAuthVersion(userId);
        });
    }
}
