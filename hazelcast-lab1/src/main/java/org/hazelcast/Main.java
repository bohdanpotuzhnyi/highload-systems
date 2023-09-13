package org.hazelcast;

import com.hazelcast.cp.IAtomicLong;

public class Main {
    public static void main(String[] args) {
        HazelcastCounter counter = new HazelcastCounter();

        System.out.println("Incrementing without locks:");
        counter.runThreadsAndMeasure(counter::incrementWithoutLocks);
        System.out.println("Counter value: " + counter.counterMap.get("counter"));

        System.out.println("Incrementing with pessimistic locking:");
        counter.runThreadsAndMeasure(counter::incrementWithPessimisticLocking);
        System.out.println("Counter value: " + counter.counterMap.get("counter"));

        System.out.println("Incrementing with optimistic locking:");
        counter.runThreadsAndMeasure(counter::incrementWithOptimisticLocking);
        System.out.println("Counter value: " + counter.counterMap.get("counter"));

        System.out.println("Incrementing with IAtomicLong:");
        counter.runThreadsAndMeasure(counter::incrementWithIAtomicLong);
        IAtomicLong atomicLong = counter.hazelcastInstance.getCPSubsystem().getAtomicLong("atomicCounter");
        System.out.println("Counter value: " + atomicLong.get());

        System.exit(0);
    }
}