package com.jairomatias.eventix.venue.entity;

import java.math.BigDecimal;

import com.jairomatias.eventix.shared.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "venue_seats")
public class VenueSeat extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "row_id", nullable = false)
    private VenueRow row;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false)
    private boolean accessible;

    @Column(name = "companion_seat", nullable = false)
    private boolean companionSeat;

    @Column(name = "x_position", precision = 10, scale = 4)
    private BigDecimal xPosition;

    @Column(name = "y_position", precision = 10, scale = 4)
    private BigDecimal yPosition;

    @Column(nullable = false)
    private boolean active = true;

    protected VenueSeat() {
    }

    public VenueSeat(
            VenueRow row,
            String seatNumber,
            String label,
            boolean accessible,
            boolean companionSeat) {
        this.row = row;
        update(seatNumber, label, accessible, companionSeat, null, null, true);
    }

    public void update(
            String seatNumber,
            String label,
            boolean accessible,
            boolean companionSeat,
            BigDecimal xPosition,
            BigDecimal yPosition,
            boolean active) {
        this.seatNumber = seatNumber;
        this.label = label;
        this.accessible = accessible;
        this.companionSeat = companionSeat;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.active = active;
    }

    public VenueRow getRow() {
        return row;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public boolean isCompanionSeat() {
        return companionSeat;
    }

    public BigDecimal getXPosition() {
        return xPosition;
    }

    public BigDecimal getYPosition() {
        return yPosition;
    }

    public boolean isActive() {
        return active;
    }
}
