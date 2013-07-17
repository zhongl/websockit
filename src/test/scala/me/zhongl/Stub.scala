package me.zhongl

import io.netty.handler.codec.http.websocketx.{TextWebSocketFrame, WebSocketFrame}
import me.zhongl.stub.WebSocketServer
import me.zhongl.driver.WebSocketClient
import java.net.URI
import java.util.concurrent.SynchronousQueue

object Stub {
  type Handle = PartialFunction[WebSocketFrame, Option[WebSocketFrame]]
  type Drive = (String => String)

  def act(h: Handle) = new Procedure(h)

  class Procedure(h: Handle) {

    def then(f: Drive => Unit) = {
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
          queue.take()
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
