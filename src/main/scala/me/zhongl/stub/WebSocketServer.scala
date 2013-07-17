package me.zhongl.stub

abstract class WebSocketServer(port: Int = 12306, path: String = "/") {

  def receive: PartialFunction[AnyRef, AnyRef]

  def start(): Unit = {

  }

  def stop(): Unit = {

  }
}
