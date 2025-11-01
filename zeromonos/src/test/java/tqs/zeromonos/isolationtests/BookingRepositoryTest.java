package tqs.zeromonos.isolationtests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import tqs.zeromonos.data.*;
import tqs.zeromonos.utils.TestDateUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes para BookingRepository usando @DataJpaTest.
 * Testa as operações de persistência com banco H2 em memória.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    private Municipality testMunicipality;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        // Limpa dados
        bookingRepository.deleteAll();
        municipalityRepository.deleteAll();

        // Cria e persiste município
        testMunicipality = new Municipality();
        testMunicipality.setName("Lisboa");
        entityManager.persistAndFlush(testMunicipality);

        // Cria booking de teste
        testBooking = createBooking(testMunicipality, 
                                    TestDateUtils.getNextValidDate(), 
                                    TimeSlot.MORNING, 
                                    "Test booking", 
                                    BookingStatus.RECEIVED);
    }

    /**
     * Método auxiliar para criar Booking com todos os campos obrigatórios.
     */
    private Booking createBooking(Municipality municipality, LocalDate requestedDate, 
                                  TimeSlot timeSlot, String description, BookingStatus status) {
        Booking booking = new Booking(municipality, description, requestedDate, timeSlot);
        booking.setStatus(status);
        return booking;
    }

    /**
     * Testa que o repositório injeta corretamente.
     */
    @Test
    void injectedComponentsAreNotNull() {
        assertThat(entityManager).isNotNull();
        assertThat(bookingRepository).isNotNull();
        assertThat(municipalityRepository).isNotNull();
    }

    /**
     * Testa save e findById.
     */
    @Test
    void whenSaveBooking_thenFindById() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Test booking");
        assertThat(found.get().getMunicipality().getName()).isEqualTo("Lisboa");
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.RECEIVED);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    /**
     * Testa busca por token.
     */
    @Test
    void whenFindByToken_thenReturnBooking() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();

        String token = saved.getToken();
        Optional<Booking> found = bookingRepository.findByToken(token);

        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo(token);
        assertThat(found.get().getDescription()).isEqualTo("Test booking");
    }

    /**
     * Testa busca por token inexistente.
     */
    @Test
    void whenFindByInvalidToken_thenReturnEmpty() {
        Optional<Booking> found = bookingRepository.findByToken("invalid-token");

        assertThat(found).isEmpty();
    }

    /**
     * Testa busca por município.
     */
    @Test
    void whenFindByMunicipality_thenReturnBookings() {
        Booking booking1 = createBooking(testMunicipality, 
                                        TestDateUtils.getNextValidDate(), 
                                        TimeSlot.MORNING, 
                                        "Booking 1", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking1);

        Booking booking2 = createBooking(testMunicipality, 
                                        TestDateUtils.getValidDateAfterDays(2), 
                                        TimeSlot.NIGHT, 
                                        "Booking 2", 
                                        BookingStatus.ASSIGNED);
        bookingRepository.save(booking2);

        entityManager.flush();

        List<Booking> found = bookingRepository.findByMunicipality(testMunicipality);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Booking::getDescription)
                .containsExactlyInAnyOrder("Booking 1", "Booking 2");
    }

    /**
     * Testa busca por município sem resultados.
     */
    @Test
    void whenFindByMunicipalityWithNoBookings_thenReturnEmpty() {
        Municipality porto = new Municipality();
        porto.setName("Porto");
        entityManager.persistAndFlush(porto);

        List<Booking> found = bookingRepository.findByMunicipality(porto);

        assertThat(found).isEmpty();
    }

    /**
     * Testa findAll com múltiplas reservas.
     */
    @Test
    void whenFindAll_thenReturnAllBookings() {
        Municipality porto = new Municipality();
        porto.setName("Porto");
        entityManager.persistAndFlush(porto);

        Booking booking1 = createBooking(testMunicipality, 
                                        TestDateUtils.getNextValidDate(), 
                                        TimeSlot.MORNING, 
                                        "Lisboa booking", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking1);

        Booking booking2 = createBooking(porto, 
                                        TestDateUtils.getValidDateAfterDays(2), 
                                        TimeSlot.NIGHT, 
                                        "Porto booking", 
                                        BookingStatus.ASSIGNED);
        bookingRepository.save(booking2);

        entityManager.flush();

        List<Booking> allBookings = bookingRepository.findAll();

        assertThat(allBookings).hasSize(2);
        assertThat(allBookings).extracting(b -> b.getMunicipality().getName())
                .containsExactlyInAnyOrder("Lisboa", "Porto");
    }

    /**
     * Testa atualização de status de uma reserva.
     */
    @Test
    void whenUpdateBookingStatus_thenPersistChanges() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();

        OffsetDateTime createdAt = saved.getCreatedAt();
        
        saved.setStatus(BookingStatus.COMPLETED);
        saved.setUpdatedAt(OffsetDateTime.now());
        bookingRepository.save(saved);
        entityManager.flush();
        entityManager.clear(); // Limpa cache para forçar busca no BD

        Optional<Booking> updated = bookingRepository.findById(saved.getId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        
        // Comparar apenas até segundos
        assertThat(updated.get().getCreatedAt().truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(createdAt.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Testa deleção de uma reserva.
     */
    @Test
    void whenDeleteBooking_thenRemoveFromDatabase() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();

        bookingRepository.delete(saved);
        entityManager.flush();

        Optional<Booking> deleted = bookingRepository.findById(saved.getId());

        assertThat(deleted).isEmpty();
    }

    /**
     * Testa filtragem por status.
     */
    @Test
    void whenFindByStatus_thenReturnMatchingBookings() {
        Booking received = createBooking(testMunicipality, 
                                        TestDateUtils.getNextValidDate(), 
                                        TimeSlot.MORNING, 
                                        "Received booking", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(received);

        Booking assigned = createBooking(testMunicipality, 
                                        TestDateUtils.getValidDateAfterDays(2), 
                                        TimeSlot.NIGHT, 
                                        "Assigned booking", 
                                        BookingStatus.ASSIGNED);
        bookingRepository.save(assigned);

        entityManager.flush();

        List<Booking> allBookings = bookingRepository.findAll();
        long receivedCount = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.RECEIVED)
                .count();
        long assignedCount = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.ASSIGNED)
                .count();

        assertThat(receivedCount).isEqualTo(1);
        assertThat(assignedCount).isEqualTo(1);
    }

    /**
     * Testa que o token é gerado automaticamente.
     */
    @Test
    void whenSaveBooking_thenTokenIsGenerated() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();

        assertThat(saved.getToken()).isNotNull();
        assertThat(saved.getToken()).isNotEmpty();
    }

    /**
     * Testa relacionamento com Municipality.
     */
    @Test
    void whenSaveBooking_thenMunicipalityIsPreserved() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();
        entityManager.clear();

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMunicipality()).isNotNull();
        assertThat(found.get().getMunicipality().getName()).isEqualTo("Lisboa");
    }

    /**
     * Testa contagem de reservas por município e data usando método do repositório.
     */
    @Test
    void whenCountByMunicipalityAndDate_thenReturnCorrectCount() {
        LocalDate targetDate = TestDateUtils.getValidDateAfterDays(3);

        Booking booking1 = createBooking(testMunicipality, 
                                        targetDate, 
                                        TimeSlot.MORNING, 
                                        "Morning booking", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking1);

        Booking booking2 = createBooking(testMunicipality, 
                                        targetDate, 
                                        TimeSlot.MORNING, 
                                        "Another morning booking", 
                                        BookingStatus.ASSIGNED);
        bookingRepository.save(booking2);

        Booking booking3 = createBooking(testMunicipality, 
                                        targetDate, 
                                        TimeSlot.NIGHT, 
                                        "Afternoon booking", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking3);

        entityManager.flush();

        // Usa o método do repositório para contar
        long morningBookings = bookingRepository.countByMunicipalityAndRequestedDateAndTimeSlot(
            testMunicipality, targetDate, TimeSlot.MORNING);

        assertThat(morningBookings).isEqualTo(2);
    }

    /**
     * Testa que createdAt é preenchido automaticamente.
     */
    @Test
    void whenSaveBooking_thenTimestampsAreSet() {
        Booking saved = bookingRepository.save(testBooking);
        entityManager.flush();

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getToken()).isNotNull();
    }

    /**
     * Testa busca por município e data.
     */
    @Test
    void whenFindByRequestedDateAndMunicipality_thenReturnBookings() {
        LocalDate targetDate = TestDateUtils.getValidDateAfterDays(5);

        Booking booking1 = createBooking(testMunicipality, 
                                        targetDate, 
                                        TimeSlot.MORNING, 
                                        "Morning booking", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking1);

        Booking booking2 = createBooking(testMunicipality, 
                                        targetDate, 
                                        TimeSlot.NIGHT, 
                                        "Afternoon booking", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking2);

        Booking booking3 = createBooking(testMunicipality, 
                                        TestDateUtils.getValidDateAfterDays(6), 
                                        TimeSlot.MORNING, 
                                        "Different day", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking3);

        entityManager.flush();

        List<Booking> found = bookingRepository.findByRequestedDateAndMunicipality(targetDate, testMunicipality);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Booking::getDescription)
                .containsExactlyInAnyOrder("Morning booking", "Afternoon booking");
    }

    /**
     * Testa contagem de reservas por município.
     */
    @Test
    void whenCountByMunicipality_thenReturnCorrectCount() {
        Municipality porto = new Municipality();
        porto.setName("Porto");
        entityManager.persistAndFlush(porto);

        Booking booking1 = createBooking(testMunicipality, 
                                        TestDateUtils.getNextValidDate(), 
                                        TimeSlot.MORNING, 
                                        "Lisboa 1", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking1);

        Booking booking2 = createBooking(testMunicipality, 
                                        TestDateUtils.getValidDateAfterDays(2), 
                                        TimeSlot.NIGHT, 
                                        "Lisboa 2", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking2);

        Booking booking3 = createBooking(porto, 
                                        TestDateUtils.getNextValidDate(), 
                                        TimeSlot.MORNING, 
                                        "Porto 1", 
                                        BookingStatus.RECEIVED);
        bookingRepository.save(booking3);

        entityManager.flush();

        int lisboaCount = bookingRepository.countByMunicipality(testMunicipality);
        int portoCount = bookingRepository.countByMunicipality(porto);

        assertThat(lisboaCount).isEqualTo(2);
        assertThat(portoCount).isEqualTo(1);
    }
}