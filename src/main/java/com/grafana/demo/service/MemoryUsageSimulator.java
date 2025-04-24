package com.grafana.demo.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "test", name = "memory", havingValue = "run")
public class MemoryUsageSimulator {
    private static final Logger logger = LoggerFactory.getLogger(MemoryUsageSimulator.class);
    private final Random random = new Random();

    // Collection to hold references for memory retention
    private final List<Object> memoryHolder = Collections.synchronizedList(new ArrayList<>());
    // Secondary holders for different patterns
    private final Map<String, Object> longLivedCache = new ConcurrentHashMap<>();
    private final List<Object> volatileMemory = Collections.synchronizedList(new ArrayList<>());

    // Configuration constants
    private static final int MAX_MEMORY_RETENTION_MB = 400; // Up to 400MB held at once
    private static final int LARGE_ALLOCATION_STEP_MB = 50; // 50MB steps for large allocations

    @Scheduled(fixedRate = 12000) // Every 12 seconds
    public void simulateMemoryPatterns() {
        int pattern = random.nextInt(6);

        switch (pattern) {
            case 0:
                simulateMassiveSpike();
                break;
            case 1:
                simulateAggressiveGrowth();
                break;
            case 2:
                simulateComplexObjectGraph();
                break;
            case 3:
                simulateMemoryFragmentation();
                break;
            case 4:
                simulateDataProcessingHeap();
                break;
            case 5:
                cleanupMostMemory(); // Occasionally clean up
                break;
        }

        // Log current memory state after each operation
        logMemoryState();
    }

    /**
     * Creates a massive, short-lived spike in memory usage
     */
    private void simulateMassiveSpike() {
        int spikeSizeMB = 100 + random.nextInt(200); // 100-300 MB spike
        logger.info("🚀 Generating MASSIVE memory spike of {} MB", spikeSizeMB);

        try {
            // Create large allocation quickly
            List<byte[]> temporaryList = new ArrayList<>();

            // Allocate in bigger chunks for faster allocation
            for (int i = 0; i < spikeSizeMB; i += 10) {
                int chunkSize = Math.min(10, spikeSizeMB - i);
                temporaryList.add(new byte[chunkSize * 1024 * 1024]); // 10MB chunks

                if (random.nextInt(3) == 0) { // 1/3 chance to fill with data
                    byte[] lastArray = temporaryList.get(temporaryList.size() - 1);
                    random.nextBytes(lastArray); // Fill with random data
                }

                // Very brief pause
                Thread.sleep(20);
            }

            logger.info("Memory spike peak reached at {} MB, holding briefly...", spikeSizeMB);
            Thread.sleep(2000 + random.nextInt(3000)); // Hold for 2-5 seconds

            logger.info("Releasing spike memory");
            temporaryList.clear();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Memory spike simulation interrupted", e);
        }
    }

    /**
     * Simulates an aggressive, sustained memory growth pattern
     */
    private void simulateAggressiveGrowth() {
        int targetGrowthMB = 50 + random.nextInt(150); // 50-200 MB growth
        logger.info("📈 Simulating aggressive memory growth of {} MB", targetGrowthMB);

        // Current total memory in holder (approximation)
        int currentMemoryMB = estimateCurrentMemoryUsageMB();

        // Check if we have room to grow
        if (currentMemoryMB < MAX_MEMORY_RETENTION_MB) {
            int growByMB = Math.min(targetGrowthMB, MAX_MEMORY_RETENTION_MB - currentMemoryMB);
            logger.info("Growing memory by {} MB (current: ~{} MB)", growByMB, currentMemoryMB);

            try {
                // Grow in large chunks
                for (int i = 0; i < growByMB; i += LARGE_ALLOCATION_STEP_MB) {
                    int chunkSize = Math.min(LARGE_ALLOCATION_STEP_MB, growByMB - i);

                    // Create varied object types
                    if (random.nextBoolean()) {
                        // Arrays
                        byte[] largeArray = new byte[chunkSize * 1024 * 1024];
                        random.nextBytes(largeArray); // Fill with data
                        memoryHolder.add(largeArray);
                    } else {
                        // String buffers (uses even more memory due to character encoding)
                        StringBuilder sb = new StringBuilder(chunkSize * 256 * 1024); // ~2x memory usage
                        for (int j = 0; j < chunkSize * 256 * 1024; j++) {
                            sb.append((char)(random.nextInt(26) + 'a'));
                        }
                        memoryHolder.add(sb.toString());
                    }

                    Thread.sleep(100); // Brief pause between allocations
                }

                logger.info("Finished memory growth, now retaining approximately {} MB",
                        estimateCurrentMemoryUsageMB());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Memory growth simulation interrupted", e);
            }
        } else {
            logger.info("Memory retention limit reached ({}MB), skipping growth",
                    MAX_MEMORY_RETENTION_MB);
        }
    }

    /**
     * Simulates a complex object graph with many interconnected references
     */
    private void simulateComplexObjectGraph() {
        int graphSizeMB = 40 + random.nextInt(60); // 40-100 MB object graph
        logger.info("🌐 Creating complex object graph of ~{} MB", graphSizeMB);

        try {
            // Number of root objects
            int rootObjects = 10 + random.nextInt(20);

            // Create object graph with approximately graphSizeMB memory footprint
            int bytesPerRootObject = (graphSizeMB * 1024 * 1024) / rootObjects;

            for (int i = 0; i < rootObjects; i++) {
                // Create a tree structure
                Map<String, Object> rootMap = createObjectGraphNode(bytesPerRootObject, 0, 5);
                longLivedCache.put("graph-root-" + System.currentTimeMillis() + "-" + i, rootMap);

                Thread.sleep(50); // Brief pause
            }

            logger.info("Complex object graph created, estimated size: {} MB", graphSizeMB);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Object graph creation interrupted", e);
        }
    }

    /**
     * Helper method to create a node in the object graph recursively
     */
    private Map<String, Object> createObjectGraphNode(int totalBytes, int depth, int maxDepth) {
        Map<String, Object> node = new HashMap<>();

        // Base case for recursion
        if (depth >= maxDepth || totalBytes < 10240) { // 10KB minimum
            byte[] data = new byte[totalBytes];
            random.nextBytes(data);
            node.put("data", data);
            return node;
        }

        // Add some direct data to this node
        int thisNodeBytes = totalBytes / 10;
        byte[] nodeData = new byte[thisNodeBytes];
        random.nextBytes(nodeData);
        node.put("nodeData", nodeData);

        // Distribute remaining bytes among children
        int remainingBytes = totalBytes - thisNodeBytes;
        int numChildren = 3 + random.nextInt(7); // 3-10 children
        int bytesPerChild = remainingBytes / numChildren;

        for (int i = 0; i < numChildren; i++) {
            // Recursively create child nodes
            if (random.nextFloat() < 0.7) { // 70% chance for recursive node
                node.put("child-" + i, createObjectGraphNode(bytesPerChild, depth + 1, maxDepth));
            } else {
                // Sometimes just create a leaf with data
                byte[] childData = new byte[bytesPerChild];
                random.nextBytes(childData);
                node.put("child-" + i, childData);
            }
        }

        return node;
    }

    /**
     * Simulates memory fragmentation with many small allocations
     */
    private void simulateMemoryFragmentation() {
        int totalFragmentationMB = 30 + random.nextInt(70); // 30-100 MB
        logger.info("🧩 Simulating memory fragmentation across {} MB", totalFragmentationMB);

        try {
            // Calculate how many fragments
            int avgFragmentBytes = 64 * 1024; // 64KB average size
            int numFragments = (totalFragmentationMB * 1024 * 1024) / avgFragmentBytes;

            logger.info("Creating {} memory fragments averaging 64KB each", numFragments);

            List<byte[]> fragments = new ArrayList<>(numFragments);
            for (int i = 0; i < numFragments; i++) {
                // Vary fragment size to create true fragmentation
                int fragmentSize = 32 * 1024 + random.nextInt(64 * 1024); // 32-96KB
                byte[] fragment = new byte[fragmentSize];

                // Fill some fragments with data
                if (random.nextInt(5) == 0) {
                    random.nextBytes(fragment);
                }

                fragments.add(fragment);

                // Occasionally sleep to spread allocation over time
                if (i % 1000 == 0) {
                    Thread.sleep(50);
                }
            }

            // Keep references in volatile memory
            volatileMemory.addAll(fragments);
            logger.info("Memory fragmentation complete: {} fragments created", fragments.size());

            // Hold for a short period then discard half
            Thread.sleep(5000 + random.nextInt(5000)); // 5-10 seconds

            int toDiscard = fragments.size() / 2;
            logger.info("Discarding {} fragments to create fragmentation", toDiscard);

            // Discard every other fragment (worse fragmentation than clearing a contiguous block)
            for (int i = 0; i < toDiscard; i++) {
                if (i < volatileMemory.size()) {
                    volatileMemory.remove(i);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Memory fragmentation simulation interrupted", e);
        }
    }

    /**
     * Simulates memory patterns typical of data processing applications
     */
    private void simulateDataProcessingHeap() {
        int batchSizeMB = 80 + random.nextInt(120); // 80-200 MB
        logger.info("🔄 Simulating data processing memory heap of {} MB", batchSizeMB);

        try {
            // Create in-memory database style structures

            // 1. Row-based data tables
            int numTables = 3 + random.nextInt(5); // 3-8 tables
            int mbPerTable = batchSizeMB / numTables;

            logger.info("Creating {} data tables with ~{}MB each", numTables, mbPerTable);

            List<List<Map<String, Object>>> tables = new ArrayList<>();

            for (int t = 0; t < numTables; t++) {
                // Create a table with rows and columns
                int rowSize = 1024 + random.nextInt(2048); // 1-3KB per row
                int numRows = (mbPerTable * 1024 * 1024) / rowSize;

                List<Map<String, Object>> table = new ArrayList<>(numRows);

                // Generate row data
                for (int r = 0; r < numRows; r++) {
                    Map<String, Object> row = new HashMap<>();

                    // Add columns with varying data types
                    row.put("id", UUID.randomUUID().toString());
                    row.put("timestamp", System.currentTimeMillis());
                    row.put("value", random.nextDouble() * 1000);

                    // Add a binary payload
                    byte[] payload = new byte[512 + random.nextInt(1024)]; // 0.5-1.5KB payload
                    random.nextBytes(payload);
                    row.put("data", payload);

                    // Add string data
                    StringBuilder text = new StringBuilder();
                    int textLength = 100 + random.nextInt(400); // 100-500 chars
                    for (int i = 0; i < textLength; i++) {
                        text.append((char)(random.nextInt(26) + 'a'));
                    }
                    row.put("text", text.toString());

                    // Add the row to the table
                    table.add(row);

                    // Pause occasionally
                    if (r % 10000 == 0) {
                        Thread.sleep(10);
                    }
                }

                tables.add(table);
                logger.info("Created table {} with {} rows", t, numRows);
            }

            // Add to memory holders
            memoryHolder.addAll(tables);

            // Simulate processing - create indexes and aggregations
            logger.info("Creating indexes and aggregations on data");

            // For each table, create some indexes (Maps)
            for (List<Map<String, Object>> table : tables) {
                // Create ID index
                Map<String, Map<String, Object>> idIndex = new HashMap<>();
                // Create timestamp index
                Map<Long, List<Map<String, Object>>> timeIndex = new HashMap<>();

                for (Map<String, Object> row : table) {
                    // Add to ID index
                    idIndex.put((String)row.get("id"), row);

                    // Add to time index
                    Long time = (Long)row.get("timestamp");
                    timeIndex.computeIfAbsent(time, k -> new ArrayList<>()).add(row);
                }

                // Add indexes to memory
                memoryHolder.add(idIndex);
                memoryHolder.add(timeIndex);
            }

            logger.info("Data processing heap created and indexed");

            // Hold for a while
            Thread.sleep(10000 + random.nextInt(20000)); // 10-30 seconds

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Data processing simulation interrupted", e);
        }
    }

    /**
     * Cleans up most memory to prevent actual OutOfMemoryErrors
     */
    private void cleanupMostMemory() {
        int retainedMB = estimateCurrentMemoryUsageMB();
        logger.info("🧹 Performing extensive memory cleanup. Current usage: ~{} MB", retainedMB);

        // Set a limit for how much to clear
        double clearPercentage = 0.7 + (random.nextDouble() * 0.25); // 70-95% clearing

        // Clear from various holders
        int memoryHolderSize = memoryHolder.size();
        int longLivedCacheSize = longLivedCache.size();
        int volatileMemorySize = volatileMemory.size();

        // Clear from main holder
        int toClear = (int)(memoryHolder.size() * clearPercentage);
        for (int i = 0; i < toClear && !memoryHolder.isEmpty(); i++) {
            memoryHolder.remove(0);
        }

        // Clear from cache
        int cacheToClear = (int)(longLivedCache.size() * clearPercentage);
        Iterator<String> keyIterator = longLivedCache.keySet().iterator();
        int cacheCleared = 0;
        while (keyIterator.hasNext() && cacheCleared < cacheToClear) {
            keyIterator.next();
            keyIterator.remove();
            cacheCleared++;
        }

        // Clear from volatile memory
        volatileMemory.clear();

        logger.info("Memory cleanup complete. Cleared {} from main holder, {} from cache, and {} from volatile",
                memoryHolderSize - memoryHolder.size(),
                longLivedCacheSize - longLivedCache.size(),
                volatileMemorySize);

        // Suggest garbage collection
        System.gc();
    }

    /**
     * Logs current memory usage state
     */
    private void logMemoryState() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = allocatedMemory - freeMemory;

        logger.info("Memory State - Used: {}MB, Free: {}MB, Allocated: {}MB, Max: {}MB",
                usedMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                allocatedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024));

        logger.info("Object counts - Main: {}, Cache: {}, Volatile: {}",
                memoryHolder.size(), longLivedCache.size(), volatileMemory.size());
    }

    /**
     * Estimates current memory usage based on holder sizes
     */
    private int estimateCurrentMemoryUsageMB() {
        // This is just a rough estimate - actual memory usage will differ
        return (memoryHolder.size() + longLivedCache.size() + volatileMemory.size()) / 2;
    }
}
