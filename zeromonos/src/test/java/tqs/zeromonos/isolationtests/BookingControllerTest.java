package tqs.zeromonos.isolationtests;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tqs.zeromonos.boundary.BookingController;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.TimeSlot;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;
import tqs.zeromonos.utils.TestDateUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes para o BookingController.
 * Usa @WebMvcTest para testar apenas a camada web, mockando o serviço.
 */
@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookingService bookingService;


    /**
     * Testa a criação de uma nova reserva (POST /api/bookings).
     * Verifica que o serviço é invocado e que a resposta contém o token gerado.
     */
    @Test
    void whenPostBooking_thenCreateBooking() throws Exception {
        java.time.LocalDate validDate = TestDateUtils.getNextValidDate();
        
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(validDate);
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test booking");

        BookingResponseDTO response = new BookingResponseDTO();
        response.setId(UUID.randomUUID());
        response.setToken("test-token-123");
        response.setMunicipalityName("Lisboa");
        response.setRequestedDate(validDate);
        response.setTimeSlot(TimeSlot.MORNING);
        response.setDescription("Test booking");
        response.setStatus(BookingStatus.RECEIVED);
        response.setCreatedAt(OffsetDateTime.now());
        response.setUpdatedAt(OffsetDateTime.now());
        response.setHistory(Collections.emptyList());

        when(bookingService.createBooking(any(BookingRequestDTO.class))).thenReturn(response);

        mvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("test-token-123")))
                .andExpect(jsonPath("$.municipalityName", is("Lisboa")))
                .andExpect(jsonPath("$.status", is("RECEIVED")));

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    /**
     * Testa criação de reserva com data inválida (domingo).
     */
    @Test
    void whenPostBookingForSunday_thenBadRequest() throws Exception {
        java.time.LocalDate sunday = TestDateUtils.getNextSunday();
        
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(sunday);
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test booking");

        when(bookingService.createBooking(any(BookingRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Não é possível agendar para fim-de-semana"));

        mvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    /**
     * Testa criação de reserva com data no passado.
     */
    @Test
    void whenPostBookingWithPastDate_thenBadRequest() throws Exception {
        java.time.LocalDate pastDate = TestDateUtils.getPastDate();
        
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(pastDate);
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test booking");

        when(bookingService.createBooking(any(BookingRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("A data não pode estar no passado"));

        mvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    /**
     * Testa a obtenção de uma reserva pelo token (GET /api/bookings/{token}).
     * Verifica que o serviço retorna os dados corretos.
     */
    @Test
    void whenGetBookingByToken_thenReturnBooking() throws Exception {
        String testToken = "test-token-123";
        java.time.LocalDate validDate = TestDateUtils.getNextValidDate();
        
        BookingResponseDTO response = new BookingResponseDTO();
        response.setId(UUID.randomUUID());
        response.setToken(testToken);
        response.setMunicipalityName("Lisboa");
        response.setRequestedDate(validDate);
        response.setTimeSlot(TimeSlot.MORNING);
        response.setDescription("Test booking");
        response.setStatus(BookingStatus.RECEIVED);
        response.setCreatedAt(OffsetDateTime.now());
        response.setUpdatedAt(OffsetDateTime.now());
        response.setHistory(Collections.emptyList());

        when(bookingService.getBookingByToken(testToken)).thenReturn(response);

        mvc.perform(
                get("/api/bookings/" + testToken)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(testToken)))
                .andExpect(jsonPath("$.municipalityName", is("Lisboa")));

        verify(bookingService, times(1)).getBookingByToken(testToken);
    }

    /**
     * Testa o comportamento quando procura-se uma reserva inexistente.
     * Espera status 404 (Not Found).
     */
    @Test
    void whenGetBookingByInvalidToken_thenNotFound() throws Exception {
        String invalidToken = "invalid-token";

        when(bookingService.getBookingByToken(invalidToken))
            .thenThrow(new NoSuchElementException("Reserva não encontrada"));

        mvc.perform(
                get("/api/bookings/" + invalidToken)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).getBookingByToken(invalidToken);
    }

    /**
     * Testa o cancelamento de uma reserva (PUT /api/bookings/{token}/cancel).
     * Verifica que o serviço é invocado e retorna status 204.
     */
    @Test
    void whenCancelBooking_thenSucceed() throws Exception {
        String testToken = "test-token-123";

        doNothing().when(bookingService).cancelBooking(testToken);

        mvc.perform(
                put("/api/bookings/" + testToken + "/cancel")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(bookingService, times(1)).cancelBooking(testToken);
    }

    /**
     * Testa cancelamento de reserva inexistente.
     * Espera status 404.
     */
    @Test
    void whenCancelInvalidBooking_thenNotFound() throws Exception {
        String invalidToken = "invalid-token";

        doThrow(new NoSuchElementException("Reserva não encontrada"))
            .when(bookingService).cancelBooking(invalidToken);

        mvc.perform(
                put("/api/bookings/" + invalidToken + "/cancel")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).cancelBooking(invalidToken);
    }

    /**
     * Testa cancelamento de reserva que não pode ser cancelada.
     * Espera status 409 (Conflict).
     */
    @Test
    void whenCancelCompletedBooking_thenConflict() throws Exception {
        String testToken = "test-token-123";

        doThrow(new IllegalStateException("Reserva não pode ser cancelada"))
            .when(bookingService).cancelBooking(testToken);

        mvc.perform(
                put("/api/bookings/" + testToken + "/cancel")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        verify(bookingService, times(1)).cancelBooking(testToken);
    }

    /**
     * Testa a listagem de municípios disponíveis (GET /api/bookings/municipalities).
     */
    @Test
    void whenGetMunicipalities_thenReturnList() throws Exception {
        List<String> municipalities = Arrays.asList("Lisboa", "Porto", "Coimbra");

        when(bookingService.getAvailableMunicipalities()).thenReturn(municipalities);

        mvc.perform(
                get("/api/bookings/municipalities")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", is("Lisboa")))
                .andExpect(jsonPath("$[1]", is("Porto")))
                .andExpect(jsonPath("$[2]", is("Coimbra")));

        verify(bookingService, times(1)).getAvailableMunicipalities();
    }

    /**
     * Testa criação de reserva com município inválido.
     */
    @Test
    void whenPostBookingWithInvalidMunicipality_thenBadRequest() throws Exception {
        java.time.LocalDate validDate = TestDateUtils.getNextValidDate();
        
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("MunicípioInexistente");
        request.setRequestedDate(validDate);
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test booking");

        when(bookingService.createBooking(any(BookingRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Município não encontrado"));

        mvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

   
}