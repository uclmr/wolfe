package ml.wolfe.term

import ml.wolfe.Language
import org.scalautils.Accumulation._
import org.scalautils._

/**
 * @author riedel
 */
object Typer {

  case class TypedTerm[T](term: Term[T], dom: Dom[T])


  def domain[T](domains: DomainBinding[Any]*)(term: Term[T]): Dom[T] Or Every[ErrorMsg] =
    domain(Domains(domains: _*))(term)

  def domain[T](domains: Domains)(term: Term[T]): Dom[T] Or Every[ErrorMsg] = term match {
    case v: Var[_] =>
      domains.get(v) match {
        case Some(d) => Good(d)
        case None => Bad(One(VariableNotBound(v)))
      }

    case SeqApply(s, _) =>
      for (d <- domain(domains)(s);
           SeqDom(e, _, _) = d) yield e

    case SeqAppend(s, _) =>
      for (d <- domain(domains)(s);
           SeqDom(e, min, max) = d) yield SeqDom(e, min + 1, max + 1)

    case ConstructProduct(args, constructor) =>
      for (argDoms <- args.map(domain(domains)).combined) yield ProductDom(argDoms, constructor)

    case GetElement(product, index) =>
      for (dom <- domain(domains)(product);
           ProductDom(doms, _) = dom) yield doms(index).asInstanceOf[Dom[T]]

    case SeqSlice(s, from, to) =>
      for (dom <- domain(domains)(s);
           SeqDom(e, min, max) = dom) yield SeqDom(e, min, max) //todo can be tighter

    case Plus(a1, a2) =>
      for (d1 <- domain(domains)(a1);
           d2 <- domain(domains)(a2)) yield {
        (d1, d2) match {
          case (RangeDom(r1), RangeDom(r2)) => RangeDom(Range(r1.start + r2.start, r1.end + r2.end))
        }
      }
  }

  def typedTerm[T](domains: Domains)(term: Term[T]): TypedTerm[T] Or Every[ErrorMsg] = term match {
    case v: Var[_] =>
      domains.get(v) match {
        case Some(d) => Good(TypedTerm(v, d))
        case None => Bad(One(VariableNotBound(v)))
      }

    case x@SeqApply(s, _) =>
      for (d <- typedTerm(domains)(s);
           TypedTerm(t, SeqDom(e, _, _)) = d) yield TypedTerm(x, e)

    case ConstructProduct(args, constructor) =>
      for (argDoms <- args.map(typedTerm(domains)).combined) yield TypedTerm(term, ProductDom(argDoms.map(_.dom), constructor))

    case GetElement(product, index) =>
      for (dom <- typedTerm(domains)(product);
           TypedTerm(prod, ProductDom(doms, _)) = dom) yield TypedTerm(term, doms(index).asInstanceOf[Dom[T]])

    case Plus(a1, a2) =>
      for (d1 <- typedTerm(domains)(a1);
           d2 <- typedTerm(domains)(a2)) yield {
        (d1, d2) match {
          case (TypedTerm(_, RangeDom(r1)), TypedTerm(_, RangeDom(r2))) =>
            TypedTerm(term, RangeDom(Range(r1.start + r2.start, r1.end + r2.end)))
        }
      }
  }


  def main(args: Array[String]) {

    import Language._

    val i = Var[Int]("i")

    println(domain(i in RangeDom(0 until 3))(i + i))
  }

  implicit class TypeTerm[T](val term: Term[T]) {
    def dom(bindings: DomainBinding[Any]*) = Typer.domain(Domains(bindings: _*))(term)

    def dom(implicit domains: Domains) = Typer.domain(domains)(term)

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