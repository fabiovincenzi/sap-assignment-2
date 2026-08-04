package delivery_service_tests.infrastructure;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.*;

import sap.shipping.delivery.infrastructure.DroneServiceProxy;
import sap.shipping.drone.application.DroneService;
import sap.shipping.drone.infrastructure.DroneController;
import sap.shipping.drone.infrastructure.InMemoryDroneRepository;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: the proxy the delivery service uses to call the drone service is driven
 * against the real drone controller. What is verified is the contract between the two -
 * URLs, query parameters, JSON and status codes - not the business logic of either side.
 */
@DisplayName("Drone Service Proxy test case")
public class DroneServiceProxyTest {

    private static final int DRONE_SERVICE_PORT = 9592;
    private static final int UNUSED_PORT = 9593;

    private Vertx vertx;
    private DroneService droneService;
    private DroneServiceProxy proxy;

    @BeforeEach
    public void setUp() throws Exception {
        var sync = new Synchroniser();
        vertx = Vertx.vertx();

        droneService = new DroneService(new InMemoryDroneRepository(), (droneId, lat, lng) -> {});
        var controller = new DroneController(droneService);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(DRONE_SERVICE_PORT)
            .onSuccess(server -> sync.notifySync());
        sync.awaitSync();

        proxy = new DroneServiceProxy("localhost", DRONE_SERVICE_PORT);
    }

    @AfterEach
    public void tearDown() {
        vertx.close();
    }

    @Test
    @DisplayName("An available drone is found and its id is read from the reply")
    public void anAvailableDroneIsFound() {
        var registered = droneService.registerDrone(5.0, 44.0, 12.0);

        var droneId = proxy.requestAvailableDrone(44.0, 12.0, 2.5);

        assertThat(droneId).contains(registered.getId().value());
    }

    @Test
    @DisplayName("No drone is returned when none can carry the weight")
    public void noDroneIsReturnedWhenTooHeavy() {
        droneService.registerDrone(1.0, 44.0, 12.0);

        var droneId = proxy.requestAvailableDrone(44.0, 12.0, 50.0);

        assertThat(droneId).isEmpty();
    }

    @Test
    @DisplayName("Releasing a drone through the proxy makes it available again")
    public void releasingADroneMakesItAvailableAgain() {
        var registered = droneService.registerDrone(5.0, 44.0, 12.0);
        proxy.requestAvailableDrone(44.0, 12.0, 2.5);
        assertThat(proxy.requestAvailableDrone(44.0, 12.0, 2.5)).isEmpty();

        proxy.releaseDrone(registered.getId().value());

        assertThat(proxy.requestAvailableDrone(44.0, 12.0, 2.5)).contains(registered.getId().value());
    }

    /** The delivery service must survive an unreachable drone service, not crash on it. */
    @Test
    @DisplayName("An unreachable drone service yields no drone instead of an error")
    public void anUnreachableServiceYieldsNoDrone() {
        var proxyToNowhere = new DroneServiceProxy("localhost", UNUSED_PORT);

        var droneId = proxyToNowhere.requestAvailableDrone(44.0, 12.0, 2.5);

        assertThat(droneId).isEmpty();
    }
}
