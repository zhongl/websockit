package zhongl.websockit

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class StubSpec extends FunSpec with ShouldMatchers {
  describe("Stub DSL") {
    it("should create stub with customized rules") {
      val s = new Stub {
        ($".name" =~ "jason") >> json"2"
        ($".name" =~ "allen") >> json"${$".seq"}"
      }

      s.receive("""{"name":"allen", "seq":1}""") should be(Some("1"))
      s.receive("""{"name":"jason"}""") should be(Some("2"))
    }

    it("should complain non-json-object") {
      val s = new Stub {
        ($".name" =~ "jason") >> json"2"
      }

      (evaluating { s.receive("""1""") } should produce[IllegalArgumentException])
        .getMessage should be("""1 is not a json object""")
    }

    it("should get array element") {
      new Stub {
        ($".[0]" =~ 1) >> json"2"
      } receive "[1]" should be(Some("2"))
    }

    it("should support regex") {
      new Stub {
        ($".name" =* """\w+""") >> json"""{"name":"${$".name"}"}"""
      } receive """{"name":"allen"}""" should be(Some("""{"name":"allen"}"""))
    }

    it("should support composed filters") {
      val s = new Stub {
        ($".name" =~ "allen" || $".age" > 25) >> json"ok"
      }

      s.receive("""{"name":"allen"}""") should be(Some("ok"))
      s.receive("""{"age":30}""") should be(Some("ok"))
    }

    it("should echo input") {
      new Stub {
        (() => true) >> $
      } receive "hi" should be(Some("hi"))
    }

    it("should do nothing") {
      new Stub {
        (() => true) >> nil
        (() => true) >> $
      } receive "hi" should be(None)
    }

    it("should miss match instead of complain invalid path") {
      new Stub {
        ($".typo" =~ 1) >> json"ok"
      } receive """{"type":1}""" should be(None)
    }
  }
}
