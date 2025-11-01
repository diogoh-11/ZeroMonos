package tqs.zeromonos.data;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository 
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByToken(String token);
    List<Booking> findByMunicipalityName(String municipalityName); 
    List<Booking> findByMunicipality(Municipality municipality);
    long countByMunicipalityAndRequestedDateAndTimeSlot(Municipality municipality, LocalDate requestedDate, TimeSlot timeSlot);
    List<Booking> findByRequestedDateAndMunicipality(LocalDate requestedDate, Municipality municipality);
    List<Booking> findByMunicipalityAndRequestedDateAndTimeSlot(Municipality municipality, LocalDate requestedDate, TimeSlot timeSlot);
    int countByMunicipality(Municipality municipality);
}
