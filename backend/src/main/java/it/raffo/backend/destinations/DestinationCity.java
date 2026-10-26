package it.raffo.backend.destinations;

import it.raffo.backend.routes.RouteCountry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Località del catalogo geografico importato da GeoNames.
 * Il paese appartiene alla località e verrà ricavato da questa
 * quando si crea o modifica una tratta.
 */
@Entity
@Table(name = "destination_cities")
public class DestinationCity {

    // ID assegnato da GeoNames, non generato dall'applicazione.
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "country_code", nullable = false, length = 2)
    private RouteCountry countryCode;

    @Column(name = "admin1_name", length = 200)
    private String admin1Name;

    @Column(name = "admin2_name", length = 200)
    private String admin2Name;

    @Column(
            name = "latitude",
            nullable = false,
            precision = 9,
            scale = 6
    )
    private BigDecimal latitude;

    @Column(
            name = "longitude",
            nullable = false,
            precision = 9,
            scale = 6
    )
    private BigDecimal longitude;

    // Richiesto da JPA per ricostruire l'entità letta dal database.
    protected DestinationCity() {
    }

    public DestinationCity(
            Long id,
            String name,
            RouteCountry countryCode,
            String admin1Name,
            String admin2Name,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
        this.admin1Name = admin1Name;
        this.admin2Name = admin2Name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RouteCountry getCountryCode() {
        return countryCode;
    }

    public String getAdmin1Name() {
        return admin1Name;
    }

    public String getAdmin2Name() {
        return admin2Name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }
}