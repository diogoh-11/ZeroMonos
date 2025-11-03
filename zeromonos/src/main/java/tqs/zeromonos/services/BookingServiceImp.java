package tqs.zeromonos.services;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;


import org.springframework.stereotype.Service;

import tqs.zeromonos.data.Booking;
import tqs.zeromonos.data.BookingRepository;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.data.StateChange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


@Service
public class BookingServiceImp implements BookingService {


    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImp.class);
    private BookingRepository bookingRepository;
    private MunicipalityRepository municipalityRepository;
    private int maxBookingsPerMunicipy;



    public BookingServiceImp(BookingRepository bookingRepository, MunicipalityRepository municipalityRepository) {
        this.bookingRepository = bookingRepository;
        this.municipalityRepository = municipalityRepository;
        this.maxBookingsPerMunicipy = 100;
    }

    // Cria uma reserva
    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        
        logger.info("Tentando criar reserva para município");

        Municipality municipality = municipalityRepository.findByName(request.getMunicipalityName())
                .orElseThrow(() -> {
                    logger.error("Município '{}' não encontrado na base de dados", 
                               request.getMunicipalityName());
                    return new IllegalArgumentException("Município '" + 
                           request.getMunicipalityName() + "' não encontrado");
                });

        logger.debug("Município encontrado");
                
        // Valida a data da reserva
        validateBookingDate(request.getRequestedDate());
        

        // Vê se excedeu o limite maximo de reservas por município
        if (bookingRepository.countByMunicipality(municipality) >= maxBookingsPerMunicipy) {
            logger.error("Número máximo de reservas atingido: {} por municipio", maxBookingsPerMunicipy);
            throw new IllegalStateException("Número máximo de reservas atingido");
        } 

        // Reserva valida, prossegue
        logger.debug("Data de reserva validada");
        Booking booking = new Booking(municipality, request.getDescription(), request.getRequestedDate(), request.getTimeSlot());
        bookingRepository.save(booking);

        return BookingResponseDTO.fromEntity(booking);
    }


    // Busca reserva pelo token
    @Override
    public BookingResponseDTO getBookingByToken(String token) {
        Booking booking = bookingRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Reserva não encontrada"));
        return BookingResponseDTO.fromEntity(booking);
    }


    // Cancela reserva pelo token
    @Override
    public void cancelBooking(String token) {
        Booking booking = bookingRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Reserva não encontrada"));


        // Avança com a reserva se estiver em estado RECEIVED ou ASSIGNED
        if (booking.getStatus() == BookingStatus.RECEIVED || booking.getStatus() == BookingStatus.ASSIGNED) {
            // Lógica de histórico  e mudança de estado
            StateChange stateChange = new StateChange(BookingStatus.CANCELLED, java.time.OffsetDateTime.now());
            booking.addStateChange(stateChange);

            bookingRepository.save(booking);  
        }
        else {
            throw new IllegalStateException("A reserva não pode ser cancelada no estado atual");
        }
    }


    // Lista municípios disponíveis (retorna nomes ou códigos)
    @Override
    public List<String> getAvailableMunicipalities() {
        return municipalityRepository.findAll()
                .stream()
                .map(Municipality::getName)
                .collect(Collectors.toList());
    }


    // Staff: lista reservas de um município
    @Override
    public List<BookingResponseDTO> listForStaff(String municipalityName) {
       
        List<Booking> bookings;
        
        // Se município não for especificado, retorna todas as reservas
        if (municipalityName.equalsIgnoreCase("todas")) {
            bookings = bookingRepository.findAll();
        } else {
            Municipality municipality = municipalityRepository.findByName(municipalityName)
                    .orElseThrow(() -> new NoSuchElementException("Município não encontrado"));
            bookings = bookingRepository.findByMunicipality(municipality);
        }

        return bookings.stream()
                .map(BookingResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    // Staff: atualiza status de reserva
    @Override
    public BookingResponseDTO updateBookingStatusForStaff(String token, BookingStatus newStatus) {
        Booking booking = bookingRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Reserva não encontrada"));
        
        // Lógica de mudança de estado e histórico
        StateChange stateChange = new StateChange(newStatus, java.time.OffsetDateTime.now());
        booking.addStateChange(stateChange);
        bookingRepository.save(booking);


        return BookingResponseDTO.fromEntity(booking);
    }



    /// Utils
    public void validateBookingDate(LocalDate requestedDate) {
        ZoneId zone = ZoneId.of("Europe/Lisbon"); 
        LocalDate today = LocalDate.now(zone);
        

        if (requestedDate.isBefore(today)) {
            throw new IllegalArgumentException("A data solicitada não pode ser no passado");
        }
        
        // NÃo permite reservas para o mesmo dia
        if (requestedDate.isEqual(today)) {
            throw new IllegalArgumentException("A data solicitada não pode ser no mesmo dia");
        }
        
        // Não ha reservas ao Domingo
        if (requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Não são feitas recolhas ao fim-de-semana");
        }
    }
    
}
