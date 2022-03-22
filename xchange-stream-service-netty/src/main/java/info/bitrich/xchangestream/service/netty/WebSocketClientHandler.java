package info.bitrich.xchangestream.service.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
  private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandler.class);
  private static final ThreadLocal<EventExecutor> executorThreadLocal = new ThreadLocal<>();

  private final StringBuilder currentMessage = new StringBuilder();

  public interface WebSocketMessageHandler {
    public void onMessage(String message);
  }

  protected final WebSocketClientHandshaker handshaker;
  protected final WebSocketMessageHandler handler;
  private ChannelPromise handshakeFuture;

  public WebSocketClientHandler(
      WebSocketClientHandshaker handshaker, WebSocketMessageHandler handler) {
    this.handshaker = handshaker;
    this.handler = handler;
  }

  public ChannelFuture handshakeFuture() {
    return handshakeFuture;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    handshakeFuture = ctx.newPromise();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    handshaker.handshake(ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    LOG.info("WebSocket Client disconnected! {}", ctx.channel());
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel ch = ctx.channel();
    if (!handshaker.isHandshakeComplete()) {
      try {
        handshaker.finishHandshake(ch, (FullHttpResponse) msg);
        LOG.info("WebSocket Client connected! {}", ctx.channel());
        handshakeFuture.setSuccess();
      } catch (WebSocketHandshakeException e) {
        LOG.error("WebSocket Client failed to connect. {} {}", e.getMessage(), ctx.channel());
        handshakeFuture.setFailure(e);
      }
      return;
    }

    if (msg instanceof FullHttpResponse) {
      FullHttpResponse response = (FullHttpResponse) msg;
      throw new IllegalStateException(
          "Unexpected FullHttpResponse (getStatus="
              + response.status()
              + ", content="
              + response.content().toString(CharsetUtil.UTF_8)
              + ')');
    }

    WebSocketFrame frame = (WebSocketFrame) msg;
    if (frame instanceof TextWebSocketFrame) {
      dealWithTextFrame(ctx, (TextWebSocketFrame) frame);
    } else if (frame instanceof ContinuationWebSocketFrame) {
      dealWithContinuation(ctx, (ContinuationWebSocketFrame) frame);
    } else if (frame instanceof PingWebSocketFrame) {
      LOG.debug("WebSocket Client received ping");
      ch.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
    } else if (frame instanceof PongWebSocketFrame) {
      LOG.debug("WebSocket Client received pong");
    } else if (frame instanceof CloseWebSocketFrame) {
      LOG.info("WebSocket Client received closing! {}", ctx.channel());
      ch.close();
    }
  }

  private void dealWithTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    if (frame.isFinalFragment()) {
      try {
        executorThreadLocal.set(ctx.executor());
        handler.onMessage(frame.text());

      } finally {
        executorThreadLocal.set(null);

      }
      return;
    }
    currentMessage.append(frame.text());
  }

  private void dealWithContinuation(ChannelHandlerContext ctx, ContinuationWebSocketFrame frame) {
    currentMessage.append(frame.text());
    if (frame.isFinalFragment()) {
      try {
        executorThreadLocal.set(ctx.executor());
        handler.onMessage(currentMessage.toString());

      } finally {
        executorThreadLocal.set(null);

      }
      currentMessage.setLength(0);
    }
  }

  public static EventExecutor getCurrentThreadExecutor() {
    return executorThreadLocal.get();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error(
        "WebSocket client {} encountered exception ({} - {}). Closing",
        ctx.channel(),
        cause.getClass().getSimpleName(),
        cause.getMessage(),
        cause);
    if (!handshakeFuture.isDone()) {
      handshakeFuture.setFailure(cause);
    }
    ctx.close();
  }
}
