package zhongl.websockit

import io.netty.handler.codec.http.websocketx.{ TextWebSocketFrame, WebSocketFrame }
import java.net.URI
import java.util.concurrent.{ TimeUnit, SynchronousQueue }
import zhongl.websockit.driver.WebSocketClient
import zhongl.websockit.stub.WebSocketServer

object Stub {
  type Handle = PartialFunction[WebSocketFrame, Option[WebSocketFrame]]
  type Drive = (String => String)

  def act(h: Handle) = new Procedure(h)

  class Procedure(h: Handle) {

    def andThen(f: Drive => Unit) = {
      val server = new WebSocketServer() {
        def receive = h
      }

      val client = new WebSocketClient(new URI("ws://localhost:12306")) {

        val queue = new SynchronousQueue[String]()

        def receive = {
          case t: TextWebSocketFrame => queue.put(t.text()); None
        }

        def ask(text: String) = {
          send(text)
          queue.poll(3, TimeUnit.SECONDS)
        }
      }

      try {
        server.start()
        client.connect()
        f(client.ask)
      } finally {
        client.disconnet()
        server.stop()
      }

    }

  }

}
