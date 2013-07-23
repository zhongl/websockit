package zhongl.websockit

import com.jayway.jsonpath.JsonPath

class Stub {

  @volatile var in = ""

  @volatile var receive: PartialFunction[String, String] = { case s => s }

  implicit class JsonPathRead(sc: StringContext) extends AnyRef {
    def $[T](args: Any*) = () => JsonPath.compile('$' + sc.s(args: _*)).read[T](in)
  }

  implicit class JsonStringHelper(sc: StringContext) extends AnyRef {
    def json(args: Any*) = sc.s(args.collect {
      case f: Function0[_] => f()
      case x               => x
    }: _*)
  }

  type Filter = () => Boolean

  implicit class Read(f: () => _) {

    def :=[T](v: T) = () => f() == v

    def >(v: Long) = () => f().asInstanceOf[Long] > v

    def <(v: Long) = () => f().asInstanceOf[Long] < v

    def >(v: Int) = () => f().asInstanceOf[Int] > v

    def <(v: Int) = () => f().asInstanceOf[Int] < v

    def ~=(v: String) = () => f().asInstanceOf[String] matches v
  }

  implicit class Composable(f: Filter) {

    def ||(h: Filter): Filter = { () => f() || h() }

    def &&(h: Filter): Filter = { () => f() && h() }

    def >>(s: => String) = {
      val f: PartialFunction[String, String] = { case o @ Extract() => s }
      receive = f orElse receive
    }

    object Extract {
      def unapply(s: String) = { in = s; f() }
    }

  }

}
