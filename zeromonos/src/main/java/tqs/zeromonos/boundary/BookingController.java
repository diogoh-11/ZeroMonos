package tqs.zeromonos.boundary;



import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }   

    // Criar reserva
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        BookingResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.ok(response);
    }

    // Consultar reserva por token (acesso público com token)
    @GetMapping("/{token}")
    public ResponseEntity<BookingResponseDTO> getBookingByToken(@PathVariable String token) {
        BookingResponseDTO response = bookingService.getBookingByToken(token);
        return ResponseEntity.ok(response);
    }


    // Cancelar por token
    @PutMapping("/{token}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable String token) {
        bookingService.cancelBooking(token);
        return ResponseEntity.noContent().build();
    }

    // Listar municípios disponíveis
    @GetMapping("/municipalities")
    public ResponseEntity<List<String>> getAvailableMunicipalities() {
        List<String> municipalities = bookingService.getAvailableMunicipalities();
        return ResponseEntity.ok(municipalities);
    }

}
