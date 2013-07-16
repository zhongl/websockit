package me.zhongl

import java.net.URI
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory

trait WebSocketClient {

  def uri: URI

  def headers = Map.empty[String, String]

  def connect(): Unit = {
    require(uri.getScheme == "ws", "Unsupported protocol:" + uri.getScheme)

//    new WebSocketClientHandler
  }

}

