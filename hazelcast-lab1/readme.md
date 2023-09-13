
# Hazelcast Counter Implementation and Report

This project demonstrates the usage of different locking mechanisms with Hazelcast to increment a counter in a concurrent environment.

## Implementation:

### Language:
- Java

### Dependencies:
- Hazelcast 5.3.2

### Counter Methods:
1. **Increment Without Locks** - This method directly increments the counter without any locks, which might lead to race conditions.
2. **Increment with Pessimistic Locking** - Uses Hazelcast's built-in locking mechanism to ensure that no two threads can increment the counter simultaneously.
3. **Increment with Optimistic Locking** - Uses Hazelcast's `EntryProcessor` to atomically read, process, and write an entry back to the map, ensuring atomic operations.
4. **Increment with IAtomicLong** - Uses Hazelcast's `IAtomicLong` for atomic increments.

## Execution Results:

- **Without Locks**:
    - Time taken: 3755 ms
    - Counter value: 16761
- **Pessimistic Locking**:
    - Time taken: 12783 ms
    - Counter value: 100000
- **Optimistic Locking**:
    - Time taken: 1619 ms
    - Counter value: 100000
- **IAtomicLong**:
    - Time taken: 866 ms
    - Counter value: 100000

## Log Output:

Hazelcast nodes log output is provided in the `hazelcast-log.txt` file. This log includes details about the Hazelcast instance, cluster details, and other diagnostics.
