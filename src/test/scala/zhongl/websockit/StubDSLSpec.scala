package zhongl.websockit

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class StubDSLSpec extends FunSpec with ShouldMatchers {
  describe("Stub DSL") {
    it("should eval to Filter") {
      ($".name" := "allen")("""{"name":"allen"}""") should be(true)

      $".age" > 26 && $".age" < 50 apply """{"age":16}""" should be(false)
    }

    it("should eval to stub") {
      ($".name" := "allen") >> json"result" apply """{"name":"allen"}""" should be("result")
    }
  }
}
