package de.tum.gossip.net.util;

import de.tum.gossip.net.ChannelInboundHandler;
import io.netty.handler.timeout.TimeoutException;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Created by Andi on 10.09.22.
 */
public abstract class ChannelCloseReason {
    public static class Exception extends ChannelCloseReason {
        public final Throwable cause;

        public Exception(java.lang.Throwable cause) {
            this.cause = cause;
        }

        public Exception(String message) {
            this(new java.lang.Exception(message));
        }

        public Exception(String message, Throwable cause) {
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

    public static class Timeout extends Exception {
        public Timeout(TimeoutException exception) {
            super(exception);
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
            logger.debug("Encountered exception", result.get());
        } else {
            logger.info("[{}] Remote disconnected: {}", channel.getHandle().remoteAddress(), getMessage());
        }
    }

    public abstract String getMessage();

    public abstract Optional<Throwable> asExceptionCause();
}