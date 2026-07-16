package drone_service_tests.steps;

import io.cucumber.java.en.*;
import sap.shipping.drone.application.DroneService;
import sap.shipping.drone.domain.Drone;
import sap.shipping.drone.infrastructure.InMemoryDroneRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class DroneSteps {

    private DroneService droneService;
    private Drone currentDrone;
    private Optional<Drone> foundDrone;

    public DroneSteps() {
        var repo = new InMemoryDroneRepository();
        droneService = new DroneService(repo, (droneId, lat, lng) -> {});
    }

    @When("I register a drone at location \\({double}, {double}) with capacity {double} kg")
    public void register_drone(double lat, double lng, double capacity) {
        currentDrone = droneService.registerDrone(lat, lng, capacity);
    }

    @Then("the drone is registered with status {string}")
    public void drone_registered_with_status(String status) {
        assertThat(currentDrone.status().name()).isEqualTo(status);
    }

    @Given("a registered drone at \\({double}, {double}) with capacity {double} kg")
    public void a_registered_drone(double lat, double lng, double capacity) {
        currentDrone = droneService.registerDrone(lat, lng, capacity);
    }

    @When("I request an available drone near \\({double}, {double}) for {double} kg")
    public void request_available_drone(double lat, double lng, double weight) {
        foundDrone = droneService.findAvailableDrone(lat, lng, weight);
    }

    @Then("an available drone is found")
    public void available_drone_found() {
        assertThat(foundDrone).isPresent();
    }
}
