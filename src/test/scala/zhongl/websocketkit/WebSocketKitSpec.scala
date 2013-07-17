package zhongl.websocketkit

import zhongl.websocketkit.dsl.Text
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class WebSocketKitSpec extends FunSpec with ShouldMatchers {

  describe("websocket-kit") {
    it("'s stub and driver should communicate with each other.") {
      Stub act { case f => Some(f.retain()) } andThen { drive => drive("hi") should be("hi") }
    }
    it("should support json query dsl.") {
      Stub act {
        case Text(json) if json ? "$.to" == "allen" => Text(s"""{"code":200, "uuid":${json ? "$.uuid"}}""")
      } andThen {
        _("""{"to":"allen", "uuid":1}""") should be("""{"code":200, "uuid":1}""")
      }
    }
  }

}



