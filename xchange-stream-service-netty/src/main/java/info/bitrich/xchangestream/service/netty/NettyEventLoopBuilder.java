package info.bitrich.xchangestream.service.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

public class NettyEventLoopBuilder {

  private static final Logger logger = LoggerFactory.getLogger(NettyEventLoopBuilder.class);

  private static SocketType nativeSocketType;

  private final SocketType socketType;

  public static SocketType nativeSocketType() {
    if (nativeSocketType != null) {
      return nativeSocketType;
    }
    if (Epoll.isAvailable()) {
      logger.info("Epoll transport available");
      nativeSocketType = SocketType.EPOLL;

    } else if (KQueue.isAvailable()) {
      logger.info("Kqueue transport available");
      nativeSocketType = SocketType.KQUEUE;

    } else {
      logger.info("Native transport not available, using NIO");
      nativeSocketType = SocketType.NIO;

    }
    return nativeSocketType;
  }

  private final ThreadFactory threadFactory;

  private final Class<? extends SocketChannel> clientChannelClass;


  private final int numberOfThreads;

  public NettyEventLoopBuilder(boolean useNative, int numberOfThreads, ThreadFactory threadFactory) {
    this(useNative ? nativeSocketType() : SocketType.NIO, numberOfThreads, threadFactory);
  }


  public NettyEventLoopBuilder(SocketType socketType, int numberOfThreads, ThreadFactory threadFactory) {
    switch (socketType) {
      case NIO:
        clientChannelClass = NioSocketChannel.class;
        break;
      case EPOLL:
        clientChannelClass = EpollSocketChannel.class;

        break;
      case KQUEUE:
        clientChannelClass = KQueueSocketChannel.class;
        break;
      default:
        throw new IllegalArgumentException("Unsupported socket type:" + socketType.name());
    }
    this.socketType = socketType;
    this.numberOfThreads = numberOfThreads;
    this.threadFactory = threadFactory;
  }

  public EventLoopGroup workerEventLoopGroup() {
    EventLoopGroup workerEventLoopGroup;
    switch (socketType) {
      case NIO:
        workerEventLoopGroup = new NioEventLoopGroup(numberOfThreads, threadFactory);
        break;
      case EPOLL:
        workerEventLoopGroup = new EpollEventLoopGroup(numberOfThreads, threadFactory);
        break;
      case KQUEUE:
        workerEventLoopGroup = new KQueueEventLoopGroup(numberOfThreads, threadFactory);
        break;
      default:
        throw new IllegalArgumentException("Unsupported socket type:" + socketType.name());
    }

    return workerEventLoopGroup;
  }


  public Class<? extends SocketChannel> clientChannelClass() {
    return clientChannelClass;
  }


  public enum SocketType {
    NIO,
    EPOLL,
    KQUEUE

  }
}