
package dev.dtzudontsay.knownworld.simulation.time;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public final class CampaignDayObserverHubTest {

    private SimulationTimeContract day(long absoluteDay) {
        CampaignCalendar calendar =
                new CampaignCalendar(0, 0, 365, 1);

        calendar.restore(
                absoluteDay,
                0,
                1
        );

        return SimulationTimeContract.capture(
                new SimulationClock(absoluteDay),
                calendar
        );
    }

    @Test
    void emptyHubAcceptsDayNotifications() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        assertDoesNotThrow(
                () -> hub.publish(day(1L))
        );

        assertEquals(0, hub.observerCount());
    }

    @Test
    void registeredObserverReceivesExactSnapshot() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        AtomicReference<SimulationTimeContract> received =
                new AtomicReference<>();

        hub.register(received::set);

        SimulationTimeContract expected = day(42L);

        hub.publish(expected);

        assertSame(expected, received.get());
        assertEquals(42L,
                received.get().campaignAbsoluteDay());
    }

    @Test
    void observersExecuteInRegistrationOrder() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        List<String> order = new ArrayList<>();

        hub.register(time -> order.add("first"));
        hub.register(time -> order.add("second"));
        hub.register(time -> order.add("third"));

        hub.publish(day(1L));

        assertEquals(
                List.of("first", "second", "third"),
                order
        );
    }

    @Test
    void duplicateObserverRegistrationIsRejected() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        Consumer<SimulationTimeContract> observer =
                time -> {};

        hub.register(observer);

        assertThrows(
                IllegalStateException.class,
                () -> hub.register(observer)
        );

        assertEquals(1, hub.observerCount());
    }

    @Test
    void unregisteredObserverStopsReceivingEvents() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        AtomicInteger calls = new AtomicInteger();

        Consumer<SimulationTimeContract> observer =
                time -> calls.incrementAndGet();

        hub.register(observer);
        hub.publish(day(1L));

        hub.unregister(observer);
        hub.publish(day(2L));

        assertEquals(1, calls.get());
        assertEquals(0, hub.observerCount());
    }

    @Test
    void subscriptionChangesDoNotCorruptCurrentDispatch() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        List<String> calls = new ArrayList<>();

        Consumer<SimulationTimeContract> second =
                time -> calls.add("second");

        hub.register(time -> {
            calls.add("first");
            hub.unregister(second);
        });

        hub.register(second);

        hub.publish(day(1L));

        assertEquals(
                List.of("first", "second"),
                calls
        );

        hub.publish(day(2L));

        assertEquals(
                List.of("first", "second", "first"),
                calls
        );
    }

    @Test
    void observerFailureIsNotSilentlyIgnored() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        AtomicInteger laterCalls = new AtomicInteger();

        hub.register(time -> {
            throw new IllegalStateException(
                    "Simulated domain failure"
            );
        });

        hub.register(time -> laterCalls.incrementAndGet());

        assertThrows(
                IllegalStateException.class,
                () -> hub.publish(day(1L))
        );

        assertEquals(0, laterCalls.get());
    }

    @Test
    void clockAndCalendarOverloadCapturesRealTime() {
        CampaignDayObserverHub hub =
                new CampaignDayObserverHub();

        CampaignCalendar calendar =
                new CampaignCalendar(0, 0, 365, 3);

        SimulationClock clock =
                new SimulationClock();

        AtomicReference<SimulationTimeContract> received =
                new AtomicReference<>();

        hub.register(received::set);

        clock.advanceBy(3L);

        assertFalse(calendar.advanceTick());
        assertFalse(calendar.advanceTick());
        assertTrue(calendar.advanceTick());

        hub.publish(clock, calendar);

        assertNotNull(received.get());
        assertEquals(1L,
                received.get().campaignAbsoluteDay());
        assertEquals(3L,
                received.get().simulationTick());
    }
}
