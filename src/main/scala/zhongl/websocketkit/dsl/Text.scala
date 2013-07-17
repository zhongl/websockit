package zhongl.websocketkit.dsl

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.jayway.jsonpath.JsonPath

object Text {
  def unapply(t: TextWebSocketFrame) = try {
    Some(new JsonQuery(t.retain().text))
  } catch {
    case _: Throwable => None
  }

  def apply(text: String) = Some(new TextWebSocketFrame(text))

  class JsonQuery(json: String) {
    def ?[T](path: String) = JsonPath.read[T](json, path)
  }

}
