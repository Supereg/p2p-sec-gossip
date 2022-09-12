package de.tum.gossip.p2p.clients;

import de.tum.gossip.crypto.GossipCrypto;
import de.tum.gossip.net.util.ChannelState;
import de.tum.gossip.p2p.GossipModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread that is responsible for connecting to our remote peers.
 * <p>
 * Created by Andi on 12.09.22.
 */
public class GossipConnectionDispatcher extends Thread {
    private static final Long TICKS_RESOLUTION = 1000L;
    private static final Long TICKS_BURST_RESOLUTION = 20L;

    private final Logger logger = LogManager.getLogger(GossipConnectionDispatcher.class);

    private volatile boolean running = true;
    private volatile boolean paused = false;

    private final GossipModule module;
    private final Object pauseMonitor = new Object();

    /** The time millis the current or last running thread tick started on */
    private long tickMillis;

    public GossipConnectionDispatcher(GossipModule module) {
        super("GossipConnectionDispatcher " + module.hostKey.identity);
        this.module = module;
    }

    @Override
    public void run() {
        if (!running) {
            return;
        }

        try {
            long timeout = GossipCrypto.SECURE_RANDOM.nextLong(2*1000, 4* 1000);
            sleep(timeout);
        } catch (InterruptedException ignored) {}

        if (!running) {
            return;
        }

        // the initial burst of connections is at a much faster rate!
        int burstCount = Math.min(module.clients().size(), module.networkDegree);
        logger.debug("Starting gossip connection dispatcher with burstCount {}", burstCount);

        while(running) {
            try {
                if (paused) {
                    // dispatcher might be paused, once we reached maximum node degree!
                    synchronized (pauseMonitor) {
                        pauseMonitor.wait();
                    }
                }

                tickMillis = System.currentTimeMillis();

                var elements = module.clients().values().stream()
                        .filter(this::shouldDispatch)
                        .toList();

                if (!elements.isEmpty()) {
                    var randomClient = elements.get(GossipCrypto.SECURE_RANDOM.nextInt(elements.size()));
                    logger.debug("Dispatching connect to {}:{}", randomClient.client().hostname, randomClient.client().port);
                    randomClient.connect();
                }

                burstCount -= 1;

                long resolution = TICKS_RESOLUTION;
                if (burstCount > 0) {
                    resolution = TICKS_BURST_RESOLUTION;
                }

                //noinspection BusyWait
                sleep(Math.max(0, resolution - (System.currentTimeMillis() - tickMillis)));
            } catch (InterruptedException ignored) {}
        }
    }

    private boolean shouldDispatch(GossipClientContext context) {
        return context.state() == ChannelState.FREE
                && context.nextRetryMillis() >= 0 // negative value signals client to be disabled for connect dispatching
                && context.nextRetryMillis() <= tickMillis;
    }

    public void pause() {
        paused = true;
    }

    public void play() {
        if (paused) {
            paused = false;
            synchronized (pauseMonitor) {
                pauseMonitor.notify();
            }
        }
    }

    public void stopDispatcher() {
        running = false;
        this.interrupt();
    }
}