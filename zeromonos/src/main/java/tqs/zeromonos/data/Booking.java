package tqs.zeromonos.data;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    
    @Id
    @GeneratedValue
    private UUID id;

    // token público para o cidadão consultar/cancelar a reserva
    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(optional = false)
    @JoinColumn(name = "municipality_id")
    private Municipality municipality;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    // histórico: one-to-many para state changes
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StateChange> history = new ArrayList<>();

    public Booking() {}

    public Booking(Municipality municipality, String description, LocalDate requestedDate, TimeSlot timeSlot) {
        this.token = UUID.randomUUID().toString();
        this.municipality = municipality;
        this.description = description;
        this.requestedDate = requestedDate;
        this.timeSlot = timeSlot;
        this.status = BookingStatus.RECEIVED;
        this.createdAt = OffsetDateTime.now();
    }

    // getters/setters
    public UUID getId() { return id; }
    public String getToken() { return token; }
    public Municipality getMunicipality() { return municipality; }
    public void setMunicipality(Municipality municipality) { this.municipality = municipality; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<StateChange> getHistory() { return history; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public void addStateChange(StateChange sc) {
        sc.setBooking(this);
        history.add(sc);
        this.updatedAt = sc.getTimestamp();
        this.status = sc.getStatus();
    }
}
