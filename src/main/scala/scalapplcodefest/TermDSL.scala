package scalapplcodefest

import scala.language.implicitConversions
import scalapplcodefest.value._
import scalapplcodefest.term._
import scalapplcodefest.value.RangeSet
import scala.Some
import scalapplcodefest.value.Reduce
import scalapplcodefest.term.RestrictedFun
import scalapplcodefest.term.DynFunTerm
import scalapplcodefest.value.SeqSet
import scalapplcodefest.term.FunApp

/**
 * This object provides a set of implicit conversions that allow users
 * to write down terms more compactly.
 *
 * @author Sebastian Riedel
 */
object TermDSL {

  implicit def intToConstant(x: Int) = Constant(x)
  implicit def intToTerm(x: Int) = RichIntTerm(x)
  implicit def doubleToConstant(x: Double) = Constant(x)
  implicit def booleanToConstant(x: Boolean) = Constant(x)
  implicit def symbolToConstant(x: Symbol) = Constant(x)
  //implicit def predicateToAllAtoms[A,B](p:Predicate[A,B]) = AllGroundAtoms(p)

  //implicit def setToConstant[T](x: Set[T]) = Constant(x)
  //implicit def setToRichSetTerm[T](x: Set[T]) = RichSetTerm(Constant(x))

  //implicit def funToConstant[A,B](x:Fun[A,B]) = x)
  implicit def toTupleTerm2[T1, T2](tuple: (Term[T1], Term[T2])) = TupleTerm2(tuple._1, tuple._2)
  implicit def toRichTupleTerm2[T1, T2](tuple: (Term[(T1, T2)])) = RichTupleTerm2(tuple)
  implicit def toRichVariable[T](v: Variable[T]) = RichVariable(v)
  implicit def toRichTerm[T](term: Term[T]) = new RichTerm(term)
  implicit def toRichInt[A](i: Term[Int]) = RichIntTerm(i)
  implicit def toRichDouble[A](t: Term[Double]) = RichDoubleTerm(t)
  implicit def toRichBooleanTerm(t: Term[Boolean]) = RichBooleanTerm(t)

  implicit def toRichFunTerm[A, B](term: Term[Fun[A, B]]): RichFunTerm[A, B] = RichFunTerm(term)
  implicit def toRichFunctionSeq[A, B](f: FunTerm[Seq[A], B]) = RichFunctionTermSeq(f)
  implicit def toRichFunction2[A1, A2, B](f: FunTerm[(A1, A2), B]) = RichFunctionTerm2(f)
  implicit def toRichPredicate[A, B](p: Predicate[A, B]) = RichPredicate(p)
  implicit def toRichPredicate2[A1, A2, B](p: Predicate[(A1, A2), B]) = RichPredicate2(p)
  implicit def toRichCartesianProductTerm2[T1, T2](term: CartesianProductTerm2[T1, T2]) = RichCartesianProductTerm2(term)
  implicit def toRichSetTerm[T](s: Term[Set[T]]) = RichSetTerm(s)
  implicit def toRichVec(term: Term[Vector]) = RichVecTerm(term)
  implicit def toRichVarSymbol(symbol: Symbol) = RichVarSymbol(symbol)
  implicit def toRichPredSymbol(symbol: Symbol) = RichPredSymbol(symbol)

  implicit def toRichIndex(index: Index) = RichIndex(index)

  case class Assign[T](variable: Variable[T], value: T)

  implicit def toAssign[T](pair: (Variable[T], T)) = Assign(pair._1, pair._2)


  def state(assignments: Assign[_]*) =
    if (assignments.isEmpty) State.empty else State(assignments.map(a => a.variable -> a.value).toMap)

  def funTerm[A, B](f: PartialFunction[A, B]) = Constant(Fun(f, new AllOfType[A], new AllOfType[B]))

  //create bracketed terms
  def br[T](term: Term[T]) = Bracketed(term)

  //table building
  def T[A, B](domain: Term[Set[A]], f: PartialFunction[A, B]) = Constant(Fun.table(domain.eval().get, f))
  def C[T1, T2](a1: Term[Set[T1]], a2: Term[Set[T2]]) = RichCartesianProductTerm2(CartesianProductTerm2(a1, a2))

  //math
  def unit(index: Term[Int], value: Term[Double] = Constant(1.0)) = FunApp(vectors.unit, TupleTerm2(index, value))
  def I(term: Term[Boolean]) = FunApp(bools.iverson, term)
  def log(term: Term[Double]) = FunApp(doubles.log, term)


  def dsum(args: Term[Seq[Double]]) = Reduce(doubles.add, args)
  def dsum(args: Term[Double]*) = Reduce(doubles.add, SeqTerm(args))
  def vsum(args: Term[Seq[Vector]]) = Reduce(vectors.add, args)
  def vsum(args: Term[Vector]*) = Reduce(vectors.add, SeqTerm(args))


  implicit def toImageSeq[A, B](f: FunTerm[A, B]) = ImageSeq1(f)
  implicit def toImageSeqCurried2[A1, A2, B](f: FunTerm[A1, Fun[A2, B]]) = ImageSeq2(f)


  implicit def uncurry[A1, A2, R](f: FunTerm[A1, Fun[A2, R]]) = f match {
    case Curried2(uncurried) => uncurried
    case _ => ???
  }

  case class RichVarSymbol(symbol: Symbol) {
    def of[T](set: Term[Set[T]]) = Var(symbol, set)
    //def of[T](set: Set[T]) = Var(symbol, Constant(set))
  }

  case class RichState(state: State) {
    def +[T](pair: (Variable[T], T)) = state + SingletonState(pair._1, pair._2)
  }

  case class RichPredSymbol(symbol: Symbol) {
    def of[A, B](domRange: (Term[Set[A]], Term[Set[B]])) = Predicate(symbol, domRange._1, domRange._2)
  }

  case class RichVariable[T](v: Variable[T]) {
    //def ->(value:T) = VarValuePair(v,value)
  }

  case class RichVecTerm(term: Term[Vector]) {
    def dot(that: Term[Vector]) = FunApp(vectors.dot, TupleTerm2(term, that))
    def +(that: Term[Vector]) = FunApp(vectors.add, TupleTerm2(term, that))
    def -(that: Term[Vector]) = FunApp(vectors.minus, TupleTerm2(term, that))
  }

  private var anonVarCount = 0
  def freshName() = {
    this.synchronized {
      anonVarCount += 1
    }
    "_x" + anonVarCount
  }

  case class RichSetTerm[T](s: Term[Set[T]], variableName: () => String = () => freshName()) {

    def freshVariable[A](dom: Term[Set[A]] = s) = Var(Symbol(variableName()), dom)

    def as(name: String) = RichSetTerm(s, () => name)

    def map[R](f: Variable[T] => Term[R]): LambdaAbstraction[T, R] = {
      val variable = freshVariable()
      LambdaAbstraction(variable, f(variable))
    }
    def flatMap[A1, A2](f: Variable[T] => LambdaAbstraction[A1, A2]) = {
      val variable: Variable[T] = freshVariable()
      val innerLambda = f(variable)
      LambdaAbstraction(variable, innerLambda)
    }


    def |->[T2](that: Term[Set[T2]]) = (s, that)

    def mappedBy[A](f: FunTerm[T, A]) =
      FunApp(RestrictedFun(MapIterable, CartesianProductTerm2(s.domain, f.domain), Constant(new AllOfType[Set[A]])), (s, f))

    def collectedBy[A](f: FunTerm[T, A]) =
      FunApp(RestrictedFun(CollectIterable, CartesianProductTerm2(s.domain, f.domain), Constant(new AllOfType[Set[A]])), (s, f))

    def filteredBy[A](f: FunTerm[T, Boolean]) =
      FunApp(RestrictedFun(FilterIterable, CartesianProductTerm2(s.domain, f.domain), Constant(new AllOfType[Set[T]])), (s, f))

  }

  case class RichCartesianProductTerm2[T1, T2](term: CartesianProductTerm2[T1, T2],
                                               variableName1: () => String = () => freshName(),
                                               variableName2: () => String = () => freshName()) {
    def as(name1: String, name2: String) = RichCartesianProductTerm2(term, () => name1, () => name2)

    def map[R](f: ((Variable[T1], Variable[T2])) => Term[R]): LambdaAbstraction[(T1, T2), R] = {
      val variable1 = Var(Symbol(variableName1()), term.a1)
      val variable2 = Var(Symbol(variableName2()), term.a2)
      val applied = f(variable1, variable2)
      //todo: replace variables with arg1 && arg2 of tuple
      val tupleVar = Var(Symbol(s"${variable1.name}_${variable2.name}"), term)
      val arg1 = FunApp(ArgTerm(term, term.a1, 0), tupleVar)
      val arg2 = FunApp(ArgTerm(term, term.a1, 1), tupleVar)
      val substituted1 = TermConverter.substituteTerm(applied, variable1, arg1)
      val substituted2 = TermConverter.substituteTerm(substituted1, variable2, arg2)
      LambdaAbstraction(tupleVar, substituted2)
    }

    def filter(f: ((Variable[T1], Variable[T2])) => Boolean) = this
    def withFilter(f: ((Variable[T1], Variable[T2])) => Boolean) = this

  }


  case class VarValuePair[T](variable: Variable[T], value: T)

  class RichTerm[T](term: Term[T]) {
    def |(condition: State) = Conditioned(term, condition)
    def |(mappings: (Variable[Any], Any)*) = Conditioned(term, State(mappings.toMap))
    def eval(state: (Variable[Any], Any)*) = term.eval(State(state.toMap))
    def value(state: (Variable[Any], Any)*) = term.value(State(state.toMap))
    def ===(that: Term[T]) = FunApp(RestrictedFun[(T,T),Boolean](Equal), TupleTerm2(term, that))//FunApp(new Equals[T].Term, TupleTerm2(term, that))
    def ===(that: T) = FunApp(RestrictedFun[(T,T),Boolean](Equal), TupleTerm2(term, Constant(that))) //FunApp(new Equals[T].Term, TupleTerm2(term, Constant(that)))
    //    def eval(state:VarValuePair[T]*):Option[T] = term.eval(State(state.map(_.toTuple).toMap))

  }

  case class RichTupleTerm2[T1, T2](t: Term[(T1, T2)]) {
    def _1 = ArgTerm(t.domain, Constant(new AllOfType[T2]), Constant(0))
    def _2 = ArgTerm(t.domain, Constant(new AllOfType[T2]), Constant(1))
  }

  case class RichIntTerm(i: Term[Int]) {
    def +(that: Term[Int]) = FunApp(ints.add, TupleTerm2(i, that))
    def -(that: Term[Int]) = FunApp(ints.minus, TupleTerm2(i, that))
    def /(that: Term[Int]) = FunApp(ints.divide, TupleTerm2(i, that))

    def ~~(that: Term[Int]) = RangeSet(i, that)
  }

  case class RichDoubleTerm(x: Term[Double]) {
    def +(that: Term[Double]) = FunApp(doubles.add, TupleTerm2(x, that))
    def -(that: Term[Double]) = FunApp(doubles.minus, TupleTerm2(x, that))
    def *(that: Term[Double]) = FunApp(doubles.times, TupleTerm2(x, that))
  }

  case class RichBooleanTerm(x: Term[Boolean]) {
    def &&(that: Term[Boolean]) = FunApp(bools.and, TupleTerm2(x, that))
    def ||(that: Term[Boolean]) = FunApp(bools.or, TupleTerm2(x, that))
    def |=>(that: Term[Boolean]) = FunApp(bools.implies, TupleTerm2(x, that))
    def <=>(that: Term[Boolean]) = FunApp(bools.equiv, TupleTerm2(x, that))
    def unary_! = FunApp(bools.neg, x)
    def unary_$ = FunApp(bools.iverson, x)
  }


  case class RichFunTerm[A, B](f: Term[Fun[A, B]]) {
    val FunTerm(funCandidateDom, _) = f
    def apply(a: Term[A]) = FunApp(f, a)
    def isDefined = for (x <- funCandidateDom) yield
      FunApp(RestrictedFun(IsDefined, CartesianProductTerm2(f.domain, funCandidateDom), Constant(Bools)), TupleTerm2(f, x))

  }

  case class RichFunctionTerm2[A1, A2, B](f: FunTerm[(A1, A2), B]) {
    def apply(a1: Term[A1], a2: Term[A2]) = FunApp(f, TupleTerm2(a1, a2))
  }

  case class RichFunctionTermSeq[A, B](f: FunTerm[Seq[A], B]) {
    def apply[C](args: Term[A]*)(implicit convert: C => Term[A]) = FunApp(f, SeqTerm(args.toSeq))
  }


  case class RichPredicate[A, B](p: Predicate[A, B]) {
    def atom(a: A) = GroundAtom(p, a)
    def allAtoms = AllGroundAtoms(p)
  }

  case class RichPredicate2[A1, A2, B](p: Predicate[(A1, A2), B]) {
    def atom(a1: A1, a2: A2) = GroundAtom(p, (a1, a2))
  }

  case class RichIndex(index: Index) {
    def apply(key: Symbol) = RichIndexFunction(index, key)
  }

  case class RichIndexFunction(index: Index, symbol: Symbol) {
    def apply[A1 <: AnyRef](a1: Term[A1]) =
      dynFun[A1, Int]({case x => index.index(Array(symbol, x))}, a1.domain, ints)(a1)
    def apply[A1 <: AnyRef, A2 <: AnyRef](a1: Term[A1], a2: Term[A2]) =
      dynFun[(A1, A2), Int]({case (x1, x2) => index.index(Array(symbol, x1, x2))}, c(a1.domain,a2.domain), ints)(a1, a2)
  }

//}

  trait ConstantValue[T] extends Term[T] {
    def unapply(term: Term[Any]): Boolean = term == this
  }

  trait ConstantFun1[A, B] extends ConstantValue[Fun[A, B]]  {
    self =>

    object Applied1 {
      def unapply(term: Term[Any]): Option[Term[A]] = term match {
        case FunApp(op, arg) if op eq self => Some(arg.asInstanceOf[Term[A]])
        case _ => None
      }
    }

  }

  trait ConstantFun2[A1, A2, B] extends ConstantFun1[(A1, A2), B]  {
    self =>

    object Applied2 {
      def unapply(term: Term[Any]): Option[(Term[A1], Term[A2])] = term match {
        case FunApp(op, TupleTerm2(arg1, arg2)) if op == self => Some(arg1.asInstanceOf[Term[A1]], arg2.asInstanceOf[Term[A2]])
        case _ => None
      }
    }

  }

  trait ConstantOperator[T] extends ConstantFun2[T,T,T] {
    self =>
    object Reduced {
      def unapply(x: Term[Any]): Option[Term[Seq[T]]] = x match {
        case Reduce(op, args) if op == self => Some(args.asInstanceOf[Term[Seq[T]]])
        case _ => None
      }
    }
    def reduce(args:Term[Seq[T]]) = Reduce(self,args)

  }


  trait ConstantSet[T] extends ConstantValue[Set[T]] {
    this: Term[Set[T]] =>
    val equal = RestrictedFun(Equal, CartesianProductTerm2(this, this), bools)

  }

  object ints extends Constant(Ints) with ConstantSet[Int] {
    val add = new Constant(Ints.Add) with ConstantOperator[Int]
    val minus = new Constant(Ints.Minus) with ConstantOperator[Int]
    val range = new Constant(Ints.Range) with ConstantFun2[Int,Int,Set[Int]]
    val divide = new Constant(Ints.Divide) with ConstantOperator[Int]

  }

  object doubles extends Constant(Doubles) with ConstantSet[Double] {
    val add = new Constant(Doubles.Add) with ConstantOperator[Double]
    val minus = new Constant(Doubles.Minus) with ConstantOperator[Double]
    val times = new Constant(Doubles.Times) with ConstantOperator[Double]
    val log = new Constant(Doubles.Log) with ConstantFun1[Double,Double]
  }

  object bools extends Constant(Bools) with ConstantSet[Boolean] {
    val and = new Constant(Bools.And) with ConstantOperator[Boolean]
    val or = new Constant(Bools.Or) with ConstantOperator[Boolean]
    val implies = new Constant(Bools.Implies) with ConstantOperator[Boolean]
    val equiv = new Constant(Bools.Equiv) with ConstantOperator[Boolean]
    val neg = new Constant(Bools.Neg) with ConstantFun1[Boolean,Boolean]
    val iverson = new Constant(Bools.Iverson) with ConstantFun1[Boolean,Double]

  }

  object vectors extends Constant(Vectors) with ConstantSet[Vector] {
    val dot = new Constant(Vectors.Dot) with ConstantFun2[Vector, Vector, Double]
    val add = new Constant(Vectors.VecAdd) with ConstantOperator[Vector]
    val minus = new Constant(Vectors.VecMinus) with ConstantOperator[Vector]
    val unit = new Constant(Vectors.UnitVector) with ConstantFun2[Int,Double,Vector]

  }

  object strings extends Constant(Strings) with ConstantSet[String] {
    val length = fun[String, Int]({case x => x.length})
  }

  //val all = new Constant(All) with ConstantSet[Any] {}

  def c[T1,T2](arg1:Term[Set[T1]],arg2:Term[Set[T2]]) = CartesianProductTerm2(arg1,arg2)
  def all[T] = Constant(new AllOfType[T])
  def set[T](values: T*) = Constant(SeqSet(values))
  def fun[A, B](f: PartialFunction[A, B], dom: Set[A] = new AllOfType[A], range: Set[B] = new AllOfType[B]) = Constant(Fun(f, dom, range))
  def dynFun[A, B](f: PartialFunction[A, B], dom: Term[Set[A]] = all[Set[A]], range: Term[Set[B]] = all[Set[B]]) = DynFunTerm(f, dom, range)



}