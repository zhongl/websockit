package zhongl.websockit

import java.util.concurrent.{ TimeoutException, TimeUnit, SynchronousQueue }
import io.netty.channel.{ ChannelInitializer, ChannelHandlerContext, SimpleChannelInboundHandler }
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http.HttpMethod._
import io.netty.channel.socket.SocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.NioSocketChannel
import java.nio.charset.Charset
import io.netty.buffer.Unpooled

case class Http(host: String, port: Int) {
  private val utf8 = Charset.forName("UTF-8")

  private lazy val queue = new SynchronousQueue[AnyRef]()

  def post(path: String, content: String): Unit = {
    val c = Unpooled.copiedBuffer(content, utf8)
    val request = new DefaultFullHttpRequest(HTTP_1_1, POST, path, c)
    HttpHeaders.setContentLength(request, c.readableBytes())
    sendAndWait(request)
  }

  def get(path: String) = sendAndWait(new DefaultFullHttpRequest(HTTP_1_1, GET, path))

  private def handler = new SimpleChannelInboundHandler[FullHttpResponse]() {
    def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse) {
      queue.put(msg.retain())
      ctx.close()
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
      queue.put(cause)
      ctx.close()
    }
  }

  private def initializer = new ChannelInitializer[SocketChannel] {
    def initChannel(ch: SocketChannel): Unit = ch.pipeline().addLast(
      new HttpClientCodec(), new HttpObjectAggregator(8194), handler
    )
  }

  private def sendAndWait(request: DefaultFullHttpRequest) = {
    val g = new NioEventLoopGroup(1)
    try {
      val channel = new Bootstrap()
        .group(g)
        .channel(classOf[NioSocketChannel])
        .handler(initializer)
        .connect(host, port).sync().channel()

      channel.writeAndFlush(request)
      val rst = queue.poll(3, TimeUnit.SECONDS) match {
        case t: Throwable        => throw t
        case r: FullHttpResponse => r.getStatus.code() -> r.content().retain().toString(utf8)
        case null                => throw new TimeoutException()
      }
      channel.closeFuture().sync()
      rst
    } finally {
      g.shutdownGracefully()
    }
  }
}
