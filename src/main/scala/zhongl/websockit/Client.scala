package zhongl.websockit

import io.netty.handler.codec.http.websocketx._
import java.net.URI
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http._
import java.util.concurrent.{ ThreadFactory, TimeoutException, TimeUnit, SynchronousQueue }

object Client {

  type Receive = PartialFunction[WebSocketFrame, Option[WebSocketFrame]]

  def stub(uri: URI)(r: Receive): Unit = connect(uri)(r)

  def drive(uri: URI, text: String) = {
    val queue = new SynchronousQueue[AnyRef]()

    val (channel, handshaker) = connect(uri) {
      case f: TextWebSocketFrame  => queue.put(f.retain()); None
      case f: CloseWebSocketFrame => Some(f.retain())
      case f                      => None
    }

    try {
      channel.writeAndFlush(new TextWebSocketFrame(text))
      queue.poll(3, TimeUnit.SECONDS) match {
        case null                  => throw new TimeoutException()
        case t: Throwable          => throw t
        case t: TextWebSocketFrame => t.text()
      }
    } finally {
      handshaker.close(channel, new CloseWebSocketFrame())
    }
  }

  private def initializer(handler: ChannelHandler) = new ChannelInitializer[SocketChannel] {
    def initChannel(ch: SocketChannel): Unit = ch.pipeline().addLast(
      new HttpClientCodec(), new HttpObjectAggregator(8192), handler
    )
  }

  private def connect(uri: URI)(r: Receive) = {

    require(uri.getScheme == "ws", "Protocol should be ws:// .")

    val g = new NioEventLoopGroup(1, new ThreadFactory {
      def newThread(r: Runnable) = new Thread(r, "Client-EventLoopGroup")
    })

    try {
      val h = WebSocketClientHandshakerFactory
        .newHandshaker(uri, WebSocketVersion.V13, null, false, HttpHeaders.EMPTY_HEADERS)

      val c = new Bootstrap()
        .group(g)
        .channel(classOf[NioSocketChannel])
        .handler(initializer(new Handler(h, r)))
        .connect(uri.getHost, uri.getPort)
        .sync().channel()

      c.closeFuture().addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture): Unit = g.shutdownGracefully()
      })

      h.handshake(c).sync()

      (c, h)
    } catch {
      case t: Throwable => g.shutdownGracefully(); throw t
    }
  }

  private class Handler(h: WebSocketClientHandshaker, r: Receive) extends SimpleChannelInboundHandler[AnyRef]() {
    def channelRead0(ctx: ChannelHandlerContext, msg: AnyRef): Unit = msg match {
      case r: FullHttpResponse if !h.isHandshakeComplete => h.finishHandshake(ctx.channel(), r)
      case f: WebSocketFrame                             => handle(ctx, f)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = ctx.close()

    private def handle(c: ChannelHandlerContext, f: WebSocketFrame) = r(f) map {
      case f: CloseWebSocketFrame => h.close(c.channel(), f)
      case f                      => c.writeAndFlush(f)
    }

  }

}
