package zhongl.websockit

import org.scalatest.{ BeforeAndAfterAll, FunSpec }
import org.scalatest.matchers.ShouldMatchers
import java.net.URI
import io.netty.handler.codec.http.websocketx.{ TextWebSocketFrame, CloseWebSocketFrame }
import java.util.concurrent.{ ArrayBlockingQueue, TimeUnit, SynchronousQueue }

class ServerSpec extends FunSpec with ShouldMatchers with BeforeAndAfterAll {

  describe("Server") {

    it("should open websockit console") {
      val queue = new SynchronousQueue[AnyRef]()
      Client.stub(new URI("ws://localhost:12306/console")) {
        case f: TextWebSocketFrame => queue.put(f.retain().text()); Some(new CloseWebSocketFrame())
        case f                     => queue.put(f.retain()); Some(new CloseWebSocketFrame())
      }
      queue.poll(3, TimeUnit.SECONDS) should be("WebSockit console is ready!")
    }

    it("should close console if open the other one") {
      val queue = new ArrayBlockingQueue[Class[_]](3)
      Client.stub(new URI("ws://localhost:12306/console")) {
        case f => queue.put(f.getClass); None
      }
      Client.stub(new URI("ws://localhost:12306/console")) {
        case f => queue.put(f.getClass); None
      }
      Thread.sleep(1000L)
      queue.contains(classOf[CloseWebSocketFrame]) should be(true)
    }

    it("should stub real server as customized") {
      Http("localhost", 12306).post("/stub", """($".k" =~ "v") >> json"ok" """)
      Client.drive(new URI("ws://localhost:12306/websocket"), """{"k":"v"}""") should be("ok")
    }

    it("should drive message to client") {
      val queue = new SynchronousQueue[AnyRef]()
      Client.stub(new URI("ws://localhost:12306/websocket")) {
        case f: TextWebSocketFrame => queue.put(f.retain().text()); Some(new CloseWebSocketFrame())
        case f                     => queue.put(f.retain()); Some(new CloseWebSocketFrame())
      }
      Thread.sleep(500L)
      Http("localhost", 12306).post("/drive", "hello")
      queue.poll(3, TimeUnit.SECONDS) should be("hello")
    }

  }

  var s: Server = _

  override protected def beforeAll() {
    s = new Server(12306).run()
  }

  override protected def afterAll() {
    s.shutdown
  }
}

