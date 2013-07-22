package zhongl.websockit

import org.scalatest.{ BeforeAndAfterAll, FunSpec }
import org.scalatest.matchers.ShouldMatchers

class ServerSpec extends FunSpec with ShouldMatchers with BeforeAndAfterAll {

  describe("Server") {

    it("should get contexts indexs") {
      pending
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

