package scalapplcodefest.term

import org.scalautils.Good
import scalapplcodefest.value.AllOfType

/**
 * @author Sebastian Riedel
 */
trait StateTerm extends Term[State] {
  def domain[C >: State] = Constant(new AllOfType[C])
  def default = State.empty
}

object StateTerm {
  def apply(semantics:State => State, vars:Set[Variable[Any]]) = new StateTerm {
    def eval(state: State) = Good(semantics(state))
    def variables = vars
  }
}