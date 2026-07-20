package sap.shipping.drone.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.drone.application.DroneRepository;
import sap.shipping.drone.domain.Drone;
import sap.shipping.drone.domain.DroneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Adapter
public class InMemoryDroneRepository implements DroneRepository {

    static Logger logger = Logger.getLogger("[DroneRepo]");

    private final Map<DroneId, Drone> store = new ConcurrentHashMap<>();

    @Override
    public void save(Drone drone) {
        store.put(drone.getId(), drone);
        logger.log(Level.INFO, "save drone " + drone.getId().value() + " - status " + drone.status());
    }

    @Override
    public Optional<Drone> findById(DroneId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Drone> findAllAvailable() {
        return store.values().stream()
            .filter(Drone::isAvailable)
            .toList();
    }
}
