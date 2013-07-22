package zhongl.websockit

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler, ChannelInitializer }
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpHeaders._
import org.slf4j.LoggerFactory
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import java.nio.charset.Charset
import io.netty.buffer.Unpooled
import scala.Some

class Server(port: Int) {
  private lazy val log = LoggerFactory.getLogger(classOf[Server])
  private lazy val boss = new NioEventLoopGroup()
  private lazy val worker = new NioEventLoopGroup()

  private val utf8 = Charset.forName("UTF-8")

  def run() = try {
    new ServerBootstrap()
      .group(boss, worker)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(initializer)
      .bind(port).sync()
    log.info(s"WebSockit serve at $port .")

    this
  } catch {
    case t: Throwable => shutdown; throw t
  }

  def shutdown = {
    boss.shutdownGracefully()
    worker.shutdownGracefully()
    this
  }

  private def initializer = new ChannelInitializer[SocketChannel] {
    def initChannel(ch: SocketChannel): Unit = ch.pipeline().addLast(
      new HttpServerCodec(), new HttpObjectAggregator(65536), new Handler()
    )
  }

  private class Handler extends SimpleChannelInboundHandler[AnyRef]() {

    private var websoclet: Option[WebSoclet] = None

    def channelRead0(c: ChannelHandlerContext, m: AnyRef): Unit = m match {
      case r: FullHttpRequest => handleHttpRequest(r, c)
      case f: WebSocketFrame  => websoclet map { _.receive }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
      log.error("close channel from " + ctx.channel().remoteAddress(), cause)
      // TODO log to console panel
      ctx.close()
    }

    private def handleWebSocketFrame(frame: WebSocketFrame, context: ChannelHandlerContext): Unit = {}

    private def handleHttpRequest(implicit r: FullHttpRequest, c: ChannelHandlerContext): Unit = r match {
      case Get(path)           => get(path)
      case Post(path, content) =>
      case _                   => throw new UnsupportedOperationException(s"${r.getMethod} ${r.getUri}")
    }

    private def get(path: String)(implicit c: ChannelHandlerContext, r: FullHttpRequest): Unit = path match {
      case "/stub"    => response(c, ok("text/plaint; charset=UTF-8", "(_ => true) => (s => s)"))
      case "/console" => websoclet = Console(c, r)
      case _          => response(c, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND))
    }

    private def ok(`type`: String, content: String) = {
      val r = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, utf8))
      r.headers().set(Names.CONTENT_TYPE, `type`)
      r
    }

    private def response(c: ChannelHandlerContext, r: FullHttpResponse): Unit = {
      c.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE)
    }

  }

  object Get {
    def unapply(r: FullHttpRequest) = if (r.getMethod == GET) Some(r.getUri) else None
  }

  object Post {
    def unapply(r: FullHttpRequest) = if (r.getMethod == POST) Some(r.getUri, r.content().toString(utf8)) else None
  }

}

