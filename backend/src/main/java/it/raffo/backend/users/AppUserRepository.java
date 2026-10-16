package it.raffo.backend.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Qui definiamo aggiornamento del DB, da usare poi nel service e verificheremo
     * il comportamento concorrente
     *
     * IL DB calcola il nuovo valore partendo da quello presente nella riga, senza ricevere un valore
     * precedentemente incrementato in Java
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update AppUser u
        set u.authVersion = u.authVersion + 1
        where u.id = :userId
        """)
    int incrementAuthVersion(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update AppUser u
        set u.passwordHash = :newPasswordHash,
            u.authVersion = u.authVersion + 1
        where u.id = :userId
          and u.active = true
          and u.authVersion = :expectedAuthVersion
          and u.passwordHash = :currentPasswordHash
        """)
    int changePasswordIfUnchanged(
            @Param("userId") UUID userId,
            @Param("expectedAuthVersion") long expectedAuthVersion,
            @Param("currentPasswordHash") String currentPasswordHash,
            @Param("newPasswordHash") String newPasswordHash
    );
}
