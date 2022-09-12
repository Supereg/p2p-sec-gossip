package de.tum.gossip.p2p.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import de.tum.gossip.p2p.protocol.EstablishedSession;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates all context information of a gossip knowledge message that is part of the network.
 */
public class GossipMessage {
    /**
     * The Gossip Message id.
     */
    public final GossipMessageId messageId;
    /**
     * Null if the message was sent by us. Otherwise, sent to the session we received the message from.
     */
    public final AtomicReference<EstablishedSession> receivedFrom;
    /**
     * The set of destinations we sent knowledge packets already (contains `receivedFrom` to easily check for targets).
     */
    public final Set<EstablishedSession> completedTransmissions;

    // KNOWLEDGE
    /**
     * The next ttl. The ttl values to used for outgoing knowledge spreading packets.
     * Set to -1, if the packet reached its end of life at this hop.
     */
    public final int nextTTL;
    /**
     * The data type this message was received for.
     */
    public final DataType dataType;
    /**
     * The message data for the given data type.
     */
    public final byte[] data;

    /**
     * The notificationId used within the API-layer protocol. It is only set, if we expect validations
     * (expect to continue to spread the message into the network).
     */
    public final AtomicReference<MessageNotificationId> notificationId;
    /**
     * The amount of pending validation we expect to receive from the API layer (typically 1).
     * A count of 0, indicates that everything was validated and the packet can be sent further into the network.
     * A value of -1, indicates that is not expected to be forwarded (e.g. ttl ran out), and consequentially no
     * validations are expected to arrive.
     */
    private final AtomicInteger pendingValidations;

    public GossipMessage(GossipMessageId messageId, int nextTTL, DataType dataType, byte[] data) {
        this(messageId, null, nextTTL, dataType, data);
    }

    public GossipMessage(GossipMessageId messageId, EstablishedSession receivedFrom, int nextTTL, DataType dataType, byte[] data) {
        Preconditions.checkState(nextTTL != 1);

        this.messageId = messageId;
        this.receivedFrom = new AtomicReference<>(receivedFrom);
        this.completedTransmissions = Sets.newConcurrentHashSet();
        if (receivedFrom != null) {
            this.completedTransmissions.add(receivedFrom);
        }

        this.nextTTL = nextTTL;
        this.dataType = dataType;
        this.data = data;

        this.notificationId = new AtomicReference<>();
        this.pendingValidations = new AtomicInteger(-1);
    }

    public boolean shouldForward() {
        // a value of -1 signals that this message has reached its end of life at this hop
        return nextTTL >= 0;
    }

    public void initPendingValidations(MessageNotificationId notificationId, int pendingValidations) {
        boolean success = this.notificationId.compareAndSet(null, notificationId);
        Preconditions.checkState(success, "Reached inconsistent state!");
        this.pendingValidations.set(pendingValidations);
    }

    public boolean reportValidity() {
        var result = pendingValidations.getAndDecrement();
        return result == 0;
    }
}