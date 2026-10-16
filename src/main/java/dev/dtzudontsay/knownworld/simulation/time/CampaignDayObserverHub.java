
package dev.dtzudontsay.knownworld.simulation.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Batch 22.10.
 *
 * Ordered notification hub for completed campaign-day
 * advances.
 *
 * The authoritative SimulationClock and CampaignCalendar
 * remain the only sources of time.
 *
 * This hub never advances either clock.
 *
 * Call publish only AFTER the campaign calendar has
 * actually advanced.
 */
public final class CampaignDayObserverHub {

    private final List<Consumer<SimulationTimeContract>>
            observers = new ArrayList<>();

    /**
     * Register an observer once.
     *
     * Duplicate registration is rejected to prevent
     * accidentally dispatching the same economic or
     * demographic operation twice.
     */
    public synchronized void register(
            Consumer<SimulationTimeContract> observer
    ) {
        Objects.requireNonNull(observer, "observer");

        if (observers.contains(observer)) {
            throw new IllegalStateException(
                    "Campaign-day observer already registered"
            );
        }

        observers.add(observer);
    }

    /**
     * Remove an observer.
     *
     * Useful when a server runtime stops or an
     * explicitly registered system is detached.
     */
    public synchronized void unregister(
            Consumer<SimulationTimeContract> observer
    ) {
        Objects.requireNonNull(observer, "observer");
        observers.remove(observer);
    }

    public synchronized int observerCount() {
        return observers.size();
    }

    /**
     * Capture the authoritative time state once,
     * then distribute the same immutable snapshot.
     */
    public void publish(
            SimulationClock clock,
            CampaignCalendar calendar
    ) {
        publish(
                SimulationTimeContract.capture(
                        Objects.requireNonNull(clock),
                        Objects.requireNonNull(calendar)
                )
        );
    }

    /**
     * Publish an already captured time snapshot.
     *
     * Observers run in registration order.
     *
     * An exception propagates immediately rather
     * than pretending that the whole dispatch
     * succeeded.
     *
     * A copy of the observer list protects iteration
     * if subscriptions change during notification.
     */
    public void publish(
            SimulationTimeContract time
    ) {
        Objects.requireNonNull(time, "time");

        List<Consumer<SimulationTimeContract>> current;

        synchronized (this) {
            current = List.copyOf(observers);
        }

        for (Consumer<SimulationTimeContract> observer :
                current) {
            observer.accept(time);
        }
    }
}
