package ml.wolfe.macros

import ml.wolfe.{MPGraph, Wolfe}
import ml.wolfe.util.NLP
import Wolfe._
import NLP._
import OptimizedOperators._
import ml.wolfe.MPGraph.MPSchedulerImpl
import ml.wolfe.MPGraph.MPSchedulerImpl.MPDirection


/**
 * Created by rockt on 09/04/2014.
 */
class MPSchedulerSpec extends StructureIsomorphisms {
  "A scheduler" should {
    "given a tree" should {
      val graph = new MPGraph()
      (0 to 5).foreach(i => graph.addNode(0)) //dummy nodes
      (0 to 104).foreach(i => graph.addTableFactor(Array(), Array(), Array())) //dummy factors
      val edges = Array(
        100 -> 0, 100 -> 1,
        101 -> 2, 101 -> 3,
        102 -> 1, 102 -> 3, 102 -> 4,
        103 -> 4, 103 -> 5,
        104 -> 5
      )
      edges.foreach(edge => graph.addEdge(graph.getFactor(edge._1), graph.getNode(edge._2)))
      graph.build()
      val root = graph.edges(2) //"randomly" pick an edge that defines the root node

      val validForwardPasses = Set(
        Seq(104 -> 5, 100 -> 1, 103 -> 4, 102 -> 3),
        Seq(104 -> 5, 103 -> 4, 100 -> 1, 102 -> 3),
        Seq(100 -> 1, 104 -> 5, 103 -> 4, 102 -> 3)
      )

      val validBackwardPasses = Set(
        Seq(101 -> 3, 102 -> 1, 100 -> 0, 102 -> 4, 103 -> 5),
        Seq(101 -> 3, 102 -> 1, 102 -> 4, 100 -> 0, 103 -> 5),
        Seq(101 -> 3, 102 -> 1, 102 -> 4, 103 -> 5, 100 -> 0)
      )

      val validForwardBackwardPasses = for {
        forward <- validForwardPasses
        backward <- validBackwardPasses
      } yield forward ++ Seq((root.f.index, root.n.index)) ++ backward

      "return the right edge ordering for a forward messaging pass" in {
        val predicted = MPSchedulerImpl.schedule(root, MPDirection.Forward).map(e => (e.f.index, e.n.index))
        info("up: " + predicted)
        validForwardPasses should contain(predicted)
      }

      "return the right edge ordering for a backward messaging pass" in {
        val predicted = MPSchedulerImpl.schedule(root, MPDirection.Backward).map(e => (e.f.index, e.n.index))
        info("down: " + predicted)
        validBackwardPasses should contain(predicted)
      }

      "return the right edge ordering for a forward-backward messaging pass" in {
        val predicted = MPSchedulerImpl.schedule(root).map(e => (e.f.index, e.n.index))
        info("schedule: " + predicted)
        validForwardBackwardPasses should contain(predicted)
      }
    }

    "given a loopy graph" should {
      val graph = new MPGraph()
      (0 to 2).foreach(i => graph.addNode(0)) //dummy nodes
      (0 to 102).foreach(i => graph.addTableFactor(Array(), Array(), Array())) //dummy factors
      val edges = Array(
          100 -> 0,
          100 -> 1,
          101 -> 1,
          101 -> 2,
          102 -> 2,
          102 -> 0
        )
      edges.foreach(edge => graph.addEdge(graph.getFactor(edge._1), graph.getNode(edge._2)))
      graph.build()
      val root = graph.edges(2) //"randomly" pick an edge that defines the root node
      info("root: " + root)

      "return only one loop for a forward messaging pass" in {
        val actual = Seq((101,1), (100,0), (102,2))
        val predicted = MPSchedulerImpl.schedule(root, MPDirection.Forward).map(e => (e.f.index, e.n.index))
        info("up: " + predicted)
        predicted should be(actual)
      }

      "return only one loop for a backward messaging pass" in {
        val actual = Seq((101,2), (102,0), (100,1))
        val predicted = MPSchedulerImpl.schedule(root, MPDirection.Backward).map(e => (e.f.index, e.n.index))
        info("down: " + predicted)
        predicted should be(actual)
      }


      "return a tree-like ordering for a forward/backward messaging pass" in {
        val actual = Seq((101,1), (100,0), (102,2), (100,1), (101,2), (102,0))
        val predicted = MPSchedulerImpl.schedule(root).map(e => (e.f.index, e.n.index))
        info("schedule: " + predicted)

      }
    }
  }

/*
    "A MetaStructuredFactor" should {
    "does not generate cycles" in {
      def space = Wolfe.all(Sentence)(seqs(5, Wolfe.all(Token)(Seq("blub") x Seq(Tag("blah")) x Seq(Chunk("blub")))))

      def features(s: Sentence) = {
//        sum { over(0 until s.tokens.size) of (i => oneHot('word -> s.tokens(i).word -> s.tokens(i).tag)) }
        sum { over(0 until s.tokens.size) of (i => oneHot('word -> s.tokens(i).word -> s.tokens(i).chunk)) }
      }

      val w = oneHot(1)
      def potential(s: Sentence) = w dot features(s)

      val factor = MetaStructuredFactor.structuredFactor[Sentence](space, potential)

      //TODO test for cycles
      println(factor.graph.edges)
    }
  }
*/
}
