package tqs.zeromonos.isolationtests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.zeromonos.data.*;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingServiceImp;
import tqs.zeromonos.utils.TestDateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes para BookingService usando Mocks.
 * Testa o fluxo completo mockando os repositórios.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {


    Logger  logger = Logger.getLogger(BookingServiceTest.class.getName());
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @InjectMocks
    private BookingServiceImp bookingService;

    private Municipality testMunicipality;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        // Cria município de teste
        testMunicipality = new Municipality();
        testMunicipality.setName("Lisboa");

        // Cria booking de teste
        testBooking = new Booking();
        testBooking.setMunicipality(testMunicipality);
        testBooking.setRequestedDate(TestDateUtils.getNextValidDate());
        testBooking.setTimeSlot(TimeSlot.MORNING);
        testBooking.setDescription("Test booking");
        testBooking.setStatus(BookingStatus.RECEIVED);
    }

    /**
     * Testa a criação de uma reserva válida.
     */
    @Test
    void whenCreateValidBooking_thenSuccess() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Recolha de resíduos recicláveis");

        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponseDTO response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals("Lisboa", response.getMunicipalityName());
        assertEquals(TimeSlot.MORNING, response.getTimeSlot());
        assertEquals(BookingStatus.RECEIVED, response.getStatus());
        assertEquals("Recolha de resíduos recicláveis", response.getDescription());
        
        verify(municipalityRepository, times(1)).findByName("Lisboa");
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    /**
     * Testa criação de reserva com município inexistente.
     */
    @Test
    void whenCreateBookingWithInvalidMunicipality_thenThrowException() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("MunicípioInválido");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        when(municipalityRepository.findByName("MunicípioInválido")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));
        
        assertTrue(exception.getMessage().contains("não encontrado"));
        verify(municipalityRepository, times(1)).findByName("MunicípioInválido");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Testa validação de data no passado.
     */
    @Test
    void whenCreateBookingWithPastDate_thenThrowException() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getPastDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));
        
        assertTrue(exception.getMessage().contains("passado"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Testa validação de data para o mesmo dia.
     */
    @Test
    void whenCreateBookingForToday_thenThrowException() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getToday());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));
        
        assertTrue(exception.getMessage().contains("mesmo dia"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Testa validação de reserva para Domingo.
     */
    @Test
    void whenCreateBookingForSunday_thenThrowException() {
        LocalDate nextSunday = TestDateUtils.getNextSunday();

        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(nextSunday);
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));
        
        assertTrue(exception.getMessage().contains("fim-de-semana"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Testa limite máximo de reservas por município.
     */
    @Test
    void whenExceedMaxBookings_thenThrowException() {
        LocalDate validDate = TestDateUtils.getNextValidDate();

        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(validDate);
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Reserva excedente");

        // Simula que já existem 20 reservas (limite máximo = 20)
        List<Booking> existingBookings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Booking booking = new Booking();
            booking.setRequestedDate(validDate);
            booking.setTimeSlot(TimeSlot.MORNING);
            booking.setMunicipality(testMunicipality);
            existingBookings.add(booking);
        }


        
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));
        // O repositório deve devolver a lista com 21 reservas
        when(bookingRepository.countByMunicipality(testMunicipality)).thenReturn(existingBookings.size());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> bookingService.createBooking(request));

        assertTrue(exception.getMessage().toLowerCase().contains("máximo"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }


    /**
     * Testa busca de reserva por token válido.
     */
    @Test
    void whenGetBookingByValidToken_thenReturnBooking() {
        String testToken = "test-token-123";
        
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));

        BookingResponseDTO found = bookingService.getBookingByToken(testToken);

        assertNotNull(found);
        assertEquals("Lisboa", found.getMunicipalityName());
        verify(bookingRepository, times(1)).findByToken(testToken);
    }

    /**
     * Testa busca com token inexistente.
     */
    @Test
    void whenGetBookingByInvalidToken_thenThrowException() {
        String invalidToken = "token-invalido";
        
        when(bookingRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> bookingService.getBookingByToken(invalidToken));
        
        assertTrue(exception.getMessage().contains("não encontrada"));
        verify(bookingRepository, times(1)).findByToken(invalidToken);
    }

    /**
     * Testa cancelamento de reserva em estado RECEIVED.
     */
    @Test
    void whenCancelBookingInReceivedState_thenSuccess() {
        String testToken = "test-token-123";
        testBooking.setStatus(BookingStatus.RECEIVED);
        
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        assertDoesNotThrow(() -> bookingService.cancelBooking(testToken));

        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        verify(bookingRepository, times(1)).save(testBooking);
    }

    /**
     * Testa cancelamento de reserva em estado ASSIGNED.
     */
    @Test
    void whenCancelBookingInAssignedState_thenSuccess() {
        String testToken = "test-token-123";
        testBooking.setStatus(BookingStatus.ASSIGNED);
        
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        assertDoesNotThrow(() -> bookingService.cancelBooking(testToken));

        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        verify(bookingRepository, times(1)).save(testBooking);
    }

    /**
     * Testa que não se pode cancelar reserva COMPLETED.
     */
    @Test
    void whenCancelCompletedBooking_thenThrowException() {
        String testToken = "test-token-123";
        testBooking.setStatus(BookingStatus.COMPLETED);
        
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> bookingService.cancelBooking(testToken));
        
        assertTrue(exception.getMessage().contains("não pode ser cancelada"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Testa listagem de municípios disponíveis.
     */
    @Test
    void whenGetAvailableMunicipalities_thenReturnList() {
        Municipality porto = new Municipality();
        porto.setName("Porto");

        Municipality coimbra = new Municipality();
        coimbra.setName("Coimbra");

        when(municipalityRepository.findAll()).thenReturn(Arrays.asList(testMunicipality, porto, coimbra));

        List<String> municipalities = bookingService.getAvailableMunicipalities();

        assertNotNull(municipalities);
        assertEquals(3, municipalities.size());
        assertTrue(municipalities.contains("Lisboa"));
        assertTrue(municipalities.contains("Porto"));
        assertTrue(municipalities.contains("Coimbra"));
        verify(municipalityRepository, times(1)).findAll();
    }

    /**
     * Testa listagem para staff filtrada por município.
     */
    @Test
    void whenListForStaffByMunicipality_thenReturnFiltered() {
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));
        when(bookingRepository.findByMunicipality(testMunicipality)).thenReturn(Arrays.asList(testBooking));

        List<BookingResponseDTO> bookings = bookingService.listForStaff("Lisboa");

        assertNotNull(bookings);
        assertEquals(1, bookings.size());
        assertEquals("Lisboa", bookings.get(0).getMunicipalityName());
        verify(municipalityRepository, times(1)).findByName("Lisboa");
        verify(bookingRepository, times(1)).findByMunicipality(testMunicipality);
    }

    /**
     * Testa listagem de todas as reservas para staff.
     */
    @Test
    void whenListForStaffWithTodas_thenReturnAll() {
        Booking booking1 = new Booking();
        booking1.setMunicipality(testMunicipality);
        booking1.setRequestedDate(TestDateUtils.getNextValidDate());
        booking1.setTimeSlot(TimeSlot.MORNING);
        booking1.setDescription("Test 1");
        booking1.setStatus(BookingStatus.RECEIVED);

        Booking booking2 = new Booking();
        Municipality porto = new Municipality();
        porto.setName("Porto");
        booking2.setMunicipality(porto);
        booking2.setRequestedDate(TestDateUtils.getNextValidDate());
        booking2.setTimeSlot(TimeSlot.NIGHT);
        booking2.setDescription("Test 2");
        booking2.setStatus(BookingStatus.ASSIGNED);

        when(bookingRepository.findAll()).thenReturn(Arrays.asList(booking1, booking2));

        List<BookingResponseDTO> allBookings = bookingService.listForStaff("todas");

        assertNotNull(allBookings);
        assertEquals(2, allBookings.size());
        verify(bookingRepository, times(1)).findAll();
    }


    /**
     * Testa atualização de status para staff.
     */
    @Test
    void whenUpdateBookingStatus_thenSuccess() {
        String testToken = "test-token-123";
        testBooking.setStatus(BookingStatus.RECEIVED);
        
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        BookingResponseDTO updated = bookingService.updateBookingStatusForStaff(testToken, BookingStatus.ASSIGNED);

        assertEquals(BookingStatus.ASSIGNED, updated.getStatus());
        verify(bookingRepository, times(1)).save(testBooking);
    }

    /**
     * Testa atualização de status com token inválido.
     */
    @Test
    void whenUpdateInvalidBookingStatus_thenThrowException() {
        String invalidToken = "invalid-token";
        
        when(bookingRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> bookingService.updateBookingStatusForStaff(invalidToken, BookingStatus.ASSIGNED));
        
        assertTrue(exception.getMessage().contains("não encontrada"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}