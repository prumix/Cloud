package handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;

public class ServerOutputHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String message = String.valueOf(msg);
        System.out.println("out: " + message);
        ByteBuf buf = ctx.alloc().directBuffer();
        buf.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
    }
}