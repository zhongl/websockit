package me.zhongl.driver

import java.net.URI
import io.netty.handler.codec.http.websocketx._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{FullHttpResponse, HttpHeaders, HttpObjectAggregator, HttpClientCodec}
import io.netty.buffer.ByteBuf
import org.slf4j.LoggerFactory

abstract class WebSocketClient(
  uri: URI,
  subprotocol: String = null,
  allowExtensions: Boolean = false,
  customHeaders: HttpHeaders = HttpHeaders.EMPTY_HEADERS,
  maxFramePayloadLength: Int = 4096
  ) {

  private lazy val log   = LoggerFactory.getLogger(classOf[WebSocketClient])
  private lazy val group = new NioEventLoopGroup()

  @volatile private var online           = false
  @volatile private var channel: Channel = _

  def connect(): Unit = {

    try {
      require(uri.getScheme == "ws", "Unsupported protocol:" + uri.getScheme)

      val b = new Bootstrap()

      b.group(group).channel(classOf[NioSocketChannel]).handler(new ChannelInitializer[SocketChannel]() {
        def initChannel(ch: SocketChannel) {
          val pipeline = ch.pipeline()
          pipeline.addLast("http-codec", new HttpClientCodec())
          pipeline.addLast("aggregator", new HttpObjectAggregator(8194))
          pipeline.addLast("ws-handler", handle)
        }
      })

      channel = b.connect(uri.getHost, uri.getPort).sync().channel()
      handle.handshakeFuture.sync()
      online = true
    } catch {
      case t: Throwable => group.shutdownGracefully(); throw t
    }
  }

  def disconnet(): Unit = {
    if (!online) return
    online = false
    channel.writeAndFlush(new CloseWebSocketFrame())
    channel.closeFuture().sync()
  }

  def send(text: String): Unit = {
    checkState(online, "Can't send text after disconnected.")
    channel.writeAndFlush(new TextWebSocketFrame(text))
  }

  def send(data: ByteBuf): Unit = {
    checkState(online, "Can't send data after disconnected.")
    channel.writeAndFlush(new BinaryWebSocketFrame(data))
  }

  def ping(): Unit = {
    checkState(online, "Can't ping after disconnected.")
    channel.writeAndFlush(new PingWebSocketFrame())
  }

  def receive(frame: WebSocketFrame): Unit

  private def checkState(e: => Boolean, m: String) = if (!e) throw new IllegalStateException(m)

  protected lazy val handle = new Handle

  protected class Handle extends SimpleChannelInboundHandler[AnyRef]() {

    @volatile var handshakeFuture: ChannelPromise = _

    private val handshaker = WebSocketClientHandshakerFactory
      .newHandshaker(uri, WebSocketVersion.V13, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength)

    override def handlerAdded(ctx: ChannelHandlerContext): Unit = handshakeFuture = ctx.newPromise()

    override def channelActive(ctx: ChannelHandlerContext): Unit = handshaker.handshake(ctx.channel())

    override def channelInactive(ctx: ChannelHandlerContext): Unit = {
      log.info("WebSocked client disconnected.")
      group.shutdownGracefully()
    }

    def channelRead0(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
      if (!handshaker.isHandshakeComplete()) {
        handshaker.finishHandshake(ctx.channel(), msg.asInstanceOf[FullHttpResponse]);
        log.info("WebSocked client connected.")
        handshakeFuture.setSuccess();
        return;
      }

      msg match {
        case f: PingWebSocketFrame  => ctx.channel.writeAndFlush(new PongWebSocketFrame(f.content()))
        case f: CloseWebSocketFrame => close(ctx.channel, f)
        case f: WebSocketFrame      => receive(f)
        case m                      => throw new IllegalStateException("Unknown message: " + m)
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      if (!handshakeFuture.isDone) {
        handshakeFuture.setFailure(cause)
        log.error("WebSocket client connect failed.")
      }

      log.error("Unexpected state: ", cause)
      ctx.close()
    }

    private def close(channel: Channel, frame: CloseWebSocketFrame): Unit = {
      if (online) {
        log.error("WebSocket server close connect: {}, {}", frame.statusCode(), frame.reasonText())

        val f = new CloseWebSocketFrame(frame.statusCode(), frame.reasonText())
        channel.writeAndFlush(f).addListener(new ChannelFutureListener {
          def operationComplete(future: ChannelFuture) {
            channel.close()
          }
        })

      } else {
        channel.close()
      }
    }
  }

}

