import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class creates a Lamport clock used to timestamp messages in a chat application.
 *
 */
public class LamportClock {
    private final AtomicInteger clock;

    /**
     * Initializes the LamportClock class by setting the initial timestamp to 0.
     */
    public LamportClock() {
        clock = new AtomicInteger(0);
    }

    /**
     * Increments the clock's timestamp by 1 and returns it.
     * @return Incremented timestamp
     */
    public int tick() {
        return clock.incrementAndGet();
    }

    /**
     * Updates the clock's timestamp to be the maximum of its current value and the given timestamp.
     * @param value updated clock timestamp
     */
    public void update(int value) {
        clock.set(Math.max(clock.get(), value) + 1);
    }
}
