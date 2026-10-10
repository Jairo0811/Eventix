package com.jairomatias.eventix.venue.entity;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "venues")
public class Venue extends AuditableEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone;

    @Column(nullable = false)
    private boolean active = true;

    protected Venue() {
    }

    public Venue(String name, String address, String city, String countryCode, String timeZone) {
        update(name, address, city, countryCode, timeZone, true);
    }

    public void update(
            String name,
            String address,
            String city,
            String countryCode,
            String timeZone,
            boolean active) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.countryCode = countryCode;
        this.timeZone = timeZone;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public boolean isActive() {
        return active;
    }
}
