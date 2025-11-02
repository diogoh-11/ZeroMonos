package tqs.zeromonos.dto;


import tqs.zeromonos.data.TimeSlot;
import java.time.LocalDate;

public class BookingRequestDTO {
    private String municipalityName;
    private LocalDate requestedDate;
    private TimeSlot timeSlot;
    private String description;

    public BookingRequestDTO() {
        // Construtor vazio necessário para serialização/desserialização pelo Jackson
        // e frameworks de persistência
    }

    

    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
