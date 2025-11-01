package tqs.zeromonos.isolationtests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

class JsonUtils {
    static byte[] toJson(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Adiciona suporte para tipos de data/hora do Java 8 (LocalDate, LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        return mapper.writeValueAsBytes(object);
    }
}
