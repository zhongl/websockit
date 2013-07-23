package zhongl.websockit

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.websocketx._
import com.twitter.util.Eval
import java.util.concurrent.atomic.AtomicReference

abstract class WebSoclet {

  def h: WebSocketServerHandshaker

  def c: ChannelHandlerContext

  def receive: PartialFunction[WebSocketFrame, Unit] = {
    case f: PingWebSocketFrame  => c.writeAndFlush(new PongWebSocketFrame(f.content().retain()))
    case f: CloseWebSocketFrame => h.close(c.channel(), f.retain())
    case f: PongWebSocketFrame  =>
    case f                      => c.writeAndFlush(f.retain()) // echo frame
  }

  def close(reason: String = "Unknow reason.") = h.close(c.channel(), new CloseWebSocketFrame(1008, reason))
}

object WebSoclet {
  def handshake(c: ChannelHandlerContext, r: FullHttpRequest)(f: WebSocketServerHandshaker => WebSoclet) = {
    val uri = "ws://" + r.headers().get(HOST) + r.getUri // TODO ws | wss ?
    // TODO Support subprotocol
    val h = new WebSocketServerHandshakerFactory(uri, null, false).newHandshaker(r)
    if (h == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(c.channel())
      None
    } else {
      h.handshake(c.channel(), r)
      Some(f(h))
    }
  }
}

class Console(val c: ChannelHandlerContext, val h: WebSocketServerHandshaker) extends WebSoclet {

  c.writeAndFlush(new TextWebSocketFrame("WebSockit console is ready!"))

  def log(m: String) = c.writeAndFlush(new TextWebSocketFrame(m))
}

object Console {

  private val singleton = new AtomicReference[Option[Console]](None)

  def error(m: String) = singleton.get() foreach { _.log(s"ERROR: $m") }

  def info(m: String) = singleton.get() foreach { _.log(s"INFO : $m") }

  def apply(c: ChannelHandlerContext, r: FullHttpRequest) = WebSoclet.handshake(c, r) {
    h =>
      val cur = new Console(c, h)
      singleton.getAndSet(Some(cur)) foreach { _.close() }
      cur
  }
}

class Session(val c: ChannelHandlerContext,
              val h: WebSocketServerHandshaker,
              @volatile var stub: Stub) extends WebSoclet {

  override def receive = {
    case f: PingWebSocketFrame  => c.writeAndFlush(new PongWebSocketFrame(f.content().retain()))
    case f: CloseWebSocketFrame => h.close(c.channel(), f.retain())
    case f: PongWebSocketFrame  =>
    case f: TextWebSocketFrame  => c.writeAndFlush(new TextWebSocketFrame(handle(f.retain().text())))
  }

  private def handle(in: String) = {
    Console.info(s">>>\n$in")
    val out = stub.receive(in)
    Console.info(s"<<<\n$out")
    out
  }
}

object Session {
  private val defaultContent = {
    val tq = "\"\"\""
    val d = "$"
    s"""
      |// `=~` is equal, like: $d".key" =~ "value"
      |// `=*` is regex, like: $d".key" =* "v\\w+"
      |// `< or >` can use for number.
      |
      |// ($d".to" =~ "allen" || $d".seq" > 25) >> json$tq{"code":200, "seq":$d{ $d".seq" }}
      |
      |(() => true) >> in  // echo input
    """.stripMargin
  }

  private val eval = new Eval()

  private val singleton = new AtomicReference[Option[Session]](None)

  @volatile var content = defaultContent

  def update(stub: String) = {
    content = stub
    singleton.get() foreach { _.stub = doEval(stub) }
  }

  def apply(c: ChannelHandlerContext, r: FullHttpRequest) = WebSoclet.handshake(c, r) {
    h =>
      val s = new Session(c, h, doEval(content))
      singleton.getAndSet(Some(s)) foreach { _.close() }
      s
  }

  private def doEval(content: String): Stub = eval(s"""import zhongl.websockit.Stub
        |new Stub {
        |$content
        |}
      """.stripMargin)

}
