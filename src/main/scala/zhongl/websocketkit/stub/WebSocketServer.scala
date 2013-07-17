package zhongl.websocketkit.stub

import io.netty.handler.codec.http.websocketx._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler, ChannelInitializer }
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpHeaders.Names._
import org.slf4j.LoggerFactory

abstract class WebSocketServer(port: Int = 12306, path: String = "/") {

  protected lazy val log = LoggerFactory.getLogger(classOf[WebSocketServer])

  private lazy val boss = new NioEventLoopGroup()
  private lazy val worker = new NioEventLoopGroup()

  def receive: PartialFunction[WebSocketFrame, Option[WebSocketFrame]]

  def start(): Unit = {

    try {
      new ServerBootstrap()
        .group(boss, worker)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(new Initializer())
        .bind(port).sync()
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
      case f: WebSocketFrame => handleWebSocketFrame(ctx, f)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      log.error("caught:", cause)
      ctx.channel().close()
    }

    private def handleHttpRequest(implicit context: ChannelHandlerContext, request: FullHttpRequest): Unit =
      request match {
        case DecodeNotSuccess() => sendThenClose(response(BAD_REQUEST))
        case UriIsNotPath() => sendThenClose(response(NOT_FOUND))
        case _ => handshake(request)
      }

    private def handleWebSocketFrame(context: ChannelHandlerContext, frame: WebSocketFrame): Unit =
      frame match {
        case f: CloseWebSocketFrame => handshaker.close(context.channel(), f.retain())
        case f: PingWebSocketFrame => context.channel().writeAndFlush(new PongWebSocketFrame(f.content().retain()))
        case _ => receive(frame) foreach { context.channel().writeAndFlush }
      }

    private def response(status: HttpResponseStatus) = new DefaultFullHttpResponse(HTTP_1_1, status)

    private def sendThenClose(response: FullHttpResponse)(implicit ctx: ChannelHandlerContext): Unit = {
      ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

    private def handshake(request: FullHttpRequest)(implicit ctx: ChannelHandlerContext): Unit = {
      val uri = "ws://" + request.headers().get(HOST) + path // TODO ws | wss ?
      // TODO Support subprotocol
      handshaker = new WebSocketServerHandshakerFactory(uri, null, false).newHandshaker(request)
      if (handshaker == null)
        WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel())
      else
        handshaker.handshake(ctx.channel(), request)
    }

  }

  private object DecodeNotSuccess {
    def unapply(request: FullHttpRequest) = !request.getDecoderResult.isSuccess
  }

  private object UriIsNotPath {
    def unapply(request: FullHttpRequest) = request.getUri != path
  }

}
