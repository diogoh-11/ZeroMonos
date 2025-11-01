package tqs.zeromonos.services;


import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.data.BookingStatus;
import java.util.List;

public interface BookingService {
    BookingResponseDTO createBooking(BookingRequestDTO request);
    BookingResponseDTO getBookingByToken(String token);
    void cancelBooking(String token);
    List<String> getAvailableMunicipalities();

    // Staff methods
    List<BookingResponseDTO> listForStaff(String municipalityCode);
    BookingResponseDTO updateBookingStatusForStaff(String token, BookingStatus newStatus);
}