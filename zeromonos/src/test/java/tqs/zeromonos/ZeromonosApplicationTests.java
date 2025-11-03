package tqs.zeromonos;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ZeromonosApplicationTests {

	@Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        // Verifica se o contexto Spring foi carregado corretamente
        assertThat(context).isNotNull();
        
        // Verifica se beans importantes estão presentes
        assertThat(context.containsBean("zeromonosApplication")).isTrue();
    }

    @Test
    void mainMethodRuns() {
        // Testa que o método main executa sem exceções
        assertDoesNotThrow(() -> {
            ZeromonosApplication.main(new String[] {});
        });
    }


}
