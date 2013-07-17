package me.zhongl

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class WebSocketKitSpec extends FunSpec with ShouldMatchers {

  describe("websocket-kit") {
    it("'s stub and driver should communicate with each other.") {
      Stub act { case x => Some(x.retain()) } then { drive => drive("hi") should be("hi") }
    }
  }
}


