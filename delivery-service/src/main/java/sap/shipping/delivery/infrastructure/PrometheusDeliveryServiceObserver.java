package sap.shipping.delivery.infrastructure;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliveryServiceObserver;

@Adapter
public class PrometheusDeliveryServiceObserver implements DeliveryServiceObserver {

    private final Gauge deliveriesInProgress;
    private final HTTPServer promServer;

    public PrometheusDeliveryServiceObserver(int port) throws ObsMetricServerException {
        JvmMetrics.builder().register();

        deliveriesInProgress = Gauge.builder()
            .name("deliveries_in_progress")
            .help("Number of deliveries currently in progress")
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
    public void notifyDeliveryScheduled(String deliveryId) {
        deliveriesInProgress.inc();
    }

    @Override
    public void notifyDeliveryCompleted(String deliveryId) {
        deliveriesInProgress.dec();
    }
}
