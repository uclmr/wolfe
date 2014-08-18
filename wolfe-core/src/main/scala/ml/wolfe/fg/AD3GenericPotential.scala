package ml.wolfe.fg

import breeze.linalg.{Transpose, DenseMatrix, DenseVector}
import ml.wolfe.MoreArrayOps._
import scalaxy.loops._

import scala.collection.mutable.ArrayBuffer

/**
 * Created by luke on 15/08/14.
 */
trait AD3GenericPotential extends DiscretePotential {
  def settings:Array[Array[Int]]
  /* MAP with N2F penalties */
  def computeMAP() : Array[Int]

  /* MAP of arbitrary function */
  def computeMAP(scoreFun : Int => Double) : Array[Int]

  def getScores: Array[Double] = getScoreTable.array
  private var scores:Array[Double] = null
  private var activeSet: ArrayBuffer[Int] = null
  private var solution: DenseVector[Double] = null
  private var Ainv : DenseMatrix[Double] = null // Inverse of [[M'M 1], [1' 0]], where M is indexed by activeSet
  private lazy val consistencyMatrices : Array[DenseMatrix[Double]] = {
    val Ms = vars.map(v => DenseMatrix.zeros[Double](v.dim, settings.length))
    for (i <- 0 until settings.length) {
      val setting = TablePotential.entryToSetting(i, dims)
      for (j <- 0 until vars.length) Ms(j)(setting(j), i) = 1
    }
    Ms
  }

  override def ad3Init(): Unit = {
    activeSet = ArrayBuffer(TablePotential.settingToEntry(computeMAP(), dims))
    solution = DenseVector.zeros[Double](settings.length)
    solution(activeSet(0)) = 1
    Ainv = DenseMatrix((0d, 1d), (1d, -vars.length.toDouble))
    scores = getScores
  }

  override def quadraticProgramF2N(stepSize:Double, maxIterations:Int) : Unit = {
    // todo: cache solution (see page 15)
    println(s"----------------\nquadraticProgramF2N($stepSize, $maxIterations)")
    // M'a
    val Mta = DenseVector.zeros[Double](settings.length)
    for (k <- (0 until vars.size).optimized) {
      for (i <- (0 until settings.length).optimized) {
        Mta(i) += vars(k).b(settings(i)(k))
        Mta(i) += msgs(k).n2f(settings(i)(k)) / stepSize
      }
    }

    /* Main Loop */
    var solved = false
    for(t <- 0 until maxIterations if !solved) {
      println(s"\titeration $t")
      println(s"activeSet = ${activeSet.mkString(",")}")
      println(s"Ainv =\n $Ainv")
      // ------------------- Solve the KKT system -----------------------------
      val rhsKKT = DenseVector.vertcat(Mta(activeSet).toDenseVector, DenseVector.ones[Double](1))
      for (i <- (0 until activeSet.size).optimized) {
        val entry = TablePotential.settingToEntry(settings(activeSet(i)), dims)
        rhsKKT(i) += scores(entry) / stepSize
      }
      println(s"KKT rhs = $rhsKKT")

      val vAndTau = Ainv * rhsKKT
      val vj:DenseVector[Double] = vAndTau(0 until vAndTau.length-1) // new solution, indexed by activeSet
      val tau = vAndTau(-1)
      println(s"vj = $vj")
      println(s"tau = $tau")
      //----------------------------------------------------------------------

      //M' * (M_J)
      val MtMj = DenseMatrix.tabulate[Double](settings.length, activeSet.length){ case(i, j) =>
        (0 until vars.length) count (k => settings(i)(k) == settings(j)(k)) }
      //M'w
      val Mtw = Mta.toDenseVector - MtMj * vj
      println("M'w = " + Mtw)

      if (vj == solution(activeSet)) {
        println("Same v as previous")
        val map = computeMAP(i => scores(i) + Mtw(i))
        val r = TablePotential.settingToEntry(map, dims)
        if (scores(r) + Mtw(r) <= tau + 1e-12) solved = true
        else addToActiveSet(r)

      } else {

        var alpha: Double = 1
        var blockingConstraint:Option[Int] = None
        for ((r,i) <- activeSet.zipWithIndex if solution(r) > vj(i)) {
          val x = solution(r) / (solution(r) - vj(i))
          if (x < alpha) {
            alpha = x
            blockingConstraint = Some(r)
          }
        }
        for ((r,i) <- activeSet.zipWithIndex) solution(r) = solution(r) * (1 - alpha) + vj(i) * alpha

        println(s"New Solution: $solution")
        blockingConstraint match {
          case Some(r:Int) => removeFromActiveSet(r)
          case _ => }

      }
    }

    for (j <- (0 until msgs.size).optimized)
      fill(msgs(j).f2n, 0)

    for(r <- activeSet) {
      val setting = settings(r)
      for (j <- (0 until msgs.size).optimized)
        msgs(j).f2n(setting(j)) += solution(r)
    }
  }


  //todo: WHAT IF SINGULAR?
  private def addToActiveSet(r:Int): Unit = {
    println(s"Adding $r to active set")
    /*   Update A⁻¹ = (MjMj')⁻¹ by blockwise inversion   */
    val B = DenseVector.tabulate[Double](activeSet.length + 1){ case(i) =>
      if(i < activeSet.length) (0 until vars.length) count { k => settings(r)(k) == settings(activeSet(i))(k) }
      else 1
    }

    val C = B.t
    val d = vars.length

    val P:Transpose[DenseVector[Double]] = C * Ainv  // CA⁻¹
    val Q:DenseVector[Double] = Ainv * B             // A⁻¹B
    val q:Double = 1d / (d - (P * B))                 // (D - CA⁻¹B)⁻¹
    val R:DenseVector[Double] = Q * q                 // A⁻¹B(D - CA⁻¹B)⁻¹

    val X1:DenseMatrix[Double] = Ainv + (R * P)      // A⁻¹ + A⁻¹B(D - CA⁻¹B)⁻¹CA⁻¹
    val X2:DenseVector[Double] = R * (-1d)             // -A⁻¹B(D - CA⁻¹B)⁻¹
    val X3:Transpose[DenseVector[Double]] = P * (-q)  // -CA⁻¹(D - CA⁻¹B)⁻¹


    Ainv = DenseMatrix.vertcat(
      DenseMatrix.horzcat(X1, X2.toDenseMatrix.t),
      DenseMatrix.horzcat(X3.t.toDenseMatrix, new DenseMatrix[Double](1, 1, Array(q)))
    )
    // At this point the matrix basis is out of order - we need to swap the final two dimensions
    // (corresponding to tau and v(r), respectively)
    val n = activeSet.length
    for(i <- (0 until n+2).optimized) { val x = Ainv(i, n+1); Ainv(i, n+1) = Ainv(i, n); Ainv(i, n) = x }
    for(i <- (0 until n+2).optimized) { val x = Ainv(n+1, i); Ainv(n+1, i) = Ainv(n, i); Ainv(n, i) = x }

    // Update the active set
    activeSet += r
  }

  private def removeFromActiveSet(r:Int): Unit = {
    println(s"Removing $r to active set")

    /*   Update A⁻¹ by http://math.stackexchange.com/questions/208001/are-there-any-decompositions-of-a-symmetric-matrix-that-allow-for-the-inversion/208021#208021*/
    val n = activeSet.length
    val s = activeSet.indexOf(r)
    for(i <- (0 until n+1).optimized) { val x = Ainv(i, n); Ainv(i, n) = Ainv(i, s); Ainv(i, s) = x }
    for(i <- (0 until n+1).optimized) { val x = Ainv(n, i); Ainv(n, i) = Ainv(s, i); Ainv(s, i) = x }

    val E = Ainv(0 until n, 0 until n)
    val f:DenseVector[Double] = Ainv(0 until n, n)
    val g:DenseVector[Double] = Ainv(n, 0 until n).t
    val h = Ainv(n, n)
    Ainv = E - (f * g.t) / h

    activeSet -= r
  }
}
