package tqs.zeromonos.functionals;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

/**
 * Steps comuns compartilhados entre diferentes features.
 */
public class CommonSteps {

    private final TestContext context;
    private WebDriverWait wait;

    public CommonSteps(TestContext context) {
        this.context = context;
        this.wait = new WebDriverWait(context.getDriver(), Duration.ofSeconds(10));
    }

    @Given("I open the application home page")
    public void openHomePage() {
        context.getDriver().get("http://localhost:8080/index.html");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    @When("I click the {string} button")
    public void clickButton(String label) {
        try {
            // Tenta encontrar como link
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.linkText(label)
            ));
            btn.click();
        } catch (TimeoutException e) {
            // Se não encontrar como link, tenta como botão
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), '" + label + "')]")
            ));
            btn.click();
        }
    }
}
