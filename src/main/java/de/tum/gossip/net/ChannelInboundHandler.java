package de.tum.gossip.net;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import de.tum.gossip.net.packets.InboundPacket;
import de.tum.gossip.net.packets.InboundPacketHandler;
import de.tum.gossip.net.packets.OutboundPacket;
import de.tum.gossip.net.util.ChannelCloseReason;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class ChannelInboundHandler extends SimpleChannelInboundHandler<InboundPacket<?>> {
    public static final AttributeKey<ProtocolDescription> PROTOCOL_DESCRIPTION_KEY = AttributeKey.valueOf("protocol-description");

    private record QueuedPacket(
            OutboundPacket packet,
            GenericFutureListener<? extends Future<? super Void>>[] futureListeners
    ) {}

    private final AtomicReference<InboundPacketHandler> packetHandler;

    // non-blocking queue to stored queued packets before the channel is open
    private final Queue<QueuedPacket> packetQueue = Queues.newConcurrentLinkedQueue();

    private Channel channel;
    private final Promise<ChannelInboundHandler> handshakePromise;
    private volatile boolean disconnected = false;

    public ChannelInboundHandler(InboundPacketHandler initialHandler, Promise<ChannelInboundHandler> handshakePromise) {
        this.packetHandler = new AtomicReference<>(initialHandler);
        this.handshakePromise = handshakePromise;
    }

    public Channel getHandle() {
        return channel;
    }

    public Promise<ChannelInboundHandler> handshakePromise() {
        return handshakePromise;
    }

    public Future<ChannelInboundHandler> handshakeFuture() {
        return handshakePromise;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen() && !disconnected;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.channel = ctx.channel();

        var handler = this.packetHandler.get();

        handler.logger().trace("Channel became active!");
        handler.onConnect(this);
        this.sendQueuedPackets();
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        this.close(new ChannelCloseReason.ChannelInactive());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InboundPacket msg) throws Exception {
        if (!this.isConnected()) {
            return;
        }

        var handler = packetHandler.get();
        if (!msg.applicableHandler(handler)) {
            throw new Exception("Incoming packet " + msg + " cannot be handled in the current state!");
        }

        handler.logger().trace("Dispatching incoming packet {} to handler!", msg);
        //noinspection unchecked
        msg.accept(handler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof TimeoutException timeout) {
            this.close(new ChannelCloseReason.Timeout(timeout));
        } else {
            this.close(new ChannelCloseReason.Exception(cause));
        }
    }

    public <Handler extends InboundPacketHandler> void replacePacketHandler(Handler handler) {
        Preconditions.checkNotNull(handler);

        InboundPacketHandler packetHandler;
        do {
            packetHandler = this.packetHandler.get();
        } while (!this.packetHandler.compareAndSet(packetHandler, handler));

        packetHandler.onHandlerRemove();
        packetHandler.logger().trace("Replaced packet handler with new {} instance!", handler.getClass());

        if (this.isConnected()) {
            handler.onConnect(this);
        }
    }

    public <Handler extends InboundPacketHandler> void replacePacketHandler(Supplier<Handler> handlerSupplier) {
        var handler = handlerSupplier.get();
        replacePacketHandler(handler);
    }

    @SafeVarargs
    public final void sendPacket(OutboundPacket packet, GenericFutureListener<? extends Future<? super Void>>... genericFutureListeners) {
        if (!this.isConnected()) {
            packetQueue.add(new QueuedPacket(packet, genericFutureListeners));
            return;
        }

        this.sendQueuedPackets();
        this.flushPacket(packet, genericFutureListeners);
    }

    private void sendQueuedPackets() {
        QueuedPacket queuedPacket;
        while ((queuedPacket = packetQueue.poll()) != null) {
            flushPacket(queuedPacket.packet, queuedPacket.futureListeners);
        }
    }

    @SafeVarargs
    private void flushPacket(OutboundPacket packet, GenericFutureListener<? extends Future<? super Void>>... genericFutureListeners) {
        Runnable runnable = () -> {
            this.packetHandler.get().logger().trace("Flushing packet {}", packet);
            var future = channel.writeAndFlush(packet);

            if (genericFutureListeners.length > 0) {
                future.addListeners(genericFutureListeners);
            }

            future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        };

        if (channel.eventLoop().inEventLoop()) {
            runnable.run();
        } else {
            channel.eventLoop().execute(runnable);
        }
    }

    public synchronized void close(ChannelCloseReason reason) {
        var handler = this.packetHandler.get();

        if (!this.disconnected) {
            this.disconnected = true;

            handler.logger().trace("Calling disconnect handler due to {}", reason);
            reason.handleBeforeClose(this, handler.logger());
            handler.onDisconnect(reason);
        }

        if (this.channel.isOpen()) {
            handler.logger().trace("Closing the netty channel!");
            this.channel.close().awaitUninterruptibly();
        }
    }
}