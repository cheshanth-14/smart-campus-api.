package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.io.IOException;
import java.net.URI;

/**
 * JAX-RS Application entry point.
 *
 * @ApplicationPath("/api/v1") establishes the versioned base path.
 * All resource classes and providers under com.smartcampus are
 * auto-discovered via packages().
 *
 * Lifecycle note: By default, JAX-RS creates a NEW resource-class instance
 * per incoming request (request-scoped). This means instance variables on
 * resource classes are NOT shared between requests. The DataStore singleton
 * pattern sidesteps this entirely — every request-scoped resource instance
 * calls DataStore.getInstance() and reads/writes the SAME in-memory maps.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Auto-scan all @Path resources, @Provider mappers, and @Provider filters
        packages("com.smartcampus");
        // Register Jackson for JSON serialisation / deserialisation
        register(JacksonFeature.class);
    }

    // ── Embedded Grizzly server entry point ───────────────────────────────────

    public static void main(String[] args) throws IOException {
        final URI BASE_URI = URI.create("http://0.0.0.0:8080/api/v1/");

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                BASE_URI,
                new SmartCampusApplication(),
                false   // don't start immediately — we start below
        );

        server.start();

        System.out.println("=======================================================");
        System.out.println("  Smart Campus API is running.");
        System.out.println("  Base URL : http://localhost:8080/api/v1");
        System.out.println("  Discovery: http://localhost:8080/api/v1/");
        System.out.println("  Rooms    : http://localhost:8080/api/v1/rooms");
        System.out.println("  Sensors  : http://localhost:8080/api/v1/sensors");
        System.out.println("  Press ENTER to stop the server...");
        System.out.println("=======================================================");

        System.in.read();   // block until user hits Enter
        server.shutdownNow();
        System.out.println("Server stopped.");
    }
}
