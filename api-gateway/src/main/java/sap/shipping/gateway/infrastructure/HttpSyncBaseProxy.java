package sap.shipping.gateway.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import io.vertx.core.json.JsonObject;

/**
 * Shared HTTP machinery for the proxies: a synchronous GET and POST built on
 * java.net.http.HttpClient. Being synchronous, these calls must never run on
 * the Vert.x event loop (the controller wraps them in executeBlocking).
 */
public class HttpSyncBaseProxy {

    protected HttpResponse<String> doGet(String uri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> doPost(String uri, JsonObject body) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
