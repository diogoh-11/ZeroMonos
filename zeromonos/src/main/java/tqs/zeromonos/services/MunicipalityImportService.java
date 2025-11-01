package tqs.zeromonos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class MunicipalityImportService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(MunicipalityImportService.class);

    private final MunicipalityRepository municipalityRepository;
    private final WebClient webClient;

    @Value("${municipalities.api.url}")
    private String apiUrl;

    @Value("${municipalities.request.timeout-ms:10000}")
    private long timeoutMs;

    public MunicipalityImportService(MunicipalityRepository municipalityRepository, WebClient.Builder webClientBuilder) {
        this.municipalityRepository = municipalityRepository;
        this.webClient = webClientBuilder.build();
    }

    // Executa ao arrancar a app
    @Override
    public void run(ApplicationArguments args) {
        try {
            fetchAndStoreMunicipalities();
        } catch (Exception e) {
            logger.warn("Falha ao importar municípios no arranque: {}", e.toString());
        }
    }


    public void fetchAndStoreMunicipalities() {
        logger.info("A obter municípios de: {}", apiUrl);
        try {
            // A API retorna apenas array de strings com nomes
            List<String> municipalityNames = webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .block(Duration.ofMillis(timeoutMs));

            if (municipalityNames == null || municipalityNames.isEmpty()) {
                logger.warn("Resposta de municípios vazia.");
                loadFallbackMunicipalities();
                return;
            }

            int created = 0, updated = 0;
            for (String name : municipalityNames) {
                if (name == null || name.trim().isEmpty()) {
                    continue; // Ignorar nomes vazios
                }

                String cleanName = name.trim();
                
                Optional<Municipality> existing = municipalityRepository.findByName(cleanName);
                if (existing.isPresent()) {
                    // Já existe, não precisa de atualizar (só o nome)
                    updated++;
                } else {
                    // Criar novo município
                    municipalityRepository.save(new Municipality(cleanName));
                    created++;
                }
            }
            
            logger.info("Import de municípios terminado: criados={}, já existiam={}, total={}", 
                       created, updated, municipalityNames.size());

        } catch (WebClientResponseException e) {
            logger.error("Erro HTTP ao buscar municípios: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            loadFallbackMunicipalities();
        } catch (Exception e) {
            logger.error("Erro ao buscar municípios: {}", e.toString());
            loadFallbackMunicipalities();
        }
    }

    // FALLBACK para caso a API esteja down
    private void loadFallbackMunicipalities() {
        logger.info("A carregar lista fallback de municípios...");
        
        String[] fallbackNames = {
            "Lisboa", "Porto", "Braga", "Coimbra", "Faro", "Aveiro", 
            "Leiria", "Santarem", "Setubal", "Viana do Castelo",
            "Vila Real", "Braganca", "Guarda", "Castelo Branco",
            "Portalegre", "Evora", "Beja", "Funchal", "Ponta Delgada",
            "Albufeira", "Almada", "Amadora", "Amarante", "Arouca",
            "Barcelos", "Barreiro", "Caldas da Rainha", "Cascais",
            "Espinho", "Esposende", "Estarreja", "Fafe", "Felgueiras",
            "Figueira da Foz", "Gondomar", "Guimarães", "Ilhavo",
            "Lousada", "Maia", "Marco de Canaveses", "Matosinhos",
            "Odivelas", "Oliveira de Azemeis", "Paredes", "Penafiel",
            "Povoa de Varzim", "Santa Maria da Feira", "Santo Tirso",
            "Seixal", "Sintra", "Trofa", "Vale de Cambra", "Valongo",
            "Vila do Conde", "Vila Nova de Famalicao", "Vila Nova de Gaia",
            "Vizela"
        };

        int created = 0;
        for (String name : fallbackNames) {
            if (!municipalityRepository.findByName(name).isPresent()) {
                municipalityRepository.save(new Municipality(name));
                created++;
            }
        }
        logger.info("Fallback carregado: {} municípios", created);
    }
}