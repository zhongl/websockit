package zhongl.websockit

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import com.jayway.jsonpath.InvalidPathException

class StubDSLSpec extends FunSpec with ShouldMatchers {
  describe("Stub DSL") {
    it("should create stub with customized rules") {
      val s = new Stub {
        ($".name" =~ "jason") >> json"2"
        ($".name" =~ "allen") >> json"${$".seq"}"
      }

      s.receive("""{"name":"allen", "seq":1}""") should be("1")
      s.receive("""{"name":"jason"}""") should be("2")
    }

    it("should complain invalid path") {
      val s = new Stub {
        ($".name" =~ "jason") >> json"2"
      }

      val e = evaluating { s.receive("""{"age":1}""") } should produce[InvalidPathException]
      e.getMessage should be("""invalid path: ".name" """)
    }

    it("should complain non-json-object") {
      val s = new Stub {
        ($".name" =~ "jason") >> json"2"
      }

      val e = evaluating { s.receive("""1""") } should produce[IllegalArgumentException]
      e.getMessage should be("""1 is not a json object""")
    }

    it("should get array element") {
      val s = new Stub {
        ($".[0]" =~ 1) >> json"2"
      }

      s.receive("[1]") should be("2")
    }

    it("should support regex") {
      val s = new Stub {
        ($".name" =* """\w+""") >> json"""{"name":"${$".name"}"}"""
      }

      s.receive("""{"name":"allen"}""") should be("""{"name":"allen"}""")
    }

    it("should support composed filters") {
      val s = new Stub {
        ($".name" =* "allen" || $".age" > 25) >> json"ok"
      }

      s.receive("""{"name":"allen"}""") should be("ok")
      s.receive("""{"age":30}""") should be("ok")
    }
  }
}
