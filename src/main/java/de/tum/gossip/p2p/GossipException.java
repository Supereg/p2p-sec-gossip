package de.tum.gossip.p2p;

/**
 * Created by Andi on 07.09.22.
 */
public class GossipException extends Exception {
    public enum Type {
        UNSUBSCRIBED_SPREAD("Tried to spread information for a non-subscribed data type!"),
        INVALID_MESSAGE_ID("Either ttl of packet ran out or the queued packet was evicted due to space constraints or invalid data!"),
        RATE_LIMIT("Exceeded allowed amount of knowledge spread packets in certain time period!")
        ;

        private final String message;

        Type(String message) {
            this.message = message;
        }
    }

    public final Type type;

    public GossipException(Type type) {
        super(type.message);
        this.type = type;
    }

    public GossipException(Type type, Throwable cause) {
        super(type.message, cause);
        this.type = type;
    }
}