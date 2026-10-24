package it.raffo.backend.routes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routes")
public class Route {

    @Id // -> chiave primaria
    private UUID id;

    @Column(name = "origin_city", nullable = false, length = 100)
    private String originCity;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(
            name = "normalized_destination_city",
            nullable = false,
            length = 100
    )
    private String normalizedDestinationCity;

    @Enumerated(EnumType.STRING) // -> salva i nomi invece delle posizioni numeriche dell'enum
    @Column(name = "destination_country_code", nullable = false, length = 2)
    private RouteCountry destinationCountryCode;

    @Column(name = "return_enabled", nullable = false)
    private boolean returnEnabled;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Serve a JPA per costruire l'oggetto quando carica una riga
    protected Route() {
    }

    // Costruttore pubblico che serve al nostro service per creare una nuova tratta
    public Route(
            UUID id,
            String originCity,
            String destinationCity,
            String normalizedDestinationCity,
            RouteCountry destinationCountryCode,
            boolean returnEnabled,
            Instant createdAt
    ) {
        this.id = id;
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.normalizedDestinationCity = normalizedDestinationCity;
        this.destinationCountryCode = destinationCountryCode;
        this.returnEnabled = returnEnabled;
        this.createdAt = createdAt;
    }

    /**
     * Metodo che aggiorna insieme i quattro attributi relativi ai dati modificabili
     *
     * @param destinationCity
     * @param normalizedDestinationCity
     * @param destinationCountryCode
     * @param returnEnabled
     */
    public void updateDetails(
            String destinationCity,
            String normalizedDestinationCity,
            RouteCountry destinationCountryCode,
            boolean returnEnabled
    ) {
        this.destinationCity = destinationCity;
        this.normalizedDestinationCity = normalizedDestinationCity;
        this.destinationCountryCode = destinationCountryCode;
        this.returnEnabled = returnEnabled;
    }

    // Getter, per permettere al service di leggere gli attributi e costruire i DTO di risposta.
    public UUID getId() {
        return id;
    }

    public String getOriginCity() {
        return originCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public String getNormalizedDestinationCity() {
        return normalizedDestinationCity;
    }

    public RouteCountry getDestinationCountryCode() {
        return destinationCountryCode;
    }

    public boolean isReturnEnabled() {
        return returnEnabled;
    }

    /**
     * Metodo che permetterà al service di leggere lo stato
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Metodo che useremo successivamente nel service per disattivare o riattivare una tratta.
     * Metodo che da solo NON esegue una richiesta HTTP o apre una transazine
     */
    public void changeStatus(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public RouteType getRouteType() {
        if (destinationCountryCode == RouteCountry.IT) {
            return RouteType.NAZIONALE;
        }
        return RouteType.INTERNAZIONALE;
    }
}
