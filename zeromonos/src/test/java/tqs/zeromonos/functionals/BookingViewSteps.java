package tqs.zeromonos.functionals;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingViewSteps {

    private static final Logger logger = LoggerFactory.getLogger(BookingViewSteps.class);
    

    // Token compartilhado entre TODOS os cenários (persiste entre execuções)
    private static String sharedToken;

    private final TestContext context;
    private WebDriverWait wait;

    public BookingViewSteps(TestContext context) {
        this.context = context;
        this.wait = new WebDriverWait(context.getDriver(), Duration.ofSeconds(10));
    }


    /**
     * Guarda o token de forma estática (compartilhado entre cenários).
     */
    public static void setSharedToken(String token) {
        logger.info("Setting shared static token: {}", token);
        sharedToken = token;
    }

    /**
     * Recupera o token estático.
     */
    public static String getSharedToken() {
        return sharedToken;
    }

   

    @When("I click the {string}")
    public void clickButton(String label) {
        WebElement link = wait.until(ExpectedConditions.elementToBeClickable(By.linkText(label)));
        link.click();
    }

    @Then("the booking search page should be displayed")
    public void bookingSearchPageDisplayed() {
        wait.until(ExpectedConditions.urlContains("booking-view.html"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("search-form")));
    }

    @Given("I am on the booking search page")
    public void onBookingSearchPage() {
        context.getDriver().get("http://localhost:8080/booking-view.html");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("search-form")));
    }

    // ---------- Token input (using docstring) ----------
    @When("I enter the token:")
    public void enterToken() {
        WebElement tokenInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("token")));
        tokenInput.clear();
        tokenInput.sendKeys(sharedToken);
    }

    @When("I click the search button")
    public void clickSearchButton() {
        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("search-btn")));
        searchBtn.click();
    }

    // ---------- Results validation ----------
    @Then("I should see the booking details containing {string}")
    public void shouldSeeBookingDetails(String expectedText) {
        WebElement details = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("booking-details")));
        WebElement content = details.findElement(By.cssSelector(".details-content"));
        String detailsText = content.getText();
        logger.info("Booking details found: {}", detailsText);
        assertTrue(detailsText.toLowerCase().contains(expectedText.toLowerCase()),
            "Expected details to contain '" + expectedText + "' but got: " + detailsText);
    }
}
