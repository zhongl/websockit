package zhongl.websockit

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class StubDSLSpec extends FunSpec with ShouldMatchers {
  describe("Stub DSL") {
    it("should create stub with customized rules") {
      val s = new Stub {
        ($".name" := "jason") >> json"2"
        ($".name" := "allen") >> json"${$".seq"}"
      }

      s.receive("""{"name":"allen", "seq":1}""") should be("1")
      s.receive("""{"name":"jason"}""") should be("2")
    }
  }
}
