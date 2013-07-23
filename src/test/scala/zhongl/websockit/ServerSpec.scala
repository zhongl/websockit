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

    it("should stub an echo server as default") {
      Client.drive(new URI("ws://localhost:12306/websocket"), "hi") should be("hi")
    }

    it("should stub a real stub server as customized") {
      Http("localhost", 12306).post("/stub", "(_ => true) => (s => hello)")
      Client.drive(new URI("ws://localhost:12306/websocket"), "hi") should be("hello")
    }

    it("should get stub definition") {
      val (_, content) = Http("localhost", 12306).get("/stub")
      content should be("(_ => true) => (s => s)")
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

