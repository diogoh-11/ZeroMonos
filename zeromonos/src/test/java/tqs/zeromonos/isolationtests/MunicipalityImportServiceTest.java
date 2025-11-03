package tqs.zeromonos.isolationtests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.web.reactive.function.client.WebClient;

import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;
import tqs.zeromonos.services.MunicipalityImportService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MunicipalityImportServiceTest {

    @Mock
    private MunicipalityRepository municipalityRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ApplicationArguments applicationArguments;

    private MunicipalityImportService service;

    @BeforeEach
    void setUp() {
        // Mock WebClient to avoid actual HTTP calls
        WebClient webClient = mock(WebClient.class);
        when(webClientBuilder.build()).thenReturn(webClient);

        service = new MunicipalityImportService(municipalityRepository, webClientBuilder);
        // Set private fields using reflection for testing
        setPrivateField(service, "apiUrl", "http://test-api.com/municipalities");
        setPrivateField(service, "timeoutMs", 5000L);
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokePrivateMethod(Object target, String methodName) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRun() {
        // Given - WebClient will throw exception, triggering fallback
        when(municipalityRepository.findByName(anyString())).thenReturn(Optional.empty());

        // When
        service.run(applicationArguments);

        // Then - fallback should be called
        verify(municipalityRepository, atLeast(1)).findByName(anyString());
    }

    @Test
    void testFetchAndStoreMunicipalities() {
        // Given - WebClient will throw exception (simulated by mocking)
        when(municipalityRepository.findByName(anyString())).thenReturn(Optional.empty());

        // When
        service.fetchAndStoreMunicipalities();

        // Then - fallback should be called
        verify(municipalityRepository, atLeast(1)).findByName(anyString());
    }

    @Test
    void testLoadFallbackMunicipalities_NewMunicipalities() {
        // Given - no municipalities exist
        when(municipalityRepository.findByName(anyString())).thenReturn(Optional.empty());

        // When
        invokePrivateMethod(service, "loadFallbackMunicipalities");

        // Then - should save all 57 fallback municipalities
        verify(municipalityRepository, times(57)).save(any(Municipality.class));
    }

    @Test
    void testLoadFallbackMunicipalities_ExistingMunicipalities() {
        // Given - all municipalities already exist
        when(municipalityRepository.findByName(anyString())).thenReturn(Optional.of(new Municipality("Existing")));

        // When
        invokePrivateMethod(service, "loadFallbackMunicipalities");

        // Then - should not save any new municipalities
        verify(municipalityRepository, never()).save(any(Municipality.class));
    }
}