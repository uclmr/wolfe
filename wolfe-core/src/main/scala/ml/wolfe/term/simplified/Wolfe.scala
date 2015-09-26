package ml.wolfe.term.simplified

import ml.wolfe.term.{NameProviderImplicits, NameProvider}

/**
 * @author riedel
 */
object Wolfe extends NameProviderImplicits with SeqHelper {

  def Variable[T](name:String) = new Var[T](name)
  def Var[T](implicit provider:NameProvider) = new Var[T](provider.newName())

  implicit def toConstant[T](value:T): Constant[T] = Constant(value)

  def sum[T,N](args:STerm[Seq[T]])(obj:STerm[T] => STerm[N])(implicit numeric:Numeric[N]) =
    Sum(SeqMap(args,obj))

  implicit class BindingCreator[T](variable:Var[T]) {
    def :=(value:T) = Binding(variable,value)
    def in(dom:Dom[T]) = DomainBinding(variable,dom)
  }

  implicit class IntTerm(i:STerm[Int]) {
    def until(to:STerm[Int]) = RangeTerm(i,to)
  }

  implicit class VarCreator[T](dom:Dom[T]) {
    def Variable(name:String)(implicit domains:Domains):Var[T] = {
      val result = new Var[T](name)
      domains(result) = dom
      result
    }
    def Var(implicit provider:NameProvider,domains:Domains) = Variable(provider.newName())
  }

  implicit class NumericTerm[N](n:STerm[N])(implicit val numeric:Numeric[N]) {
    def +(that:STerm[N]) = Plus(n,that)
    def -(that:STerm[N]) = Minus(n,that)
    def *(that:STerm[N]) = Times(n,that)
    def unary_- = Times(Constant(numeric.fromInt(-1)),n)

  }

  implicit class Tuple2Term[T1,T2](t:STerm[(T1, T2)]) {
    def _1 = GetElement[T1](t,0)
    def _2 = GetElement[T1](t,1)
  }
}

trait SeqHelper {

  def max(s1:STerm[Seq[Double]],s2:STerm[Seq[Double]]) = SeqPointWiseMax(s1,s2)
  def fill[E](length:STerm[Int])(elem:STerm[E]) = SeqFill(length,elem)

  implicit class SeqTerm[E](val s: STerm[Seq[E]]) {
    def apply(i: STerm[Int]) = SeqApply[E](s, i)
    def map[B](f:STerm[E] => STerm[B]) = SeqMap(s,f)
    def :+(that:STerm[E]) = SeqAppend(s,that)
    def length = SeqLength(s)
    def slice(from:STerm[Int],to:STerm[Int]) = SeqSlice(s,from,to)
  }

  implicit class NumericSeqTerm[E](val s: STerm[Seq[E]])(implicit numeric:Numeric[E]) {
    def sum = Sum(s)
  }

  implicit class DoubleSeqTerm(val s: STerm[Seq[Double]]) {
    def -(that:STerm[Seq[Double]]) = SeqMinus(s,that)
  }



}
