package com.grafana.demo.service;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "test", name = "cpu", havingValue = "run")
public class CpuLoadSimulator {
    private final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(CpuLoadSimulator.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, (int)(Runtime.getRuntime().availableProcessors() * 0.7)) // Use ~70% of available cores
    );

    @Scheduled(fixedRate = 6000) // Every 6 seconds
    public void generateConstantCpuLoad() {
        // High probability of generating load for consistency
        if (random.nextFloat() < 0.9) { // 90% chance
            int loadDuration = 5000 + random.nextInt(2000); // 5-7 seconds

            // Use more cores, but not all
            int numCores = Math.max(2, (int)(Runtime.getRuntime().availableProcessors() * 0.6));

            logger.info("Generating steady CPU load: cores={}, duration={}ms",
                    numCores, loadDuration);

            // Start a load on multiple cores
            for (int i = 0; i < numCores; i++) {
                executorService.submit(() -> {
                    generateModerateSteadyLoad(loadDuration);
                });
            }
        }
    }

    /**
     * Generates a moderate but steady CPU load with brief pauses
     */
    private void generateModerateSteadyLoad(int duration) {
        long endTime = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < endTime) {
            // Medium-sized workloads
            sortLargeArrays(30000); // Medium-sized arrays

            // Brief pause to prevent maxing out
            try {
                Thread.sleep(10 + random.nextInt(15)); // 10-25ms pauses (shorter)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Mix in some more CPU-intensive operations
            if (random.nextFloat() < 0.3) { // 30% chance
                multiplyMatrices(200); // Smaller matrices, but still substantial
            } else {
                findPrimes(80000); // Moderate prime calculation
            }

            // Another brief pause
            try {
                Thread.sleep(5 + random.nextInt(10)); // 5-15ms pauses (very short)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Original methods below, kept for reference
    private void generateMediumLoad(int duration) {
        long endTime = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < endTime) {
            // Sort large arrays repeatedly
            sortLargeArrays(50000);
            // Calculate some primes
            findPrimes(100000);
        }
    }

    private void generateHeavyLoad(int duration) {
        long endTime = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < endTime) {
            // Larger matrix operations
            multiplyMatrices(500);
            // Heavy string operations
            performStringOperations(20000);
        }
    }

    private void generateVeryHeavyLoad(int duration) {
        long endTime = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < endTime) {
            // Recursive operations with higher values
            calculateFibonacciRecursive(40);
            // Complex mathematical operations
            performComplexMath(1000000);
            // Large matrix operations
            multiplyMatrices(800);
        }
    }

    private void generateExtremeLoad(int duration) {
        long endTime = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < endTime) {
            // Multiple heavy operations in parallel within the thread
            IntStream.range(0, 4).parallel().forEach(i -> {
                switch (i % 4) {
                    case 0: multiplyMatrices(1000); break;
                    case 1: calculateFibonacciRecursive(42); break;
                    case 2: findPrimes(1000000); break;
                    case 3: sortLargeArrays(500000); break;
                }
            });
        }
    }

    // Enhanced methods for CPU-intensive operations
    private void sortLargeArrays(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(Integer.MAX_VALUE);
        }
        Arrays.sort(array);
    }

    private boolean[] findPrimes(int max) {
        boolean[] isPrime = new boolean[max + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = isPrime[1] = false;

        for (int i = 2; i * i <= max; i++) {
            if (isPrime[i]) {
                for (int j = i * i; j <= max; j += i) {
                    isPrime[j] = false;
                }
            }
        }
        return isPrime;
    }

    private void multiplyMatrices(int size) {
        double[][] a = new double[size][size];
        double[][] b = new double[size][size];
        double[][] c = new double[size][size];

        // Initialize with random values
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                a[i][j] = random.nextDouble();
                b[i][j] = random.nextDouble();
            }
        }

        // Multiply
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                c[i][j] = 0;
                for (int k = 0; k < size; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }
    }

    private void performStringOperations(int iterations) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            sb.append(UUID.randomUUID().toString());
            if (i % 100 == 0) {
                sb = new StringBuilder(sb.toString().toUpperCase());
            }
        }
    }

    private void performComplexMath(int iterations) {
        double result = 0;
        for (int i = 0; i < iterations; i++) {
            result += Math.sin(i) * Math.cos(i) / (Math.tan(i) + 0.1);
            result = Math.pow(result, 1.01);
            if (i % 1000 == 0) {
                result = Math.sqrt(Math.abs(result));
            }
        }
    }

    private int calculateFibonacciRecursive(int n) {
        if (n <= 1) return n;
        return calculateFibonacciRecursive(n - 1) + calculateFibonacciRecursive(n - 2);
    }

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
    }
}
