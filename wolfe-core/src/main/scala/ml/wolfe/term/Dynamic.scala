package ml.wolfe.term

/**
 * @author riedel
 */

trait Dynamic[+T] {

  self =>
  def value(): T

  def generators: List[DynamicGenerator[_]]

  def map[A](f: T => A): Dynamic[A] = new Dynamic[A] {
    def generators = self.generators

    private var _currentA: A = _
    generators.foreach(_.addListener { () =>
      _currentA = f(self.value())
    })

    def value() = {
      _currentA
    }

  }

  override def toString = s"Dynamic(${value()})"
}

trait Dynamic2[T] {
  parent =>
  protected var children: List[Dynamic2[_]] = Nil
  protected var owners: List[Dynamic2[_]] = Nil

  def ownerCount = owners.size

  def childCount = children.size

  def size = 1

  private def detach(): Unit = {
    for (owner <- owners) {
      owner.children = owner.children.filterNot(_ == parent)
    }
    owners = Nil
  }

  private def attach(child: Dynamic2[_]): Unit = {
    children ::= child
    child.owners ::= parent
  }

  protected def update()

  def updateValue(): Unit = {
    update()
    children.reverse foreach (_.updateValue())
  }

  def value(): T

  def map[A](f: T => A):Dynamic2[A] = new Dynamic2[A] {
    parent.attach(this)

    private var current: A = _
    private var needsUpdate = true

    protected def update() = {
      if (owners.nonEmpty) needsUpdate = true
    }

    def value() = {
      if (needsUpdate) {
        current = f(parent.value())
        needsUpdate = false
      }
      current
    }
  }

  def flatMap[A](f: T => Dynamic2[A]):Dynamic2[A] = new Dynamic2[A] {
    parent.attach(this)

    private var current: Dynamic2[A] = _
    private var needsUpdate = true

    protected def update() = {
      if (current != null) current.detach()
      needsUpdate = true
    }

    def value() = {
      if (needsUpdate) {
        current = f(parent.value())
        current.updateValue()
        needsUpdate = false
      }
      current.value()
    }

  }

}

object Dynamic2 {
  def sequential[T](seq: IndexedSeq[T]): Dynamic2[T] = new Dynamic2[T] {
    private var _current = -1

    protected def update() = {
      _current = (_current + 1) % seq.length
    }

    def value() = seq(_current)

    override def size = seq.size
  }

  def stochastic[T](seq: IndexedSeq[T]): Dynamic2[T] = new Dynamic2[T] {
    import ml.wolfe.util.Math.random
    private var _seq = random.shuffle(seq)
    private var _current = -1

    protected def update() = {
      if (_current == _seq.size - 1) {
        _seq = random.shuffle(seq)
        _current = -1
      }
      _current = _current + 1
    }

    def value() = seq(_current)

    override def size = seq.size
  }

  object unapply3 {
    def unapply[T1, T2, T3](d: Dynamic2[(T1, T2, T3)]) = {
      val t1 = d.map(_._1)
      val t2 = d.map(_._2)
      val t3 = d.map(_._3)
      Some((t1,t2,t3))
    }
  }


}

trait DynamicTerm[D <: DoubleDom, T] extends ProxyTerm[D] with NAry {

  dyn =>

  def generator: DynamicGenerator[T]

  override def evaluator() = new Evaluator {
    val eval = self.evaluator()

    def eval(inputs: Array[Setting], output: Setting) = {
      generator.generateNext()
      eval.eval(inputs, output)
    }
  }

  override def differentiator(wrt: Seq[Var[Dom]]) = new Differentiator {
    val diff = self.differentiator(wrt)

    def forwardProp(current: Array[Setting]) = {
      generator.generateNext()
      diff.forwardProp(current)
      activation := diff.activation
    }

    def term = diff.term

    def withRespectTo = diff.withRespectTo

    def backProp(error: Setting, gradient: Array[Setting]) = {
      diff.backProp(error, gradient)
    }
  }

  type ArgumentType = Term[DoubleDom]

  def arguments = IndexedSeq(self)

  def copy(args: IndexedSeq[ArgumentType]) = new DynamicTerm[D,T] {
    def generator = dyn.generator
    def self = dyn.self
  }
}

trait DynamicTerm2[D <: DoubleDom, T] extends ProxyTerm[D] {
  def generator: Dynamic2[T]

  override def evaluator() = new Evaluator {
    val eval = self.evaluator()

    def eval(inputs: Array[Setting], output: Setting) = {
      //generator.generateNext()
      generator.updateValue()
      eval.eval(inputs, output)
    }
  }

  override def differentiator(wrt: Seq[Var[Dom]]) = new Differentiator {
    val diff = self.differentiator(wrt)

    def forwardProp(current: Array[Setting]) = {
      //generator.generateNext()
      generator.updateValue()
      diff.forwardProp(current)
      activation := diff.activation
    }

    def term = diff.term

    def withRespectTo = diff.withRespectTo

    def backProp(error: Setting, gradient: Array[Setting]) = {
      diff.backProp(error, gradient)
    }
  }
}

trait DynamicGenerator[+T] {
  type Listener = () => Unit
  private var listeners: List[Listener] = Nil

  def updateValue()

  def generateNext(): Unit = {
    updateValue()
    //println("Updating listeners: " + listeners)
    for (l <- listeners) l()
  }

  def value: Dynamic[T]

  def addListener(listener: Listener): Unit = {
    //println("Adding listener " + listener)
    listeners ::= listener
  }

  def termsPerEpoch: Int = 1
}

trait Generator[+T] {
  def generateNext()

  def current(): T

  def termsPerEpoch: Int
}