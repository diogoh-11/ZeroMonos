package tqs.zeromonos.functionals;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StaffBookingSteps {

    private static final Logger logger = LoggerFactory.getLogger(StaffBookingSteps.class);
    
    private final TestContext context;
    private WebDriverWait wait;

    public StaffBookingSteps(TestContext context) {
        this.context = context;
        this.wait = new WebDriverWait(context.getDriver(), Duration.ofSeconds(10));
        logger.info("StaffBookingSteps initialized");
    }

    // ============ Navigation ============
    
    @Then("the staff page should be displayed")
    public void staffPageDisplayed() {
        wait.until(ExpectedConditions.urlContains("staff-bookings"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("list-section")));
        logger.info("Staff page displayed successfully");
    }

    // ============ Filtering ============
    
    @Given("I am on the staff page")
    public void onStaffPage() {
        context.getDriver().get("http://localhost:8080/staff-bookings.html");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("list-section")));
        logger.info("Navigated to staff page");
    }

    @When("I select municipality {string} from the municipality filter")
    public void selectMunicipalityFromFilter(String municipality) {
        WebElement selectEl = wait.until(ExpectedConditions.elementToBeClickable(By.id("municipality-filter")));
        Select sel = new Select(selectEl);
        try {
            sel.selectByVisibleText(municipality);
            logger.info("Selected municipality by visible text: {}", municipality);
        } catch (NoSuchElementException e) {
            try {
                sel.selectByValue(municipality);
                logger.info("Selected municipality by value: {}", municipality);
            } catch (NoSuchElementException ex) {
                logger.error("Municipality '{}' not found in filter", municipality);
                throw new AssertionError("Municipality '" + municipality + "' not found in filter");
            }
        }
    }

    @When("I click the filter button")
    public void clickFilterButton() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("filter-btn")));
        btn.click();
        logger.info("Clicked filter button");

        // Wait for bookings table to be refreshed
        wait.until(driver -> {
            WebElement tbody = context.getDriver().findElement(By.id("bookings-tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
            if (rows.isEmpty()) return false;
            String firstText = rows.get(0).getText().toLowerCase();
            return !firstText.contains("carregando");
        });
        logger.info("Bookings table refreshed");
    }

    @Then("the bookings table should contain a row with token and status {string}")
    public void bookingsTableContainsTokenAndStatus(String status) {
        logger.info("Looking for booking with status: {}", status);
        RowMatch rm = findFirstRowWithStatus(status);
        assertNotNull(rm, "No booking row with status " + status + " found.");
        
        // Guardar o token no BookingViewSteps para usar em cenários futuros
        BookingViewSteps.setSharedToken(rm.token);
        logger.info("Found {} booking with token: {}", status, rm.token);
    }

    // ============ Change status scenario ============
    
    @Given("the bookings table contains a row with token")
    public void bookingsTableContainsRowWithToken() {
        // Recuperar o token do BookingViewSteps
        String token = BookingViewSteps.getSharedToken();
        
        if (token != null && !token.isEmpty()) {
            logger.info("Using shared token: {}", token);
            // Verificar se existe na tabela
            RowMatch rm = findRowByToken(token);
            if (rm == null) {
                // Fallback: procurar primeiro booking com status RECEIVED
                logger.warn("Token {} not found in table, searching for first RECEIVED booking", token);
                RowMatch firstReceived = findFirstRowWithStatus("RECEIVED");
                assertNotNull(firstReceived, "No RECEIVED booking found to operate on.");
                BookingViewSteps.setSharedToken(firstReceived.token);
                logger.info("Using fallback token: {}", firstReceived.token);
            }
        } else {
            // Sem token compartilhado, procurar primeiro RECEIVED
            logger.info("No shared token found, searching for first RECEIVED booking");
            RowMatch firstReceived = findFirstRowWithStatus("RECEIVED");
            assertNotNull(firstReceived, "No RECEIVED booking found to operate on.");
            BookingViewSteps.setSharedToken(firstReceived.token);
            logger.info("Using token from first RECEIVED booking: {}", firstReceived.token);
        }
    }

    @When("I click the {string} action for token")
    public void clickActionForToken(String action) {
        String token = BookingViewSteps.getSharedToken();
        assertNotNull(token, "No token available in shared context");
        logger.info("Clicking action '{}' for token: {}", action, token);
        
        RowMatch rm = findRowByToken(token);
        assertNotNull(rm, "Could not find row for token: " + token);

        WebElement actionsCell = rm.actionsCell;
        
        // Mapear ação para valor do select
        String statusValue = mapActionToStatus(action);
        
        // Encontrar o select e selecionar o status
        try {
            WebElement statusSelect = actionsCell.findElement(By.tagName("select"));
            Select select = new Select(statusSelect);
            select.selectByValue(statusValue);
            logger.info("Selected status: {}", statusValue);
            
            // Aguardar um pouco para garantir que o select foi atualizado
            Thread.sleep(200);
            
            // Encontrar e clicar no botão "Atualizar"
            WebElement updateBtn = actionsCell.findElement(By.xpath(".//button[contains(text(), 'Atualizar')]"));
            wait.until(ExpectedConditions.elementToBeClickable(updateBtn)).click();
            logger.info("Clicked update button");
            
        } catch (NoSuchElementException e) {
            logger.error("Could not find status select or update button for token {}", token);
            throw new AssertionError("Could not find status select or update button in Actions cell for token " + token);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Then("the row for token should show status {string}")
    public void rowForTokenShouldShowStatus(String expectedStatus) {
        String token = BookingViewSteps.getSharedToken();
        assertNotNull(token, "No token in shared context to verify status.");
        logger.info("Verifying token {} has status: {}", token, expectedStatus);
        
        // Aguardar até que o status seja atualizado
        try {
            wait.until(driver -> {
                try {
                    RowMatch rm = findRowByToken(token);
                    return rm != null && expectedStatus.equalsIgnoreCase(rm.status);
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });
        } catch (TimeoutException e) {
            RowMatch rm = findRowByToken(token);
            String actualStatus = rm != null ? rm.status : "NOT FOUND";
            fail("Expected status '" + expectedStatus + "' but was '" + actualStatus + "' for token " + token);
        }
        
        RowMatch rm = findRowByToken(token);
        assertNotNull(rm, "Row disappeared after status change for token " + token);
        assertEquals(expectedStatus.toUpperCase(), rm.status.toUpperCase(), 
            "Status mismatch for token " + token);
        logger.info("Status verified successfully: {}", rm.status);
    }

    @Then("I should see a message containing {string}")
    public void shouldSeeMessageContaining(String expected) {
        WebElement msg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("msg")));
        String text = msg.getText();
        logger.info("Message displayed: {}", text);
        assertTrue(text.toLowerCase().contains(expected.toLowerCase()), 
            "Expected message to contain '" + expected + "' but was: " + text);
    }

    // ============ Helper methods ============

    private static class RowMatch {
        String token;
        String municipality;
        String date;
        String time;
        String status;
        WebElement rowElement;
        WebElement actionsCell;
    }

    /**
     * Mapeia a ação do feature file para o valor do status.
     */
    private String mapActionToStatus(String action) {
        switch (action.toLowerCase()) {
            case "assign":
                return "ASSIGNED";
            case "start":
                return "IN_PROGRESS";
            case "complete":
                return "COMPLETED";
            case "cancel":
                return "CANCELLED";
            default:
                logger.warn("Unknown action '{}', using as-is", action);
                return action.toUpperCase();
        }
    }

    /**
     * Encontra a primeira linha com o status especificado.
     */
    private RowMatch findFirstRowWithStatus(String statusToFind) {
        WebElement tbody = context.getDriver().findElement(By.id("bookings-tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));
        
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() < 6) continue; // Não é uma linha de dados
            
            String status = cells.get(4).getText().trim(); // Status é a 5ª coluna (índice 4)
            if (status.equalsIgnoreCase(statusToFind)) {
                return buildRowMatchFromCells(cells, row);
            }
        }
        return null;
    }

    /**
     * Encontra uma linha pelo token.
     */
    private RowMatch findRowByToken(String token) {
        WebElement tbody = context.getDriver().findElement(By.id("bookings-tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));
        
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() < 6) continue;
            
            String rowToken = cells.get(0).getText().trim();
            if (rowToken.equals(token)) {
                return buildRowMatchFromCells(cells, row);
            }
        }
        return null;
    }

    /**
     * Constrói um objeto RowMatch a partir das células da linha.
     */
    private RowMatch buildRowMatchFromCells(List<WebElement> cells, WebElement row) {
        RowMatch rm = new RowMatch();
        rm.token = cells.get(0).getText().trim();
        rm.municipality = cells.get(1).getText().trim();
        rm.date = cells.get(2).getText().trim();
        rm.time = cells.get(3).getText().trim();
        rm.status = cells.get(4).getText().trim();
        rm.rowElement = row;
        rm.actionsCell = cells.get(5); // Actions é a última coluna
        return rm;
    }
}
