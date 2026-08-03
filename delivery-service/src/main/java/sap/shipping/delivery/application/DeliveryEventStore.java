package sap.shipping.delivery.application;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.delivery.domain.DeliveryId;
import java.util.List;

/**
 * Append-only store of the events of each delivery: the source of truth of the service.
 * Events are only appended and replayed, never updated nor deleted.
 */
@OutBoundPort
public interface DeliveryEventStore {

    /** Expected version of a delivery whose stream does not exist yet. */
    long NEW_AGGREGATE = 0;

    /**
     * Appends events to the stream of a delivery.
     *
     * @param expectedVersion version the caller has read the aggregate at; if the stream has
     *                        moved on in the meantime the append is rejected, so that two
     *                        concurrent commands cannot both believe they are the latest.
     */
    void append(DeliveryId id, long expectedVersion, List<DomainEvent> events);

    /** Events of a delivery, in the order they happened. Empty if unknown. */
    List<DomainEvent> load(DeliveryId id);

    /** Number of events stored for a delivery, i.e. its current version. */
    long currentVersion(DeliveryId id);
}
