package tqs.zeromonos.data;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_state_changes")
public class StateChange {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(nullable = false)
    private OffsetDateTime timestamp;


    public StateChange() {}

    public StateChange(BookingStatus status, OffsetDateTime timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    // getters/setters
    public UUID getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
