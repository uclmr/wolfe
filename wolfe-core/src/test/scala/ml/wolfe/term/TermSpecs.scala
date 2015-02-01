package ml.wolfe.term

import ml.wolfe.WolfeSpec

/**
 * @author riedel
 */
class TermSpecs extends WolfeSpec {

  import ml.wolfe.term.TermImplicits._
  import ml.wolfe.util.Math._

  "An vector variable term" should {
    "evaluate to a vector" in {
      val x = vectors(2).variable("x")
      val result = x(vector(1.0, 2.0))
      result should equal(vector(1.0, 2.0))
    }

    "provide its constant gradient" in {
      val x = vectors(2).variable("x")
      val result = x.gradient(x, vector(2.0, 1.0))
      result should equal(vector(1.0, 1.0))
    }
  }

  "A double variable term" should {
    "provide its argmax" in {
      val x = doubles.variable("x")
      val y = doubles.variable("y")
      x.argmax(x) should be(Double.PositiveInfinity)
      x.argmax(y, 2.0) should be(2.0)
    }
  }

  "A Tuple2Var term" should {
    "evaluate to a tuple2" in {
      val dom = doubles x doubles
      val x = dom.variable("x")
      val result = x((1.0, 2.0))
      result should be(1.0, 2.0)
    }

    "provide its first argument" in {
      val dom = doubles x doubles
      val x = dom.variable("x")
      val arg1 = x._1
      arg1((2.0, 1.0)) should be(2.0)
    }
  }

  "A dot product term" should {
    "evaluate to the value of a dot product" in {
      val x = vectors(2).variable("x")
      val dot = x dot x
      val result = dot(vector(2.0, 3.0))
      result should be(13.0)
    }

    "provide its gradient for identical variables" in {
      val x = vectors(2).variable("x")
      val dot = x dot x
      val result = dot.gradient(x, vector(2.0, 3.0))
      result should equal(vector(4.0, 6.0))
    }

    "provide its gradient for different variables " in {
      val x = vectors(2).variable("x")
      val y = vectors(2).variable("y")
      val dot = x dot y
      dot.gradient(x, vector(2.0, 3.0), vector(1.0, 2.0)) should equal(vector(1.0, 2.0))
      dot.gradient(y, vector(2.0, 3.0), vector(1.0, 2.0)) should equal(vector(2.0, 3.0))
    }
  }

  "A sum" should {
    "evaluate to the sum of its arguments" in {
      val x = doubles.variable("x")
      val y = doubles.variable("y")
      val term = x + y + x
      term(1.0, 2.0) should be(4.0)
    }

    "calculate its gradient" in {
      val x = doubles.variable("x")
      val y = doubles.variable("y")
      val term = x + y + x
      term.gradient(x, 10.0, 5.0) should be(2.0)
      term.gradient(y, 10.0, 5.0) should be(1.0)
    }
  }

  "A product" should {
    "evaluate to the product of its arguments" in {
      val x = doubles.variable("x")
      val y = doubles.variable("y")
      val term = x * y * x * 0.5
      term(2.0, 3.0) should be(6.0)
    }
    "calculate its gradient" in {
      val x = doubles.variable("x")
      val y = doubles.variable("y")
      val term = x * y * x * 0.5
      term.gradient(x, 2.0, 3.0) should be(6.0)
      term.gradient(y, 2.0, 3.0) should be(2.0)
    }
  }

  "An iverson bracket" should {
    "evaluate to 0 if a predicate is false, and 1 otherwise" in {
      val x = bools.variable("x")
      val term = I(x)
      term(false) should be (0.0)
      term(true) should be (1.0)
    }
  }

  "Composing log, sigmoid and dot prodcuts" should {
    "provide a logistic loss matrix factorization objective" in {
      val x = vectors(2).variable("x")
      val y = vectors(2).variable("y")
      val term = log(sigm(x dot y))
      term(vector(1.0, 2.0), vector(2.0, 3.0)) should equal(math.log(sigmoid(8.0)))
    }

    "provide the gradient of a logistic loss matrix factorization objective" in {
      val x = vectors(2).variable("x")
      val y = vectors(2).variable("y")
      val term = log(sigm(x dot y))
      val result = term.gradient(x, vector(1.0, 2.0), vector(2.0, 3.0))
      val prob = sigmoid(8.0)
      result should equal(vector(2.0, 3.0) * (1.0 - prob))
    }
  }

  "A term with discrete variables" should {
    "provide its argmax" in {
      val result = argmax(bools x bools) {x => I(x._1 && x._2)}
      result should be (true,true)
    }
    "provide a partial argmax" in {
      val x = bools.variable("x")
      val y = bools.variable("y")
      val term = I(x === y)
      term.argmax(x, true) should be (true)
      term.argmax(x, false) should be (false)
    }
  }

}