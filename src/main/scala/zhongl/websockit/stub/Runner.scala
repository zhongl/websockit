package zhongl.websockit.stub

import com.twitter.util.Eval
import java.io.{ File, FileWriter }

object Runner {

  val usage =
    """Usage:
      |   stub                - create config sample file.
      |   stub <config>       - run a server with config file
      |   stub <config> <dir> - run a server with config file, and output classes to dir
    """.stripMargin

  val quotes = "\"\"\""
  val jsonPath = "json ? \"$.to\" == \"allen\""
  val result = "\"code\":200, \"uuid\":${json ? \"$.uuid\"}"

  val sample =
    s"""
      |import zhongl.websockit.dsl._
      |import zhongl.websockit.stub._
      |
      |new WebSocketServer(port = 12306, path = "/ws") {
      |  def receive = {
      |    case Text(json) if $jsonPath => Text(s$quotes{$result}$quotes)
      |  }
      |}
      |
    """.stripMargin

  def main(args: Array[String]): Unit = args match {
    case Array(config)         => start(new Eval(None)[WebSocketServer](new File(config)))
    case Array(config, target) => start(new Eval(Some(new File(target)))[WebSocketServer](new File(config)))
    case Array()               => createConfigSample()
    case _ =>
      Console.err.println(usage); sys.exit(-1)
  }

  def start(server: WebSocketServer): Unit = {
    sys.addShutdownHook { server.stop() }
    server.start()
  }

  def createConfigSample(): Unit = {
    val writer = new FileWriter("server")
    try {
      writer.write(sample)
    } finally {
      writer.close()
    }
  }

}
