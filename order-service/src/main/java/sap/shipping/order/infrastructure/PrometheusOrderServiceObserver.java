package sap.shipping.order.infrastructure;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.order.application.OrderServiceObserver;

@Adapter
public class PrometheusOrderServiceObserver implements OrderServiceObserver {

    private final Counter ordersCreatedTotal;
    private final HTTPServer promServer;

    public PrometheusOrderServiceObserver(int port) throws ObsMetricServerException {
        JvmMetrics.builder().register();

        ordersCreatedTotal = Counter.builder()
            .name("orders_placed_total")
            .help("Total number of orders placed")
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
    public void notifyOrderCreated(String orderId) {
        ordersCreatedTotal.inc();
    }
}
