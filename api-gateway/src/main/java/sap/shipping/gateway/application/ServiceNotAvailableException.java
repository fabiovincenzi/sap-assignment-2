package sap.shipping.gateway.application;

/**
 * Raised by a proxy when the downstream service cannot be reached or returns
 * an unexpected response. It lets the controller translate any inter-service
 * failure into a single, uniform error response for the client.
 */
public class ServiceNotAvailableException extends RuntimeException {
    public ServiceNotAvailableException(String message) {
        super(message);
    }
}
