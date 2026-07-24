package sap.shipping.drone.infrastructure;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.drone.application.DroneServiceObserver;

@Adapter
public class PrometheusDroneServiceObserver implements DroneServiceObserver {

    private final Gauge dronesAvailable;
    private final HTTPServer promServer;

    public PrometheusDroneServiceObserver(int port) throws ObsMetricServerException {
        JvmMetrics.builder().register();

        dronesAvailable = Gauge.builder()
            .name("drones_available")
            .help("Number of drones currently available")
            .register();

        try {
            promServer = HTTPServer.builder()
                .port(port)
                .buildAndStart();
        } catch (Exception ex) {
            throw new ObsMetricServerException();
        }
    }

    @Override
    public void notifyDroneAvailable(String droneId) {
        dronesAvailable.inc();
    }

    @Override
    public void notifyDroneAssigned(String droneId) {
        dronesAvailable.dec();
    }
}
