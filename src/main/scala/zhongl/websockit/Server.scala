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
import java.util.concurrent.atomic.AtomicReference

class Server(port: Int) {
  private lazy val log = LoggerFactory.getLogger(classOf[Server])
  private lazy val utf8 = Charset.forName("UTF-8")
  private lazy val boss = new NioEventLoopGroup()
  private lazy val worker = new NioEventLoopGroup()

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
      case r: FullHttpRequest => handleHttpRequest(c, r)
      case f: WebSocketFrame => try { websoclet map { _.receive(f) } } catch {
        case t: Throwable =>
          c.writeAndFlush(f.retain())
          error("Invalid WebSocket frame", t)
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
      log.error("close channel from " + ctx.channel().remoteAddress(), cause)
      // TODO log to console panel
      ctx.close()
    }

    private def handleHttpRequest(implicit c: ChannelHandlerContext, r: FullHttpRequest): Unit = r match {
      case Get(path)           => get(path)
      case Post(path, content) => post(path, content)
      case _                   => throw new UnsupportedOperationException(s"${r.getMethod} ${r.getUri}")
    }

    private def post(path: String, content: String)(implicit c: ChannelHandlerContext): Unit = path match {
      case "/stub"  => response(updateStub(content))
      case "/drive" =>
    }

    private def get(path: String)(implicit c: ChannelHandlerContext, r: FullHttpRequest): Unit = path match {
      case "/stub"      => response(ok("text/plaint; charset=UTF-8", "(_ => true) => (s => s)"))
      case "/console"   => websoclet = Console(c, r)
      case "/websocket" => websoclet = Session(c, r)
      case _            => response(new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND))
    }

    private def updateStub(content: String) = {

      info(s"Update stub \n$content\n")
      try {
        Session.update(content)
        new DefaultFullHttpResponse(HTTP_1_1, OK)
      } catch {
        case t: Throwable =>
          error("Failed to update stub, ", t)
          new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST)
      }
    }

    private def set[T <: WebSoclet](ref: AtomicReference[Option[T]], cur: T): T = {
      val pre = ref.getAndSet(Some(cur))
      pre map { _.close("Close by the other one connected.") }
      cur
    }

    private def ok(`type`: String, content: String) = {
      val r = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, utf8))
      r.headers().set(Names.CONTENT_TYPE, `type`)
      r
    }

    private def response(r: FullHttpResponse)(implicit c: ChannelHandlerContext): Unit = {
      c.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE)
    }

    private def info(m: String) = {
      Console.info(m)
      log.info(m)
    }

    private def error(m: String, t: Throwable) = {
      Console.error(t.getMessage)
      log.error(m, t)
    }

  }

  object Get {
    def unapply(r: FullHttpRequest) = if (r.getMethod == GET) Some(r.getUri) else None
  }

  object Post {
    def unapply(r: FullHttpRequest) = if (r.getMethod == POST) Some(r.getUri, r.content().toString(utf8)) else None
  }

}

object Server {
  def main(args: Array[String]): Unit = {
    val s = args match {
      case Array()     => new Server(12306).run()
      case Array(port) => new Server(port.toInt).run()
      case _           => throw new IllegalArgumentException("Usage: cmd [port]")
    }

    sys.addShutdownHook(s.shutdown)
  }
}
