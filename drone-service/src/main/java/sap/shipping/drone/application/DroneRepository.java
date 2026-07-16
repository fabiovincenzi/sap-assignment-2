package sap.shipping.drone.application;

import sap.shipping.common.ddd.Repository;
import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.drone.domain.Drone;
import sap.shipping.drone.domain.DroneId;
import java.util.List;
import java.util.Optional;

@OutBoundPort
public interface DroneRepository extends Repository {
    void save(Drone drone);
    Optional<Drone> findById(DroneId id);
    List<Drone> findAllAvailable();
}
