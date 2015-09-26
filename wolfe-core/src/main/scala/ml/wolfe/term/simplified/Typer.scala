package ml.wolfe.term.simplified

import org.scalautils._

/**
 * @author riedel
 */
object Typer {

  def domain[T](domains: DomainBinding[Any]*)(term: STerm[T]): Dom[T] Or Every[ErrorMsg] =
    domain(Domains(domains:_*))(term)

  def domain[T](domains: Domains)(term: STerm[T]): Dom[T] Or Every[ErrorMsg] = term match {
    case v:Var[_] =>
      domains.get(v) match {
        case Some(d) => Good(d)
        case None => Bad(One(VariableNotBound(v)))
      }
    case Plus(a1, a2) =>
      for (d1 <- domain(domains)(a1);
           d2 <- domain(domains)(a2)) yield {
        (d1,d2) match {
          case (RangeDom(r1),RangeDom(r2)) => RangeDom(Range(r1.start + r2.start, r1.end + r2.end))
        }
      }
  }

  def main(args: Array[String]) {
    import Wolfe._
    val i = Var[Int]

    println(domain(i in RangeDom(0 until 3))(i + i))
  }

  implicit class TypeTerm[T](val term:STerm[T]) {
    def dom(bindings:DomainBinding[Any]*) = Typer.domain(Domains(bindings:_*))(term)
  }


}

class Domains {
  private var map: Map[Var[Any], Dom[Any]] = Map.empty

  def get[T](variable: Var[T]): Option[Dom[T]] = map.get(variable).asInstanceOf[Option[Dom[T]]]

  def update[T](variable: Var[T], dom: Dom[T]) = {
    map += (variable -> dom)
  }
}

case class DomainBinding[+T](variable: Var[T], dom: Dom[T])

object Domains {
  def apply(bindings: DomainBinding[Any]*) = {
    val r = new Domains
    r.map = bindings.map(b => b.variable -> b.dom).toMap
    r
  }
}