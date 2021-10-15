import handler.MessageHandler;
import handler.ServerInputHandler;
import handler.ServerOutputHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Server {
    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new StringDecoder(),
                                    new StringEncoder(),
                                    new MessageHandler(),
                                    new ServerInputHandler(),
                                    new ServerOutputHandler());
                        }
                    });

            ChannelFuture channelFuture = server.bind(getPort()).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

    private int getPort() {
        Properties properties = new Properties();
        int port = 9000;
        try (InputStream in = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(in);
            port = Integer.parseInt(properties.getProperty("port"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }
}

