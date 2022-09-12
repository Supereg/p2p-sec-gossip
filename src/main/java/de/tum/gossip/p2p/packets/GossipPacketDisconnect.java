package de.tum.gossip.p2p.packets;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.util.ChannelCloseReason;
import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.packets.Packet;
import de.tum.gossip.p2p.protocol.GossipPacketHandler;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * A packet signalling that the remote peer is closing the connection.
 * This packet may be sent in any protocol state.
 */
public class GossipPacketDisconnect implements Packet<GossipPacketHandler> {
    public enum Reason {
        /** Connection was closed normally. */
        NORMAL(0),
        /** Handshake header contained unsupported version. */
        UNSUPPORTED(1),

        /** Handshake failed to failed signature or challenge response verification. */
        AUTHENTICATION(64),
        /** Error occurred unexpectedly. */
        UNEXPECTED_FAILURE(65),
        /** Operation was cancelled due to erroneous state. */
        CANCELLED(66),
        /** The requested operation is considered to be a duplication. */
        DUPLICATE(67),
        /** The requested resource or operation is unavailable. */
        BUSY(68),
        NOT_ALLOWED(69), // TODO enforce rate limiting with it!
        /** The connection or operation timed out. */
        TIMEOUT(70),
        ;
        final byte reason;

        Reason(int reason) {
            Preconditions.checkState(reason >= 0 && reason <= 255);
            this.reason = (byte) reason;
        }

        public static Optional<Reason> of(ChannelCloseReason reason) {
            if (reason instanceof ChannelCloseReason.Timeout) {
                return Optional.of(TIMEOUT);
            } else if (reason instanceof ChannelCloseReason.Exception exception) {
                if (exception.cause instanceof GossipPacketHandshakeHello.UnsupportedVersionException) {
                    return Optional.of(UNSUPPORTED);
                }
                // any other exceptions based close reasons are considered a failure!
                return Optional.of(UNEXPECTED_FAILURE);
            } else {
                return Optional.empty();
            }
        }

        static Reason fromReason(byte reasonByte) {
            for (var reason : values()) {
                if (reason.reason == reasonByte) {
                    return reason;
                }
            }
            throw new IllegalArgumentException("Reason " + reasonByte + " is not defined!");
        }
    }

    public interface DisconnectReasonContaining {
        Reason disconnectReason();
    }

    public static class OutboundCloseReason extends ChannelCloseReason.LocationCapturing implements DisconnectReasonContaining {
        private final Reason closeReason;

        public OutboundCloseReason(Reason reason, String message) {
            super(message);
            this.closeReason = reason;
        }

        public OutboundCloseReason(Reason reason, String message, Throwable cause) {
            super(message, cause);
            this.closeReason = reason;
        }

        public OutboundCloseReason(Reason reason, Throwable cause) {
            super(cause);
            this.closeReason = reason;
        }

        @Override
        public Reason disconnectReason() {
            return closeReason;
        }

        @Override
        public String getMessage() {
            return "Sent close reason " + closeReason + " due to: " + super.getMessage();
        }

        @Override
        public void handleBeforeClose(ChannelInboundHandler channel, Logger logger) {
            super.handleBeforeClose(channel, logger);
            channel.sendPacket(new GossipPacketDisconnect(closeReason));
        }
    }

    public static class InboundCloseReason extends ChannelCloseReason.LocationCapturing implements DisconnectReasonContaining {
        private final Reason reason;
        public InboundCloseReason(Reason reason) {
            super("PacketDisconnect with reason " + reason);
            this.reason = reason;
        }

        @Override
        public Reason disconnectReason() {
            return reason;
        }
    }

    public Reason reason;

    public GossipPacketDisconnect() {}

    public GossipPacketDisconnect(Reason reason) {
        this.reason = reason;
    }

    public ChannelCloseReason channelCloseReason(ChannelInboundHandler channel) {
        return new InboundCloseReason(reason);
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeByte(reason.reason);
        byteBuf.writeBytes(new byte[] {0, 0, 0});
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        reason = Reason.fromReason(byteBuf.readByte());

        var bytes = new byte[3];
        byteBuf.readBytes(bytes);
    }

    @Override
    public void accept(GossipPacketHandler handler) {
        handler.handle(this);
    }
}