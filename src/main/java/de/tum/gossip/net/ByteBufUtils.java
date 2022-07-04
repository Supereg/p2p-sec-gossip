package de.tum.gossip.net;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 27.06.22.
 */
public class ByteBufUtils {
    public static void writeVarInt(ByteBuf buf, int value) {
        int part;

        do {
            part = value & 0x7F;
            value >>>= 7;

            if (value != 0)
                part |= 0x80;

            buf.writeByte(part);
        } while (value != 0);
    }

    public static int readVarInt(ByteBuf buf) {
        int out = 0;
        int bytes = 0;
        byte in;

        do {
            in = buf.readByte();
            out |= (in & 0x7F) << (bytes++ * 7);

            if (bytes > 5)
                throw new RuntimeException("VarInt too big");

        } while ((in & 0x80) == 0x80);

        return out;
    }

    public static void writeString(ByteBuf buf, String value) {
        Preconditions.checkArgument(value.length() <= Short.MAX_VALUE, "Cannot send string longer than Short.MAX_VALUE (got %s characters)", value.length());

        byte[] bytes = value.getBytes(Charsets.UTF_8);
        ByteBufUtils.writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString(ByteBuf buf) {
        int length = ByteBufUtils.readVarInt(buf);
        Preconditions.checkArgument(length <= Short.MAX_VALUE, "Cannot receive string longer than Short.MAX_VALUE (got %s characters)", length);

        byte[] bytes = new byte[length];
        buf.readBytes(bytes);

        return new String(bytes, Charsets.UTF_8);
    }
}