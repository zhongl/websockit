package zhongl

import com.jayway.jsonpath.JsonPath

package object websockit {

  implicit class JsonPathHelper(val sc: StringContext) extends AnyRef {
    def $(args: Any*) = JsonPath.compile('$' + sc.s(args: _*))
  }

  implicit class JsonStringHelper(val sc: StringContext) extends AnyRef {
    def json(arg: Any*) = sc.s(arg: _*)
  }

  type Filter = (String => Boolean)

  implicit class Read(p: JsonPath) {

    def :=[T](v: T): Filter = p.read[T](_: String) == v

    def >(v: Long): Filter = p.read[Long](_: String) > v

    def <(v: Long): Filter = p.read[Long](_: String) < v

    def >(v: Int): Filter = p.read[Int](_: String) > v

    def <(v: Int): Filter = p.read[Int](_: String) > v

    def ~=(v: String): Filter = p.read[String](_: String) matches v
  }

  implicit class Composable(f: Filter) {
    def ||(h: Filter): Filter = { s: String => f(s) || h(s) }

    def &&(h: Filter): Filter = { s: String => f(s) && h(s) }

    def >>(s: String): PartialFunction[String, String] = {
      val E = new Extractor

      { case o @ E() => s }
    }

    class Extractor {
      def unapply(s: String) = f(s)
    }

  }

}
