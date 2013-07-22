package zhongl.websockit

import java.util.concurrent.{ TimeoutException, TimeUnit, SynchronousQueue }
import io.netty.channel.{ ChannelInitializer, ChannelHandlerContext, SimpleChannelInboundHandler }
import io.netty.handler.codec.http._
import io.netty.channel.socket.SocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.NioSocketChannel
import java.nio.charset.Charset

case class Http(host: String, port: Int) {

  private lazy val queue = new SynchronousQueue[AnyRef]()

  private lazy val handler = new SimpleChannelInboundHandler[FullHttpResponse]() {
    def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse) {
      queue.put(msg.retain())
      ctx.close()
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
      queue.put(cause)
      ctx.close()
    }
  }

  private lazy val initializer = new ChannelInitializer[SocketChannel] {
    def initChannel(ch: SocketChannel) {
      val pipeline = ch.pipeline()
      pipeline.addLast("http-codec", new HttpClientCodec())
      pipeline.addLast("aggregator", new HttpObjectAggregator(8194))
      pipeline.addLast("handler", handler)
    }
  }

  def get(path: String) = sendAndWait(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path))

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
        case r: FullHttpResponse => r.getStatus.code() -> r.content().retain().toString(Charset.forName("UTF-8"))
        case null                => throw new TimeoutException()
      }
      channel.closeFuture().sync()
      rst
    } finally {
      g.shutdownGracefully()
    }
  }
}
