package com.grafana.demo.service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(prefix = "test", name = "http", havingValue = "run")
public class HttpTrafficSimulator {
    private static final Logger logger = LoggerFactory.getLogger(HttpTrafficSimulator.class);
    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // Base URL for API calls (using localhost)
    private final String baseUrl = "http://localhost:8080/api/demo";

    // Tracking IDs for PUT/DELETE operations
    private final List<String> resourceIds = Collections.synchronizedList(new ArrayList<>());

    // Traffic pattern: Low, Medium, High, Burst
    private enum TrafficPattern { LOW, MEDIUM, HIGH, BURST }
    private TrafficPattern currentPattern = TrafficPattern.MEDIUM;

    /**
     * Periodically changes traffic patterns to create interesting metrics
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void changeTrafficPattern() {
        TrafficPattern[] patterns = TrafficPattern.values();
        currentPattern = patterns[random.nextInt(patterns.length)];

        logger.info("Switching to {} traffic pattern", currentPattern);
    }

    /**
     * Main scheduler that generates HTTP traffic
     */
    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void generateHttpTraffic() {
        // Determine number of requests based on current pattern
        int requestCount;
        switch (currentPattern) {
            case LOW:
                requestCount = 1 + random.nextInt(3); // 1-3 requests
                break;
            case MEDIUM:
                requestCount = 5 + random.nextInt(10); // 5-15 requests
                break;
            case HIGH:
                requestCount = 15 + random.nextInt(20); // 15-35 requests
                break;
            case BURST:
                requestCount = random.nextFloat() < 0.3 ?
                        50 + random.nextInt(50) : // 30% chance: 50-100 requests (burst)
                        3 + random.nextInt(7);    // 70% chance: 3-10 requests (normal)
                break;
            default:
                requestCount = 5; // Default
        }

        logger.info("Generating {} HTTP requests", requestCount);

        // Generate requests
        for (int i = 0; i < requestCount; i++) {
            executorService.submit(this::makeRandomRequest);
        }
    }

    /**
     * Makes a random HTTP request to one of our demo endpoints
     */
    private void makeRandomRequest() {
        try {
            float methodSelector = random.nextFloat();

            // Method distribution: 60% GET, 20% POST, 10% PUT, 10% DELETE
            if (methodSelector < 0.6) {
                // GET requests
                makeGetRequest();
            } else if (methodSelector < 0.8) {
                // POST requests
                makePostRequest();
            } else if (methodSelector < 0.9) {
                // PUT requests
                makePutRequest();
            } else {
                // DELETE requests
                makeDeleteRequest();
            }

            // Brief pause between requests if needed
            if (random.nextFloat() < 0.3) {
                Thread.sleep(100 + random.nextInt(200));
            }

        } catch (Exception e) {
            logger.error("Error making HTTP request", e);
        }
    }

    /**
     * Makes a GET request to one of the available endpoints
     */
    private void makeGetRequest() {
        String[] endpoints = {"/fast", "/slow", "/flaky"};
        String endpoint = endpoints[random.nextInt(endpoints.length)];

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    baseUrl + endpoint, Map.class);

            logger.debug("GET {} - Status: {}", endpoint, response.getStatusCode());

        } catch (HttpStatusCodeException e) {
            logger.debug("GET {} - Error: {}", endpoint, e.getStatusCode());
        }
    }

    /**
     * Makes a POST request with generated data
     */
    private void makePostRequest() {
        Map<String, Object> payload = generateRandomPayload();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/data", payload, Map.class);

            logger.debug("POST /data - Status: {}", response.getStatusCode());

            // Store ID for future PUT/DELETE if successful
            if (response.getBody() != null && response.getBody().containsKey("id")) {
                String id = (String) response.getBody().get("id");
                if (resourceIds.size() > 100) {
                    resourceIds.removeFirst(); // Keep list from growing too large
                }
                resourceIds.add(id);
            }

        } catch (HttpStatusCodeException e) {
            logger.debug("POST /data - Error: {}", e.getStatusCode());
        }
    }

    /**
     * Makes a PUT request to update an existing resource
     */
    private void makePutRequest() {
        // If we don't have any IDs yet, do a POST instead to create some
        if (resourceIds.isEmpty()) {
            makePostRequest();
            return;
        }

        // Get a random ID from our list
        String id = resourceIds.get(random.nextInt(resourceIds.size()));
        Map<String, Object> payload = generateRandomPayload();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/data/" + id,
                    HttpMethod.PUT,
                    new HttpEntity<>(payload),
                    Map.class);

            logger.debug("PUT /data/{} - Status: {}", id, response.getStatusCode());

        } catch (HttpStatusCodeException e) {
            logger.debug("PUT /data/{} - Error: {}", id, e.getStatusCode());

            // If resource not found, remove from our list
            if (e.getStatusCode().value() == 404) {
                resourceIds.remove(id);
            }
        }
    }

    /**
     * Makes a DELETE request to remove a resource
     */
    private void makeDeleteRequest() {
        // If we don't have any IDs yet, do a POST instead to create some
        if (resourceIds.isEmpty()) {
            makePostRequest();
            return;
        }

        // Get a random ID from our list
        String id = resourceIds.get(random.nextInt(resourceIds.size()));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/data/" + id,
                    HttpMethod.DELETE,
                    null,
                    Map.class);

            logger.debug("DELETE /data/{} - Status: {}", id, response.getStatusCode());

            // Remove from our list if successful
            resourceIds.remove(id);

        } catch (HttpStatusCodeException e) {
            logger.debug("DELETE /data/{} - Error: {}", id, e.getStatusCode());

            // If resource not found, remove from our list
            if (e.getStatusCode().value() == 404) {
                resourceIds.remove(id);
            }
        }
    }

    /**
     * Generates a random payload for POST/PUT requests
     */
    private Map<String, Object> generateRandomPayload() {
        Map<String, Object> payload = new HashMap<>();

        // Random size payload
        int fieldCount = 3 + random.nextInt(10); // 3-13 fields

        payload.put("timestamp", System.currentTimeMillis());
        payload.put("name", "Test-" + UUID.randomUUID().toString().substring(0, 8));
        payload.put("value", random.nextDouble() * 1000);

        // Add additional random fields
        for (int i = 0; i < fieldCount; i++) {
            String key = "field" + i;
            switch (random.nextInt(4)) {
                case 0:
                    payload.put(key, random.nextInt(1000));
                    break;
                case 1:
                    payload.put(key, random.nextDouble() * 1000);
                    break;
                case 2:
                    payload.put(key, UUID.randomUUID().toString());
                    break;
                case 3:
                    payload.put(key, random.nextBoolean());
                    break;
            }
        }

        return payload;
    }
}
