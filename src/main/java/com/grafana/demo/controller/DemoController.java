package com.grafana.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    private final Random random = new Random();

    @GetMapping("/fast")
    public ResponseEntity<Map<String, Object>> getFastResponse() {
        logger.debug("Processing fast GET request");
        return ResponseEntity.ok(createResponse("Fast response"));
    }

    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> getSlowResponse() throws InterruptedException {
        logger.debug("Processing slow GET request");
        // Random delay between 500-2000ms
        TimeUnit.MILLISECONDS.sleep(500 + random.nextInt(1500));
        return ResponseEntity.ok(createResponse("Slow response"));
    }

    @GetMapping("/flaky")
    public ResponseEntity<Map<String, Object>> getFlakyResponse() throws InterruptedException {
        logger.debug("Processing flaky GET request");

        // 20% chance of slow response
        if (random.nextFloat() < 0.2) {
            TimeUnit.MILLISECONDS.sleep(1000 + random.nextInt(2000));
        }

        // Status code distribution: 70% 200, 20% 400s, 10% 500s
        float statusRoll = random.nextFloat();
        if (statusRoll < 0.7) {
            return ResponseEntity.ok(createResponse("Successful response"));
        } else if (statusRoll < 0.9) {
            logger.warn("Flaky Request Error!");
            return ResponseEntity.badRequest().body(createResponse("Bad request error"));
        } else {
            logger.error("Flaky Request Error!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("Server error"));
        }
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> postData(@RequestBody(required = false) Map<String, Object> payload) {
        logger.debug("Processing POST request with payload size: {}",
                payload != null ? payload.size() : 0);

        // 90% success, 10% error
        if (random.nextFloat() < 0.9) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createResponse("Data created successfully"));
        } else {
            logger.error("Post Data Request Error!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createResponse("Invalid data format"));
        }
    }

    @PutMapping("/data/{id}")
    public ResponseEntity<Map<String, Object>> updateData(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> payload) {

        logger.debug("Processing PUT request for id: {}", id);

        // 80% success, 15% not found, 5% server error
        float statusRoll = random.nextFloat();
        if (statusRoll < 0.8) {
            return ResponseEntity.ok(createResponse("Data updated successfully"));
        } else if (statusRoll < 0.95) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createResponse("Resource not found"));
        } else {
            logger.error("Update Data Request Error!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("Server error during update"));
        }
    }

    @DeleteMapping("/data/{id}")
    public ResponseEntity<Map<String, Object>> deleteData(@PathVariable String id) {
        logger.debug("Processing DELETE request for id: {}", id);

        // 85% success, 15% not found
        if (random.nextFloat() < 0.85) {
            return ResponseEntity.ok(createResponse("Data deleted successfully"));
        } else {
            logger.warn("Delete Request Error!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createResponse("Resource not found"));
        }
    }

    private Map<String, Object> createResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", UUID.randomUUID().toString());
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
