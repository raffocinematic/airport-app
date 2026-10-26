package it.raffo.backend.destinations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Nome utilizzabile per cercare una città del catalogo.
 * Alias differenti possono riferirsi alla stessa località.
 */
@Entity
@Table(name = "destination_city_aliases")
public class DestinationCityAlias {

    // Identificativo interno generato dal database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // L'alias si riferisce a una città già presente nel catalogo.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private DestinationCity city;

    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 400)
    private String normalizedName;

    protected DestinationCityAlias() {
    }

    public DestinationCityAlias(
            DestinationCity city,
            String name,
            String normalizedName
    ) {
        this.city = city;
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public Long getId() {
        return id;
    }

    public DestinationCity getCity() {
        return city;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }
}