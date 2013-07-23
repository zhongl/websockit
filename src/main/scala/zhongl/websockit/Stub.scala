package zhongl.websockit

import com.jayway.jsonpath.{ InvalidPathException, JsonPath }
import com.jayway.jsonpath

class Stub {

  @volatile protected var in = ""

  @volatile var receive: PartialFunction[String, String] = { case s => s }

  implicit class JsonPathRead(sc: StringContext) extends AnyRef {
    def $[T](args: Any*) = () => {
      val expr = sc.s(args: _*)
      try {
        JsonPath.compile('$' + expr).read[T](in)
      } catch {
        case t: InvalidPathException     => throw new jsonpath.InvalidPathException(s"""${t.getMessage}: "$expr" """)
        case t: IllegalArgumentException => throw new IllegalArgumentException(s"$in is not a json object")
      }
    }
  }

  implicit class JsonStringHelper(sc: StringContext) extends AnyRef {
    def json(args: Any*) = sc.s(args.collect {
      case f: Function0[_] => f()
      case x               => x
    }: _*)
  }

  type Filter = () => Boolean

  implicit class Read(f: () => _) {

    def =~[T](v: T) = () => f() == v

    def >(v: Long) = () => f().asInstanceOf[Long] > v

    def <(v: Long) = () => f().asInstanceOf[Long] < v

    def >(v: Int) = () => f().asInstanceOf[Int] > v

    def <(v: Int) = () => f().asInstanceOf[Int] < v

    def =*(v: String) = () => f().asInstanceOf[String] matches v
  }

  implicit class Composable(f: Filter) {

    def ||(h: Filter) = () => {
      val b = try { f() } catch { case t: Throwable => false }
      b || h()
    }

    def &&(h: Filter) = () => f() && h()

    def >>(s: => String) = {
      val f: PartialFunction[String, String] = { case o @ Extract() => s }
      receive = f orElse receive
    }

    object Extract {
      def unapply(s: String) = { in = s; f() }
    }

  }

}
