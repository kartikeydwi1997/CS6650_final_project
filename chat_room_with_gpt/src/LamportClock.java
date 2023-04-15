import java.util.concurrent.atomic.AtomicInteger;

public class LamportClock {
    private final AtomicInteger clock;

    public LamportClock() {
        clock = new AtomicInteger(0);
    }

    public int tick() {
        return clock.incrementAndGet();
    }

    public void update(int value) {
        clock.set(Math.max(clock.get(), value) + 1);
    }
}
