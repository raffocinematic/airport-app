package it.raffo.backend.routes;

import it.raffo.backend.destinations.DestinationCity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    private UUID id;

    @Column(name = "origin_city", nullable = false, length = 100)
    private String originCity;

    // La città collegata determina nome e paese della destinazione.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_city_id", nullable = false)
    private DestinationCity destination;

    @Column(name = "return_enabled", nullable = false)
    private boolean returnEnabled;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Richiesto da JPA per ricostruire l'entità dal database.
    protected Route() {
    }

    public Route(
            UUID id,
            String originCity,
            DestinationCity destination,
            boolean returnEnabled,
            Instant createdAt
    ) {
        this.id = id;
        this.originCity = originCity;
        this.createdAt = createdAt;

        updateDetails(destination, returnEnabled);
    }

    // Aggiorna i dati modificabili mantenendo obbligatoria la destinazione.
    public void updateDetails(
            DestinationCity destination,
            boolean returnEnabled
    ) {
        this.destination = Objects.requireNonNull(
                destination,
                "destination must not be null"
        );
        this.returnEnabled = returnEnabled;
    }

    public void changeStatus(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginCity() {
        return originCity;
    }

    public DestinationCity getDestination() {
        return destination;
    }

    // Proprietà calcolata: il nome è conservato nel catalogo.
    public String getDestinationCity() {
        return destination.getName();
    }

    public RouteCountry getDestinationCountryCode() {
        return destination.getCountryCode();
    }

    public RouteType getRouteType() {
        return getDestinationCountryCode() == RouteCountry.IT
                ? RouteType.NAZIONALE
                : RouteType.INTERNAZIONALE;
    }

    public boolean isReturnEnabled() {
        return returnEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}