package zhongl.websockit.stub

import io.netty.handler.codec.http.websocketx._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpHeaders.Names._
import org.slf4j.LoggerFactory
import scala.Some

class WebSocketServer(
    port: Int = 12306,
    defaultMock: PartialFunction[WebSocketFrame, Option[WebSocketFrame]] = { case f: WebSocketFrame => Some(f.retain) }) {

  require(port > 1024 && port < 65536, s"Invalid port number: $port")

  private lazy val log = LoggerFactory.getLogger(classOf[WebSocketServer])
  private lazy val boss = new NioEventLoopGroup()
  private lazy val worker = new NioEventLoopGroup()

  type Mock = PartialFunction[WebSocketFrame, Option[WebSocketFrame]]

  private var mocks = Map.empty[String, Mock].withDefaultValue(defaultMock)
  private var channels = Map.empty[ChannelHandlerContext, String]

  def start(): Unit = {

    try {
      new ServerBootstrap()
        .group(boss, worker)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(new Initializer())
        .bind(port).sync()

      log.info(s"WebSocket server listen at http://localhost:${port}")
    } catch {
      case t: Throwable => shutdown(); throw t
    }

  }

  def stop(): Unit = shutdown()

  private def shutdown(): Unit = {
    boss.shutdownGracefully()
    worker.shutdownGracefully()
  }

  private class Initializer extends ChannelInitializer[SocketChannel] {
    def initChannel(ch: SocketChannel): Unit = ch.pipeline()
      .addLast("codec-http", new HttpServerCodec())
      .addLast("aggergator", new HttpObjectAggregator(65536))
      .addLast("handler", new Handle())
  }

  private class Handle extends SimpleChannelInboundHandler[Object] {

    private var handshaker: WebSocketServerHandshaker = _

    def channelRead0(ctx: ChannelHandlerContext, msg: Object): Unit = msg match {
      case r: FullHttpRequest => handleHttpRequest(ctx, r)
      case f: WebSocketFrame  => handleWebSocketFrame(ctx, f)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      log.error("caught:", cause)
      channels = channels - ctx
      ctx.channel().close()
    }

    private def handleHttpRequest(implicit context: ChannelHandlerContext, request: FullHttpRequest): Unit =
      request match {
        case GetIndex()         =>
        case PostIndex()        =>
        case DecodeNotSuccess() => sendThenClose(response(BAD_REQUEST))
        case MethodNotAllowed() => sendThenClose(response(METHOD_NOT_ALLOWED))
        case _                  => handshake(request)
      }

    private def handleWebSocketFrame(context: ChannelHandlerContext, frame: WebSocketFrame): Unit =
      frame match {
        case f: CloseWebSocketFrame => handshaker.close(context.channel(), f.retain())
        case f: PingWebSocketFrame  => context.channel().writeAndFlush(new PongWebSocketFrame(f.content().retain()))
        case _                      => mocks(channels(context))(frame) foreach { context.channel().writeAndFlush }
      }

    private def response(status: HttpResponseStatus) = new DefaultFullHttpResponse(HTTP_1_1, status)

    private def sendThenClose(response: FullHttpResponse)(implicit ctx: ChannelHandlerContext): Unit = {
      ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

    private def handshake(request: FullHttpRequest)(implicit ctx: ChannelHandlerContext): Unit = {
      val uri = "ws://" + request.headers().get(HOST) + request.getUri // TODO ws | wss ?
      // TODO Support subprotocol
      handshaker = new WebSocketServerHandshakerFactory(uri, null, false).newHandshaker(request)
      if (handshaker == null)
        WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel())
      else {
        handshaker.handshake(ctx.channel(), request)
        channels = channels + (ctx -> request.getUri)
      }
    }

  }

  private object PostIndex {
    def unapply(request: FullHttpRequest) = request.getUri == "/" && request.getMethod == HttpMethod.POST
  }

  private object GetIndex {
    def unapply(request: FullHttpRequest) = request.getUri == "/" && request.getMethod == HttpMethod.GET
  }

  private object DecodeNotSuccess {
    def unapply(request: FullHttpRequest) = !request.getDecoderResult.isSuccess
  }

  private object MethodNotAllowed {
    def unapply(request: FullHttpRequest) = request.getMethod != HttpMethod.GET
  }

}
