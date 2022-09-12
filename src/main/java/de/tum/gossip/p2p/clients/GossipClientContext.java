package de.tum.gossip.p2p.clients;

import de.tum.gossip.net.ChannelInboundHandler;
import de.tum.gossip.net.TCPClient;
import de.tum.gossip.net.util.ChannelCloseReasonCause;
import de.tum.gossip.net.util.ChannelState;
import de.tum.gossip.p2p.packets.GossipPacketDisconnect;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by Andi on 12.09.22.
 */
public class GossipClientContext {
    private static final Long SECOND_MILLIS = 1000L;
    private static final Long MINUTE_MILLIS = 60*SECOND_MILLIS;
    private static final Long HOUR_MILLIS = 60*MINUTE_MILLIS;
    private static final Long DAY_MILLIS = 24*HOUR_MILLIS;

    private final Logger logger = LogManager.getLogger(GossipClientContext.class);

    private final TCPClient client;
    private long consecutiveFailureCount;
    private volatile long nextRetryMillis = 0;

    public GossipClientContext(TCPClient client) {
        this.client = client;
    }

    public Future<Void> connect() {
        return client.connect().addListener(this::handleConnected);
    }

    public Future<ChannelInboundHandler> handshakeFuture() {
        return client.handshakeFuture();
    }

    public ChannelState state() {
        return client.state();
    }

    public long nextRetryMillis() {
        return nextRetryMillis;
    }

    public TCPClient client() {
        return client;
    }

    public synchronized void signalServerBoundSessionDisconnect() {
        if (nextRetryMillis == -1) {
            calculateNextRetryMillis(10 * MINUTE_MILLIS);
        }
    }

    public Future<Void> disconnect() {
        return client.disconnect().addListener(this::handleDisconnected);
    }

    private synchronized <F extends Future<? super Void>> void handleConnected(F future) {
        if (future.isSuccess()) {
            client.handshakeFuture().addListener(this::handleHandshakeCompleted);
            return;
        }

        consecutiveFailureCount += 1;

        if (!future.isCancelled() && !future.isSuccess()) {
            logger.debug("[ " + client.toString() + "] Failed to connect to remote peer for the {}. time",
                    future.cause(), consecutiveFailureCount);
        }

        calculateNextRetryMillis(MINUTE_MILLIS);
    }

    private synchronized void handleHandshakeCompleted(Future<? super ChannelInboundHandler> future) {
        if (future.isSuccess()) {
            consecutiveFailureCount = 0;
            nextRetryMillis = -1;
            return;
        }

        consecutiveFailureCount += 1;
        long nextRetryMillisFactor = SECOND_MILLIS;

        if (!future.isCancelled() && !future.isSuccess()) {
            logger.debug("[ " + client.toString() + "] Failed to connect/initiate handshake with remote peer for the {}. time: {}",
                    consecutiveFailureCount, future.cause());

            if (future.cause() instanceof ChannelCloseReasonCause reasonCause
                && reasonCause.channelCloseReason instanceof GossipPacketDisconnect.DisconnectReasonContaining disconnectReasonContaining) {
                var reason = disconnectReasonContaining.disconnectReason();

                switch (reason) {
                    case DUPLICATE -> {
                        nextRetryMillisFactor = -1; // disables the retry
                        consecutiveFailureCount = 0;
                    }
                    case NORMAL, UNSUPPORTED, CANCELLED -> nextRetryMillisFactor = DAY_MILLIS;
                    case BUSY, AUTHENTICATION, UNEXPECTED_FAILURE -> nextRetryMillisFactor = HOUR_MILLIS;
                    case NOT_ALLOWED -> nextRetryMillisFactor = MINUTE_MILLIS;
                    // timeout -> SECOND_MILLIS
                }
            } else {
                nextRetryMillisFactor = MINUTE_MILLIS;
            }
        }

        calculateNextRetryMillis(nextRetryMillisFactor);
    }

    private void calculateNextRetryMillis(long factor) {
        if (factor == -1) {
            nextRetryMillis = -1;
            return;
        }

        // assumes to be called from within a locked state!
        long duration = (long) Math.pow(2, Math.max(0, consecutiveFailureCount - 1)) * factor;
        duration += 10 * SECOND_MILLIS; // general 10s delay after disconnect!
        nextRetryMillis = System.currentTimeMillis() + duration;

        // taken from https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format
        var formatted = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
        logger.info("Failed to connect to remote peer. Trying again in {}", formatted);
    }

    private synchronized <F extends Future<? super Void>> void handleDisconnected(F future) {
        logger.debug("[{}] Client was properly disconnected!", client);
    }
}