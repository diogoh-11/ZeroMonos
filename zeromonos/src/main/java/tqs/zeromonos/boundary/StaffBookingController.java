package tqs.zeromonos.boundary;


import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/bookings")
public class StaffBookingController {

    private final BookingService bookingService;

    public StaffBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Listagem com filtros úteis para staff
    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> list(
            @RequestParam(value = "municipality", required = false) String municipalityName
    ) {
        List<BookingResponseDTO> list = bookingService.listForStaff(municipalityName);
        return ResponseEntity.ok(list);
    }


    // Atualizar estado (staff)
    @PatchMapping("/{token}/status")
    public ResponseEntity<BookingResponseDTO> updateStatus(
            @PathVariable String token,
            @RequestParam("status") BookingStatus status,
            @RequestParam(value = "note", required = false) String note
    ) {
        return ResponseEntity.ok(bookingService.updateBookingStatusForStaff(token, status));
    }

}
