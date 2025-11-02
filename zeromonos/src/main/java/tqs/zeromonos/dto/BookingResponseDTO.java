package tqs.zeromonos.dto;

import tqs.zeromonos.data.Booking;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.TimeSlot;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BookingResponseDTO {
    private UUID id;
    private String token;
    private String municipalityName;
    private String description;
    private LocalDate requestedDate;
    private TimeSlot timeSlot;
    private BookingStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<String> history; // lista simples timestamp + estado

    public BookingResponseDTO() {
        // Construtor vazio necessário para serialização/desserialização pelo Jackson
        // e frameworks de persistência
    }

    public static BookingResponseDTO fromEntity(Booking b) {
        if (b == null) return null;
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(b.getId());
        dto.setToken(b.getToken());
        if (b.getMunicipality() != null) {
            dto.setMunicipalityName(b.getMunicipality().getName());
        }
        dto.setDescription(b.getDescription());
        dto.setRequestedDate(b.getRequestedDate());
        dto.setTimeSlot(b.getTimeSlot());
        dto.setStatus(b.getStatus());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setUpdatedAt(b.getUpdatedAt());

        if (b.getHistory() != null) {
            dto.setHistory(b.getHistory().stream()
                .map(sc -> {
                    String ts = sc.getTimestamp() != null ? sc.getTimestamp().toString() : "unknown-ts";
                    String st = sc.getStatus() != null ? sc.getStatus().name() : "UNKNOWN";
                    return ts + " - " + st;
                })
                .collect(Collectors.toCollection(ArrayList::new))); // Explicitly modifiable
        }

        return dto;
    }

    // getters / setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getHistory() { return history; }
    public void setHistory(List<String> history) { this.history = history; }
}