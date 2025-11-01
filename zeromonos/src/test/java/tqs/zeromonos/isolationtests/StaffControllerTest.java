package tqs.zeromonos.isolationtests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tqs.zeromonos.boundary.StaffBookingController;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.TimeSlot;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o StaffBookingController.
 * Testa endpoints específicos para a equipe de staff.
 */
@WebMvcTest(StaffBookingController.class)
class StaffControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookingService bookingService;

    /**
     * Testa a listagem de todas as reservas sem filtro.
     */
    @Test
    void whenGetAllBookings_thenReturnList() throws Exception {
        BookingResponseDTO booking1 = createBookingResponse("token1", "Lisboa", BookingStatus.RECEIVED);
        BookingResponseDTO booking2 = createBookingResponse("token2", "Porto", BookingStatus.ASSIGNED);

        when(bookingService.listForStaff("todas")).thenReturn(Arrays.asList(booking1, booking2));

        mvc.perform(
                get("/api/staff/bookings")
                    .param("municipality", "todas")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].token", is("token1")))
                .andExpect(jsonPath("$[0].municipalityName", is("Lisboa")))
                .andExpect(jsonPath("$[1].token", is("token2")))
                .andExpect(jsonPath("$[1].municipalityName", is("Porto")));

        verify(bookingService, times(1)).listForStaff("todas");
    }

    /**
     * Testa a listagem de reservas filtrada por município específico.
     */
    @Test
    void whenGetBookingsByMunicipality_thenReturnFilteredList() throws Exception {
        BookingResponseDTO booking = createBookingResponse("token1", "Lisboa", BookingStatus.RECEIVED);

        when(bookingService.listForStaff("Lisboa")).thenReturn(Collections.singletonList(booking));

        mvc.perform(
                get("/api/staff/bookings")
                    .param("municipality", "Lisboa")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].municipalityName", is("Lisboa")))
                .andExpect(jsonPath("$[0].status", is("RECEIVED")));

        verify(bookingService, times(1)).listForStaff("Lisboa");
    }

    /**
     * Testa a listagem quando não há parâmetro de município (deve listar todas).
     */
    @Test
    void whenGetBookingsWithoutFilter_thenReturnAll() throws Exception {
        BookingResponseDTO booking1 = createBookingResponse("token1", "Lisboa", BookingStatus.RECEIVED);
        BookingResponseDTO booking2 = createBookingResponse("token2", "Porto", BookingStatus.ASSIGNED);
        BookingResponseDTO booking3 = createBookingResponse("token3", "Coimbra", BookingStatus.COMPLETED);

        when(bookingService.listForStaff(null)).thenReturn(Arrays.asList(booking1, booking2, booking3));

        mvc.perform(
                get("/api/staff/bookings")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        verify(bookingService, times(1)).listForStaff(null);
    }

    /**
     * Testa a atualização do status de uma reserva para ASSIGNED.
     */
    @Test
    void whenUpdateBookingStatusToAssigned_thenSucceed() throws Exception {
        String testToken = "test-token-123";
        BookingStatus newStatus = BookingStatus.ASSIGNED;
        
        BookingResponseDTO updatedBooking = createBookingResponse(testToken, "Lisboa", newStatus);

        when(bookingService.updateBookingStatusForStaff(testToken, newStatus)).thenReturn(updatedBooking);

        mvc.perform(
                patch("/api/staff/bookings/" + testToken + "/status")
                    .param("status", newStatus.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(testToken)))
                .andExpect(jsonPath("$.status", is("ASSIGNED")));

        verify(bookingService, times(1)).updateBookingStatusForStaff(testToken, newStatus);
    }

    /**
     * Testa a atualização do status de uma reserva para COMPLETED.
     */
    @Test
    void whenUpdateBookingStatusToCompleted_thenSucceed() throws Exception {
        String testToken = "test-token-456";
        BookingStatus newStatus = BookingStatus.COMPLETED;
        
        BookingResponseDTO updatedBooking = createBookingResponse(testToken, "Porto", newStatus);

        when(bookingService.updateBookingStatusForStaff(testToken, newStatus)).thenReturn(updatedBooking);

        mvc.perform(
                patch("/api/staff/bookings/" + testToken + "/status")
                    .param("status", newStatus.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(bookingService, times(1)).updateBookingStatusForStaff(testToken, newStatus);
    }

    /**
     * Testa a atualização de status com token inexistente.
     * Espera status 404 (Not Found).
     */
    @Test
    void whenUpdateInvalidBookingStatus_thenNotFound() throws Exception {
        String invalidToken = "invalid-token";
        BookingStatus newStatus = BookingStatus.ASSIGNED;

        when(bookingService.updateBookingStatusForStaff(invalidToken, newStatus))
            .thenThrow(new NoSuchElementException("Reserva não encontrada"));

        mvc.perform(
                patch("/api/staff/bookings/" + invalidToken + "/status")
                    .param("status", newStatus.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).updateBookingStatusForStaff(invalidToken, newStatus);
    }

    /**
     * Testa a atualização de status com nota adicional (opcional).
     */
    @Test
    void whenUpdateBookingStatusWithNote_thenSucceed() throws Exception {
        String testToken = "test-token-789";
        BookingStatus newStatus = BookingStatus.CANCELLED;
        String note = "Cliente solicitou cancelamento";
        
        BookingResponseDTO updatedBooking = createBookingResponse(testToken, "Coimbra", newStatus);

        when(bookingService.updateBookingStatusForStaff(testToken, newStatus)).thenReturn(updatedBooking);

        mvc.perform(
                patch("/api/staff/bookings/" + testToken + "/status")
                    .param("status", newStatus.toString())
                    .param("note", note)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        verify(bookingService, times(1)).updateBookingStatusForStaff(testToken, newStatus);
    }

    /**
     * Testa listagem quando não há reservas.
     */
    @Test
    void whenGetBookingsWithNoResults_thenReturnEmptyList() throws Exception {
        when(bookingService.listForStaff("Faro")).thenReturn(Collections.emptyList());

        mvc.perform(
                get("/api/staff/bookings")
                    .param("municipality", "Faro")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(bookingService, times(1)).listForStaff("Faro");
    }

    /**
     * Método auxiliar para criar BookingResponseDTO de teste.
     */
    private BookingResponseDTO createBookingResponse(String token, String municipality, BookingStatus status) {
        BookingResponseDTO response = new BookingResponseDTO();
        response.setId(UUID.randomUUID());
        response.setToken(token);
        response.setMunicipalityName(municipality);
        response.setRequestedDate(LocalDate.now().plusDays(1));
        response.setTimeSlot(TimeSlot.MORNING);
        response.setDescription("Test booking for staff");
        response.setStatus(status);
        response.setCreatedAt(OffsetDateTime.now());
        response.setUpdatedAt(OffsetDateTime.now());
        response.setHistory(Collections.emptyList());
        return response;
    }
}