package sap.shipping.delivery.infrastructure;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliveryEventStore;
import sap.shipping.delivery.application.EventStoreConcurrencyException;
import sap.shipping.delivery.domain.DeliveryId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Adapter
public class InMemoryDeliveryEventStore implements DeliveryEventStore {

    static Logger logger = Logger.getLogger("[DeliveryEventStore]");

    private final Map<DeliveryId, List<StoredEvent>> streams = new ConcurrentHashMap<>();

    /** An event as stored: its position in the stream and when it was recorded. */
    private record StoredEvent(long version, Instant recordedAt, DomainEvent event) {}

    @Override
    public void append(DeliveryId id, long expectedVersion, List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        streams.compute(id, (key, stream) -> {
            var current = stream == null ? List.<StoredEvent>of() : stream;
            requireVersion(id, current.size(), expectedVersion);
            return withEventsAppended(current, events);
        });
        logger.log(Level.INFO, "append " + events.size() + " event(s) to delivery " + id.value()
            + " - version " + (expectedVersion + events.size()));
    }

    /** Rejects the append if the stream moved on since the caller read the aggregate. */
    private static void requireVersion(DeliveryId id, long actual, long expected) {
        if (actual != expected) {
            throw new EventStoreConcurrencyException(id, expected, actual);
        }
    }

    /** A copy of the stream with the events appended, numbered from its current end. */
    private static List<StoredEvent> withEventsAppended(List<StoredEvent> current,
                                                        List<DomainEvent> events) {
        var updated = new ArrayList<>(current);
        var recordedAt = Instant.now();
        long version = current.size();
        for (var event : events) {
            updated.add(new StoredEvent(++version, recordedAt, event));
        }
        return List.copyOf(updated);
    }

    @Override
    public List<DomainEvent> load(DeliveryId id) {
        return streams.getOrDefault(id, List.of()).stream()
            .map(StoredEvent::event)
            .toList();
    }

    @Override
    public List<DomainEvent> loadFrom(DeliveryId id, long fromVersion) {
        return streams.getOrDefault(id, List.of()).stream()
            .filter(stored -> stored.version() > fromVersion)
            .map(StoredEvent::event)
            .toList();
    }

    @Override
    public List<DeliveryId> streamIds() {
        return List.copyOf(streams.keySet());
    }

    @Override
    public long currentVersion(DeliveryId id) {
        return streams.getOrDefault(id, List.of()).size();
    }
}
