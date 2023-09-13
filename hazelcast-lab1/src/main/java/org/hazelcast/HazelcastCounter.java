package org.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.map.IMap;
import com.hazelcast.map.EntryProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class HazelcastCounter {
    HazelcastInstance hazelcastInstance;
    IMap<String, Integer> counterMap;

    public HazelcastCounter() {
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "slf4j");
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        counterMap = hazelcastInstance.getMap("counterMap");
    }

    public void resetCounter() {
        counterMap.put("counter", 0);
        IAtomicLong atomicLong = hazelcastInstance.getCPSubsystem().getAtomicLong("atomicCounter");
        atomicLong.set(0);
    }

    public void incrementWithoutLocks() {
        int currentValue = counterMap.getOrDefault("counter", 0);
        counterMap.put("counter", currentValue + 1);
    }

    public void incrementWithPessimisticLocking() {
        counterMap.lock("counter");
        try {
            int currentValue = counterMap.getOrDefault("counter", 0);
            counterMap.put("counter", currentValue + 1);
        } finally {
            counterMap.unlock("counter");
        }
    }

    public void incrementWithOptimisticLocking() {
        counterMap.executeOnKey("counter", new IncrementProcessor());
    }

    public static class IncrementProcessor implements EntryProcessor<String, Integer, Void> {
        @Override
        public Void process(Map.Entry<String, Integer> entry) {
            Integer value = entry.getValue();
            if (value == null) {
                value = 0;
            }
            entry.setValue(value + 1);
            return null;
        }

        @Override
        public EntryProcessor<String, Integer, Void> getBackupProcessor() {
            return null;
        }
    }

    public void incrementWithIAtomicLong() {
        IAtomicLong atomicLong = hazelcastInstance.getCPSubsystem().getAtomicLong("atomicCounter");
        atomicLong.incrementAndGet();
    }

    public void runThreadsAndMeasure(Runnable incrementMethod) {
        resetCounter();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        long startTime = System.currentTimeMillis();

        for (int t = 0; t < 10; t++) {
            executorService.submit(() -> {
                for (int i = 0; i < 10000; i++) {
                    incrementMethod.run();
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
    }
}
