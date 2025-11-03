package tqs.zeromonos.functionals;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

public class BookingSteps {

    private static final Logger logger = LoggerFactory.getLogger(BookingSteps.class);
    
    private final TestContext context;
    private WebDriverWait wait;

    public BookingSteps(TestContext context) {
        this.context = context;
        this.wait = new WebDriverWait(context.getDriver(), Duration.ofSeconds(10));
    }

    @Then("the booking form page should be displayed")
    public void bookingFormDisplayed() {
        wait.until(ExpectedConditions.urlContains("booking-form.html"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("municipality")));
    }

    @Given("I am on the booking form page")
    public void onBookingFormPage() {
        context.getDriver().get("http://localhost:8080/booking-form.html");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("booking-form")));
    }

    @When("I search and select municipality {string}")
    public void selectMunicipality(String municipality) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.id("municipality")));
        input.clear();
        input.sendKeys(municipality);

        // Aguarda que o dropdown de sugestões apareça
        try {
            WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("suggestions-dropdown")));
            dropdown.click();
        } catch (TimeoutException e) {
            logger.warn("Nenhuma sugestão apareceu para o município '{}', continuando mesmo assim", municipality);
        }
    }

    @When("I set the reservation date to {string}")
    public void setDate(String dateStr) {
        WebElement dateInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("requestedDate")));
        dateInput.clear();
        dateInput.sendKeys(dateStr);
        dateInput.sendKeys(Keys.TAB);
    }

    @When("I select the time slot {string}")
    public void selectTimeSlot(String slot) {
        WebElement selectElement = wait.until(ExpectedConditions.elementToBeClickable(By.id("timeSlot")));
        new Select(selectElement).selectByValue(slot);
    }

    @When("I enter the description:")
    public void enterDescription(String description) {
        WebElement desc = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("description")));
        desc.clear();
        desc.sendKeys(description);
    }

    @When("I submit the booking form")
    public void submitForm() {
        // O botão de submit é do tipo <button type="submit" class="btn btn-primary">
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']")));
        submitButton.click();
    }

    @Then("I should see a confirmation message containing {string}")
    public void shouldSeeConfirmation(String expectedText) {
        // Aguarda que a mensagem de sucesso apareça dentro do #form-msg
        WebElement messageDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#form-msg .message.success")));
        String message = messageDiv.getText();
        
        logger.info("Confirmation message found: {}", message);

        // Extrai o token se presente
        if (message.contains("Token:")) {
            // O token aparece após "Token:" em um <strong> tag
            try {
                WebElement tokenElement = messageDiv.findElement(By.tagName("strong"));
                String token = tokenElement.getText().trim();
                context.setBookingToken(token);
                BookingViewSteps.setSharedToken(token); // Salva no token estático também
                logger.info("Saved booking token: {}", token);
            } catch (NoSuchElementException e) {
                logger.warn("Could not extract token from strong element", e);
            }
        }

        assertTrue(message.toLowerCase().contains(expectedText.toLowerCase()), 
                   "Expected message to contain '" + expectedText + "' but got: " + message);
    }
}
