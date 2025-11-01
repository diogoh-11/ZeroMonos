package tqs.zeromonos;

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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplUnitTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @InjectMocks
    private BookingServiceImp bookingService;

    private Municipality testMunicipality;
    private Booking testBooking;

    /**
     * Inicializa objetos reutilizáveis antes de cada teste.
     * - testMunicipality: município de exemplo ("Lisboa")
     * - testBooking: reserva de exemplo com data no futuro e estado RECEIVED
     */
    @BeforeEach
    void setUp() {
        // Criar município de teste
        testMunicipality = new Municipality();
        testMunicipality.setName("Lisboa");

        // Criar booking de teste - sem setId/setToken pois são auto-gerados
        testBooking = new Booking();
        testBooking.setMunicipality(testMunicipality);
        testBooking.setRequestedDate(TestDateUtils.getNextValidDate());
        testBooking.setTimeSlot(TimeSlot.MORNING);
        testBooking.setDescription("Test description");
        testBooking.setStatus(BookingStatus.RECEIVED);
    }

    /**
     * Testa o comportamento quando o município indicado na requisição não existe.
     * Espera-se que o serviço lance IllegalArgumentException (ou um erro equivalente)
     * indicando que o município não foi encontrado.
     */
    @Test
    void createBooking_ThrowsWhenMunicipalityNotFound() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Invalid Municipality");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test description");

        when(municipalityRepository.findByName("Invalid Municipality")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));
        assertTrue(exception.getMessage().contains("não encontrado") ||
                   exception.getMessage().contains("Município"));
    }

    /**
     * Testa o caminho feliz de criação de booking:
     * - Município existe
     * - Repositório persiste o booking
     * Verifica-se que a resposta contém os dados esperados e que save foi invocado.
     */
    @Test
    void createBooking_Success() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test description");

        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            // Retorna o booking que foi salvo (ID e token seriam gerados pelo DB)
            return invocation.getArgument(0);
        });

        BookingResponseDTO response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals("Lisboa", response.getMunicipalityName());
        assertEquals(request.getRequestedDate(), response.getRequestedDate());
        assertEquals(TimeSlot.MORNING, response.getTimeSlot());
        assertEquals("Test description", response.getDescription());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    /**
     * Testa cancelamento quando o token não corresponde a nenhuma reserva.
     * O serviço deve lançar NoSuchElementException (ou equivalente) indicando não encontrado.
     */
    @Test
    void cancelBooking_ThrowsWhenBookingNotFound() {
        String testToken = "test-token-123";
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // O serviço lança NoSuchElementException, não IllegalArgumentException
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> bookingService.cancelBooking(testToken));
        assertTrue(exception.getMessage().contains("não encontrada") ||
                   exception.getMessage().contains("Reserva"));
    }

    /**
     * Testa que não é possível cancelar uma reserva já em estado final (COMPLETED).
     * Espera IllegalStateException (ou equivalente) indicando que não se pode cancelar.
     */
    @Test
    void cancelBooking_ThrowsWhenInvalidStatus() {
        String testToken = "test-token-123";
        Booking completedBooking = new Booking();
        completedBooking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(completedBooking));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> bookingService.cancelBooking(testToken));
        assertTrue(exception.getMessage().contains("não pode ser cancelada"));
    }

    /**
     * Testa cancelamento bem-sucedido:
     * - Reserva existe e está num estado cancelável -> após o método o estado deve ser CANCELLED
     * - Verifica que o repositório save foi invocado
     */
    @Test
    void cancelBooking_Success() {
        String testToken = "test-token-123";
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        assertDoesNotThrow(() -> bookingService.cancelBooking(testToken));
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        verify(bookingRepository, times(1)).save(testBooking);
    }

    /**
     * Testa cancelamento bem-sucedido quando a reserva está no estado ASSIGNED.
     * Verifica que após o cancelamento o estado passa para CANCELLED.
     */
    @Test
    void cancelBooking_SuccessWithAssignedStatus() {
        String testToken = "test-token-456";
        testBooking.setStatus(BookingStatus.ASSIGNED);
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        assertDoesNotThrow(() -> bookingService.cancelBooking(testToken));
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
    }

    /**
     * Testa que o serviço devolve a lista de municípios disponíveis (apenas nomes).
     * Simula municipalityRepository.findAll() e verifica a transformação para List<String>.
     */
    @Test
    void getAvailableMunicipalities_ReturnsNames() {
        Municipality mun1 = new Municipality();
        mun1.setName("Lisboa");
        Municipality mun2 = new Municipality();
        mun2.setName("Porto");

        when(municipalityRepository.findAll()).thenReturn(List.of(mun1, mun2));

        List<String> municipalities = bookingService.getAvailableMunicipalities();

        assertEquals(2, municipalities.size());
        assertTrue(municipalities.contains("Lisboa"));
        assertTrue(municipalities.contains("Porto"));
    }

    /**
     * Testa obtenção de reserva por token (caminho feliz).
     * Verifica que os dados do DTO correspondem aos da entidade mock.
     */
    @Test
    void getBookingByToken_Success() {
        String testToken = "valid-token-789";
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));

        BookingResponseDTO response = bookingService.getBookingByToken(testToken);

        assertNotNull(response);
        assertEquals("Lisboa", response.getMunicipalityName());
        verify(bookingRepository, times(1)).findByToken(testToken);
    }

    /**
     * Testa o comportamento quando procura-se um token inexistente.
     * Espera NoSuchElementException (ou equivalente) a indicar que não foi encontrada.
     */
    @Test
    void getBookingByToken_ThrowsWhenNotFound() {
        String invalidToken = "invalid-token";
        when(bookingRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // O serviço lança NoSuchElementException, não IllegalArgumentException
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> bookingService.getBookingByToken(invalidToken));
        assertTrue(exception.getMessage().contains("não encontrada"));
    }


    /**
     * Testa que listForStaff("todas") devolve todas as reservas (caso especial aceito pela implementação).
     */
    @Test
    void listForStaff_ReturnsAllWhenMunicipalityIsTodas() {
        List<Booking> allBookings = List.of(testBooking);
        when(bookingRepository.findAll()).thenReturn(allBookings);

        List<BookingResponseDTO> result = bookingService.listForStaff("todas");

        assertEquals(1, result.size());
        verify(bookingRepository, times(1)).findAll();
    }

    /**
     * Testa filtragem por município no método listForStaff.
     * Simula obtenção do município e busca por municipality no repositório de bookings.
     */
    @Test
    void listForStaff_FiltersByMunicipality() {
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(testMunicipality));
        when(bookingRepository.findByMunicipality(testMunicipality)).thenReturn(List.of(testBooking));

        List<BookingResponseDTO> result = bookingService.listForStaff("Lisboa");

        assertEquals(1, result.size());
        assertEquals("Lisboa", result.get(0).getMunicipalityName());
    }

    /**
     * Testa atualização de estado por staff (caminho feliz).
     * Verifica que o estado da entidade é alterado e que o save é invocado.
     */
    @Test
    void updateBookingStatus_Success() {
        String testToken = "test-token-789";
        when(bookingRepository.findByToken(testToken)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        assertDoesNotThrow(() -> bookingService.updateBookingStatusForStaff(testToken, BookingStatus.ASSIGNED));
        assertEquals(BookingStatus.ASSIGNED, testBooking.getStatus());
        verify(bookingRepository, times(1)).save(testBooking);
    }

    /**
     * Testa atualização de estado para token inexistente.
     * Espera NoSuchElementException para indicar que a reserva não foi encontrada.
     */
    @Test
    void updateBookingStatus_ThrowsWhenBookingNotFound() {
        String invalidToken = "invalid-token";
        when(bookingRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // O serviço lança NoSuchElementException, não IllegalArgumentException
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> bookingService.updateBookingStatusForStaff(invalidToken, BookingStatus.ASSIGNED));
        assertTrue(exception.getMessage().contains("não encontrada"));
    }
}
