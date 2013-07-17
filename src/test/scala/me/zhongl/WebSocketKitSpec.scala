package me.zhongl

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import me.zhongl.driver.WebSocketClient
import java.net.URI
import me.zhongl.stub.WebSocketServer
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import java.util.concurrent.SynchronousQueue

class WebSocketKitSpec extends FunSpec with ShouldMatchers {

  describe("websocket-kit") {
    it("'s stub and driver should communicate with each other.") {
      val server = new WebSocketServer() {
        def receive = { case x => Some(x.retain()) }
      }

      val client = new WebSocketClient(new URI("ws://localhost:12306")) {

        val queue = new SynchronousQueue[String]()

        def receive = {
          case t: TextWebSocketFrame => queue.put(t.text()); None
        }

        def ask(text: String) = {
          send(text)
          queue.take()
        }
      }

      try {
        server.start()

        client.connect()

        client.ask("hi") should be("hi")
      } finally {
        client.disconnet()
        server.stop()
      }
    }
  }
}
