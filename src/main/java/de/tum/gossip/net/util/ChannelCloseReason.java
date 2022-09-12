package de.tum.gossip.net.util;

import de.tum.gossip.net.ChannelInboundHandler;
import io.netty.handler.timeout.TimeoutException;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Created by Andi on 10.09.22.
 */
public abstract class ChannelCloseReason {
    public abstract static class LocationCapturing extends ChannelCloseReason {
        public final Throwable cause;

        public LocationCapturing(java.lang.Throwable cause) {
            this.cause = cause;
        }

        public LocationCapturing(String message) {
            this(new java.lang.Exception(message));
        }

        public LocationCapturing(String message, Throwable cause) {
            this(new java.lang.Exception(message, cause));
        }

        @Override
        public String getMessage() {
            return cause.getMessage();
        }

        @Override
        public Optional<Throwable> asExceptionCause() {
            return Optional.of(cause);
        }
    }

    public static class Exception extends LocationCapturing {
        public Exception(Throwable cause) {
            super(cause);
        }

        public Exception(String message) {
            super(message);
        }

        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class Timeout extends LocationCapturing {
        public Timeout(TimeoutException exception) {
            super(exception);
        }

        @Override
        public String getMessage() {
            return "Netty Timeout Handler";
        }
    }

    public static class Message extends ChannelCloseReason {
        public final String message;

        public Message(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public Optional<Throwable> asExceptionCause() {
            return Optional.empty();
        }
    }

    public static class ChannelInactive extends Message {
        public ChannelInactive() {
            super("Channel became inactive!");
        }
    }

    protected ChannelCloseReason() {}

    public void handleBeforeClose(ChannelInboundHandler channel, Logger logger) {
        var result = asExceptionCause();

        if (result.isPresent()) {
            logger.error("[{}] Remote disconnected erroneously: {}", channel.getHandle().remoteAddress(), getMessage());
            if (this instanceof Exception) {
                logger.debug("Printing exception at handleBeforeClose()", new java.lang.Exception(result.get()));
            } else {
                logger.trace("Printing exception at handleBeforeClose()", new java.lang.Exception(result.get()));
            }
        } else {
            logger.info("[{}] Remote disconnected: {}", channel.getHandle().remoteAddress(), getMessage());
        }
    }

    public abstract String getMessage();

    public abstract Optional<Throwable> asExceptionCause();
}