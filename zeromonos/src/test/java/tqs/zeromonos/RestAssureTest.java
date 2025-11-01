package tqs.zeromonos;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import tqs.zeromonos.data.*;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.utils.TestDateUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de API usando RestAssured.
 * Testa os endpoints REST da aplicação completa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RestAssureTest {

    @LocalServerPort
    private int port;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    private Municipality testMunicipality;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Limpa dados
        bookingRepository.deleteAll();
        municipalityRepository.deleteAll();

        // Cria município de teste
        testMunicipality = new Municipality();
        testMunicipality.setName("Lisboa");
        municipalityRepository.save(testMunicipality);
    }


    /**
     * Testa criação de uma nova reserva (POST /api/bookings).
     */
    @Test
    void whenPostValidBooking_thenStatus200AndReturnToken() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Recolha de resíduos recicláveis");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("municipalityName", equalTo("Lisboa"))
            .body("timeSlot", equalTo("MORNING"))
            .body("status", equalTo("RECEIVED"))
            .body("description", equalTo("Recolha de resíduos recicláveis"));
    }

    /**
     * Testa criação de reserva com município inexistente.
     */
    @Test
    void whenPostBookingWithInvalidMunicipality_thenStatus400() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("MunicípioInexistente");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(400);
    }

    /**
     * Testa criação de reserva com data no passado.
     */
    @Test
    void whenPostBookingWithPastDate_thenStatus400() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getPastDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(400);
    }

    /**
     * Testa criação de reserva para domingo (fim-de-semana).
     */
    @Test
    void whenPostBookingForSunday_thenStatus400() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getNextSunday());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Test");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(400);
    }

    /**
     * Testa busca de reserva por token válido (GET /api/bookings/{token}).
     */
    @Test
    void whenGetBookingByValidToken_thenStatus200AndReturnBooking() {
        // Cria uma reserva primeiro
        Booking booking = new Booking(testMunicipality, "Test booking", 
                                     TestDateUtils.getNextValidDate(), TimeSlot.NIGHT);
        booking = bookingRepository.save(booking);
        String token = booking.getToken();

        given()
        .when()
            .get("/api/bookings/" + token)
        .then()
            .statusCode(200)
            .body("token", equalTo(token))
            .body("municipalityName", equalTo("Lisboa"))
            .body("timeSlot", equalTo("NIGHT"))
            .body("status", equalTo("RECEIVED"));
    }

    /**
     * Testa busca de reserva com token inválido.
     */
    @Test
    void whenGetBookingByInvalidToken_thenStatus404() {
        given()
        .when()
            .get("/api/bookings/invalid-token-123")
        .then()
            .statusCode(404);
    }

    /**
     * Testa cancelamento de reserva (PUT /api/bookings/{token}/cancel).
     */
    @Test
    void whenCancelBooking_thenStatus204() {
        // Cria uma reserva
        Booking booking = new Booking(testMunicipality, "Test booking", 
                                     TestDateUtils.getNextValidDate(), TimeSlot.MORNING);
        booking = bookingRepository.save(booking);
        String token = booking.getToken();

        given()
        .when()
            .put("/api/bookings/" + token + "/cancel")
        .then()
            .statusCode(204);

        // Verifica que foi cancelada
        given()
        .when()
            .get("/api/bookings/" + token)
        .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"));
    }

    /**
     * Testa cancelamento de reserva inexistente.
     */
    @Test
    void whenCancelInvalidBooking_thenStatus404() {
        given()
        .when()
            .put("/api/bookings/invalid-token/cancel")
        .then()
            .statusCode(404);
    }

    /**
     * Testa listagem de municípios (GET /api/bookings/municipalities).
     */
    @Test
    void whenGetMunicipalities_thenStatus200AndReturnList() {
        // Adiciona mais municípios
        Municipality porto = new Municipality();
        porto.setName("Porto");
        municipalityRepository.save(porto);

        Municipality coimbra = new Municipality();
        coimbra.setName("Coimbra");
        municipalityRepository.save(coimbra);

        given()
        .when()
            .get("/api/bookings/municipalities")
        .then()
            .statusCode(200)
            .body("$", hasSize(3))
            .body("$", hasItems("Lisboa", "Porto", "Coimbra"));
    }

    /**
     * Testa listagem de todas as reservas para staff (GET /api/staff/bookings).
     */
    @Test
    void whenStaffGetAllBookings_thenStatus200AndReturnList() {
        // Cria algumas reservas em dias diferentes
        Booking booking1 = new Booking(testMunicipality, "Booking 1", 
                                      TestDateUtils.getNextValidDate(), TimeSlot.MORNING);
        bookingRepository.save(booking1);

        // Usa o método que já valida Domingos
        Booking booking2 = new Booking(testMunicipality, "Booking 2", 
                                      TestDateUtils.getValidDateAfterDays(2), TimeSlot.NIGHT);
        bookingRepository.save(booking2);

        given()
            .queryParam("municipality", "todas")
        .when()
            .get("/api/staff/bookings")
        .then()
            .statusCode(200)
            .body("$", hasSize(2))
            .body("[0].municipalityName", equalTo("Lisboa"))
            .body("[1].municipalityName", equalTo("Lisboa"));
    }

    /**
     * Testa listagem filtrada por município para staff.
     */
    @Test
    void whenStaffGetBookingsByMunicipality_thenStatus200AndReturnFiltered() {
        Municipality porto = new Municipality();
        porto.setName("Porto");
        municipalityRepository.save(porto);

        // Cria reservas em diferentes municípios
        Booking lisboaBooking = new Booking(testMunicipality, "Lisboa booking", 
                                           TestDateUtils.getNextValidDate(), TimeSlot.MORNING);
        bookingRepository.save(lisboaBooking);

        Booking portoBooking = new Booking(porto, "Porto booking", 
                                          TestDateUtils.getNextValidDate(), TimeSlot.NIGHT);
        bookingRepository.save(portoBooking);

        given()
            .queryParam("municipality", "Lisboa")
        .when()
            .get("/api/staff/bookings")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].municipalityName", equalTo("Lisboa"))
            .body("[0].description", equalTo("Lisboa booking"));
    }

    /**
     * Testa atualização de status para staff (PATCH /api/staff/bookings/{token}/status).
     */
    @Test
    void whenStaffUpdateBookingStatus_thenStatus200() {
        // Cria uma reserva
        Booking booking = new Booking(testMunicipality, "Test booking", 
                                     TestDateUtils.getNextValidDate(), TimeSlot.MORNING);
        booking = bookingRepository.save(booking);
        String token = booking.getToken();

        given()
            .queryParam("status", "ASSIGNED")
        .when()
            .patch("/api/staff/bookings/" + token + "/status")
        .then()
            .statusCode(200)
            .body("status", equalTo("ASSIGNED"))
            .body("token", equalTo(token));
    }

    /**
     * Testa atualização de status com token inválido.
     */
    @Test
    void whenStaffUpdateInvalidBookingStatus_thenStatus404() {
        given()
            .queryParam("status", "ASSIGNED")
        .when()
            .patch("/api/staff/bookings/invalid-token/status")
        .then()
            .statusCode(404);
    }

    /**
     * Testa fluxo completo: criar -> buscar -> atualizar -> cancelar.
     */
    @Test
    void whenCompleteBookingFlow_thenSuccess() {
        // 1. Criar reserva
        BookingRequestDTO request = new BookingRequestDTO();
        request.setMunicipalityName("Lisboa");
        request.setRequestedDate(TestDateUtils.getNextValidDate());
        request.setTimeSlot(TimeSlot.MORNING);
        request.setDescription("Complete flow test");

        String token = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(200)
            .extract()
            .path("token");

        // 2. Buscar reserva
        given()
        .when()
            .get("/api/bookings/" + token)
        .then()
            .statusCode(200)
            .body("status", equalTo("RECEIVED"));

        // 3. Staff atualiza para ASSIGNED
        given()
            .queryParam("status", "ASSIGNED")
        .when()
            .patch("/api/staff/bookings/" + token + "/status")
        .then()
            .statusCode(200)
            .body("status", equalTo("ASSIGNED"));

        // 4. Cliente cancela
        given()
        .when()
            .put("/api/bookings/" + token + "/cancel")
        .then()
            .statusCode(204);

        // 5. Verificar cancelamento
        given()
        .when()
            .get("/api/bookings/" + token)
        .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"));
    }


    /**
     * Testa que não se pode cancelar reserva já completada.
     */
    @Test
    void whenCancelCompletedBooking_thenStatus409() {
        // Cria e completa uma reserva
        Booking booking = new Booking(testMunicipality, "Test booking", 
                                     TestDateUtils.getNextValidDate(), TimeSlot.MORNING);
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);
        String token = booking.getToken();

        given()
        .when()
            .put("/api/bookings/" + token + "/cancel")
        .then()
            .statusCode(409);
    }

    /**
     * Testa múltiplas reservas no mesmo dia e timeslot.
     */
    @Test
    void whenCreateMultipleBookingsSameDayAndSlot_thenAllSucceed() {
        // Usa método que já valida Domingos
        java.time.LocalDate targetDate = TestDateUtils.getValidDateAfterDays(3);

        // Primeira reserva
        BookingRequestDTO request1 = new BookingRequestDTO();
        request1.setMunicipalityName("Lisboa");
        request1.setRequestedDate(targetDate);
        request1.setTimeSlot(TimeSlot.MORNING);
        request1.setDescription("First booking");

        given()
            .contentType(ContentType.JSON)
            .body(request1)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(200);

        // Segunda reserva (mesmo dia e slot)
        BookingRequestDTO request2 = new BookingRequestDTO();
        request2.setMunicipalityName("Lisboa");
        request2.setRequestedDate(targetDate);
        request2.setTimeSlot(TimeSlot.MORNING);
        request2.setDescription("Second booking");

        given()
            .contentType(ContentType.JSON)
            .body(request2)
        .when()
            .post("/api/bookings")
        .then()
            .statusCode(200);
    }
}