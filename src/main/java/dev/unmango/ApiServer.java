package dev.unmango;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);
    static final int PORT = 8080;

    private final SealService sealService;
    private HttpServer server;

    public ApiServer(SealService sealService) {
        this.sealService = sealService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/v1/cert.pem", this::handleCert);
        server.createContext("/v1/encrypt", this::handleEncrypt);
        server.start();
        log.info("API server started on port {}", PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleCert(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }
        try {
            String pem = sealService.getPublicKeyPem();
            sendResponse(exchange, 200, "text/plain", pem);
        } catch (Exception e) {
            log.error("Failed to retrieve public key", e);
            sendResponse(exchange, 500, "text/plain", "Internal Server Error");
        }
    }

    private void handleEncrypt(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }
        try (InputStream body = exchange.getRequestBody()) {
            Secret inputSecret = Serialization.unmarshal(body, Secret.class);
            Secret sealed = sealService.sealSecret(inputSecret);
            String json = Serialization.asJson(sealed);
            sendResponse(exchange, 200, "application/json", json);
        } catch (Exception e) {
            log.error("Failed to encrypt secret", e);
            sendResponse(exchange, 500, "text/plain", "Internal Server Error: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
