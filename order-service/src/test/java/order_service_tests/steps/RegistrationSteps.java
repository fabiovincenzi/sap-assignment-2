package order_service_tests.steps;

import io.cucumber.java.en.*;
import sap.shipping.order.application.UserService;
import sap.shipping.order.infrastructure.InMemoryUserRepository;

import static org.assertj.core.api.Assertions.*;

public class RegistrationSteps {

    private UserService userService;
    private String lastInfo = "";
    private String lastError = "";

    public RegistrationSteps() {
        userService = new UserService(new InMemoryUserRepository());
    }

    @Given("I have not registered before")
    public void i_have_not_registered_before() {
    }

    @When("I register with username {string} and password {string}")
    public void i_register_with_username_and_password(String username, String password) {
        try {
            userService.register(username, password);
            lastInfo = "Account created";
        } catch (IllegalArgumentException e) {
            lastError = e.getMessage();
        }
    }

    @Then("I should see a confirmation that my account was created")
    public void i_should_see_confirmation() {
        assertThat(lastInfo).isEqualTo("Account created");
    }

    @Given("Someone already registered with username {string}")
    public void someone_already_registered(String username) {
        try {
            userService.register(username, "any");
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Then("I should see an error {string}")
    public void i_should_see_an_error(String message) {
        assertThat(lastError).isEqualTo(message);
    }
}
