package zhongl.websockit

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.websocketx._
import java.util.concurrent.atomic.AtomicBoolean

abstract class WebSoclet {
  private val closed = new AtomicBoolean(false)

  def h: WebSocketServerHandshaker

  def c: ChannelHandlerContext

  def receive: PartialFunction[WebSocketFrame, Unit] = {
    case f: PingWebSocketFrame  => c.writeAndFlush(new PongWebSocketFrame(f.content().retain()))
    case f: CloseWebSocketFrame => if (closed.get) c.close() else h.close(c.channel(), f.retain())
    case f: PongWebSocketFrame  =>
    case f                      => c.writeAndFlush(f.retain()) // echo frame
  }

  def close(reason: String = "Unknow reason.") = if (closed.compareAndSet(false, true)) {
    h.close(c.channel(), new CloseWebSocketFrame(1008, reason))
  }
}

object WebSoclet {
  def handshake(c: ChannelHandlerContext, r: FullHttpRequest) = {
    val uri = "ws://" + r.headers().get(HOST) + r.getUri // TODO ws | wss ?
    // TODO Support subprotocol
    val h = new WebSocketServerHandshakerFactory(uri, null, false).newHandshaker(r)
    if (h == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(c.channel())
      None
    } else {
      h.handshake(c.channel(), r)
      Some(h)
    }
  }
}

class Console(val c: ChannelHandlerContext, val h: WebSocketServerHandshaker) extends WebSoclet {
  c.writeAndFlush(new TextWebSocketFrame("WebSockit console is ready!"))

  def log(m: String) = c.writeAndFlush(new TextWebSocketFrame(m))
}

object Console {
  def apply(c: ChannelHandlerContext, r: FullHttpRequest) = WebSoclet.handshake(c, r) map { new Console(c, _) }
}
