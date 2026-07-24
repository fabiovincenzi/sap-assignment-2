package sap.shipping.gateway.infrastructure;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import sap.shipping.common.exagonal.Adapter;

/**
 * Application Metrics for the API Gateway. Unlike the services, the gateway has
 * no domain core to observe: the metric counts incoming HTTP requests, which is
 * a pure infrastructure concern, so it lives entirely in this layer and is
 * incremented by a routing handler (see GatewayController).
 */
@Adapter
public class GatewayMetrics {

    private final Counter requestsTotal;
    private final HTTPServer promServer;

    public GatewayMetrics(int port) throws ObsMetricServerException {
        JvmMetrics.builder().register();

        requestsTotal = Counter.builder()
            .name("gateway_requests_total")
            .help("Total number of HTTP requests received by the gateway")
            .register();

        try {
            promServer = HTTPServer.builder()
                .port(port)
                .buildAndStart();
        } catch (Exception ex) {
            throw new ObsMetricServerException();
        }
    }

    public void incRequest() {
        requestsTotal.inc();
    }
}
