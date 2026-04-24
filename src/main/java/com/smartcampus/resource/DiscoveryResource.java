package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1.2 — Discovery Endpoint
 *
 * GET /api/v1
 * Returns API metadata including version, contact details, and a HATEOAS-style
 * map of primary resource collection URLs.  This acts as the entry point for
 * any client that wants to explore the API without consulting static docs.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {

        // ── Resource links (HATEOAS map) ──────────────────────────────────────
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");

        // ── Contact information ───────────────────────────────────────────────
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name",  "Smart Campus Admin");
        contact.put("email", "admin@smartcampus.ac.uk");
        contact.put("team",  "Facilities Management & IoT Infrastructure");

        // ── Full response body ────────────────────────────────────────────────
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name",        "Smart Campus Sensor & Room Management API");
        body.put("version",     "1.0.0");
        body.put("description", "RESTful API for managing campus rooms and IoT sensor infrastructure.");
        body.put("contact",     contact);
        body.put("resources",   resources);

        return Response.ok(body).build();
    }
}
