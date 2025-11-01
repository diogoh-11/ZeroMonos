package tqs.zeromonos.functionals;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Contexto compartilhado entre todos os steps do Cucumber.
 * Gerencia o ciclo de vida do WebDriver (uma instância por cenário).
 */
public class TestContext {
    
    private static final Logger logger = LoggerFactory.getLogger(TestContext.class);
    
    private WebDriver driver;
    private Scenario scenario;
    private String bookingToken;

    /**
     * Executado ANTES de cada cenário.
     * Cria uma nova instância do WebDriver.
     */
    @Before(order = 0) // Executa primeiro
    public void setUp(Scenario scenario) {
        this.scenario = scenario;
        logger.info("=== Starting scenario: {} ===", scenario.getName());
        
        // Configura o driver do Chrome
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        
        // Descomente para rodar headless (sem interface gráfica)
        // options.addArguments("--headless=new");
        
        this.driver = new ChromeDriver(options);
        
        logger.info("WebDriver created: {}", driver.getClass().getSimpleName());
    }

    /**
     * Executado DEPOIS de cada cenário.
     * Fecha o WebDriver e captura screenshot se falhou.
     */
    @After(order = 1000) // Executa por último
    public void tearDown() {
        // Captura screenshot se o teste falhou
        if (scenario.isFailed() && driver != null) {
            try {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", "screenshot-" + scenario.getName());
                logger.info("Screenshot captured for failed scenario");
            } catch (Exception e) {
                logger.error("Error capturing screenshot: {}", e.getMessage());
            }
        }
        
        // Fecha o WebDriver
        if (driver != null) {
            try {
                driver.quit();
                logger.info("WebDriver closed");
            } catch (Exception e) {
                logger.error("Error closing WebDriver: {}", e.getMessage());
            }
        }
        
        logger.info("=== Finished scenario: {} - Status: {} ===\n", scenario.getName(), scenario.getStatus());
    }

    /**
     * Retorna a instância compartilhada do WebDriver.
     */
    public WebDriver getDriver() {
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialized. Make sure @Before hook ran.");
        }
        return driver;
    }

    /**
     * Retorna o cenário atual.
     */
    public Scenario getScenario() {
        return scenario;
    }

    public String getBookingToken() {
        return bookingToken;
    }

    public void setBookingToken(String bookingToken) {
        this.bookingToken = bookingToken;
    }
}
