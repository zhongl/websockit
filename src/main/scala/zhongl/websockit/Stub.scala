package zhongl.websockit

import com.jayway.jsonpath.{ InvalidPathException, JsonPath }

class Stub {

  type Filter = () => Boolean
  type Receive = PartialFunction[String, Option[String]]

  protected val nil: Option[String] = None

  @volatile protected var in = ""
  @volatile protected var cases = List.empty[Receive]

  lazy val receive: Receive = {
    val f: Receive = { case s => nil }
    cases.reverse.foldRight(f) { (s, b) => s orElse b }
  }

  protected def $ = Some(in)

  implicit class JsonPathRead(sc: StringContext) extends AnyRef {
    def $[T](args: Any*) = () => {
      val expr = sc.s(args: _*)
      try {
        JsonPath.compile('$' + expr).read[T](in)
      } catch {
        case t: IllegalArgumentException => throw new IllegalArgumentException(s"$in is not a json object")
      }
    }
  }

  implicit class JsonStringHelper(sc: StringContext) extends AnyRef {
    def json(args: Any*) = Some(sc.s(args.collect {
      case f: Function0[_] => f()
      case x               => x
    }: _*))
  }

  implicit class Read(f: () => _) {

    def =~[T](v: T) = silent { f() == v }

    def >(v: Long) = silent { f().asInstanceOf[Long] > v }

    def <(v: Long) = silent { f().asInstanceOf[Long] < v }

    def >(v: Int) = silent { f().asInstanceOf[Int] > v }

    def <(v: Int) = silent { f().asInstanceOf[Int] < v }

    def =*(v: String) = silent { f().asInstanceOf[String] matches v }

    private def silent(g: => Boolean) = () => try { g } catch { case _: InvalidPathException => false }
  }

  implicit class Composable(f: Filter) {

    def ||(h: Filter) = () => f() || h()

    def &&(h: Filter) = () => f() && h()

    def >>(s: => Option[String]) = {
      val f: Receive = { case o @ Extract() => s }
      cases = f :: cases
    }

    object Extract {
      def unapply(s: String) = { in = s; f() }
    }

  }

}
