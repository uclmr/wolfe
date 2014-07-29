package ml.wolfe.nlp

import scala.collection.mutable


case class CharOffsets(start: Int, end: Int)

/**
 * A natural language token.
 * @param word word at token.
 * @param offsets character offsets
 * @param posTag part-of-speech tag at token.
 * @param attributes collection of generic attributes.
 */
case class Token(word: String, offsets: CharOffsets, posTag: String = null, attributes: Attributes = Attributes.empty) {
  def toTaggedText = word + "/" + posTag
  def sentence$(implicit graph: ObjectGraph) =
    graph.receiveOrdered[Token, Sentence, Sentence]('tokens, this)((_, s) => s)
  def next$(implicit graph: ObjectGraph) =
    graph.receiveOrdered[Token, Sentence, Option[Token]]('tokens, this)((i, s) => s.tokens.lift(i + 1))
  def prev$(implicit graph: ObjectGraph) =
    graph.receiveOrdered[Token, Sentence, Option[Token]]('tokens, this)((i, s) => s.tokens.lift(i - 1))

}

/**
 * A sentence consisting of tokens.
 * @param tokens the tokens of the sentence.
 * @param attributes collection of generic attributes.
 */
case class Sentence(tokens: Seq[Token], attributes: Attributes = Attributes.empty) {
  def toText = tokens map (_.word) mkString " "
  def toTaggedText = tokens map (_.toTaggedText) mkString " "
  def $tokens(implicit graph: ObjectGraph) =
    graph.link1toNOrdered[Sentence, Token, Seq[Token]]('tokens, this, tokens)
}

/**
 * A document consisting of sentences.
 * @param source the source text.
 * @param sentences list of sentences.
 * @param attributes collection of generic attributes.
 */
case class Document(source: String, sentences: Seq[Sentence], attributes: Attributes = Attributes.empty) {
  def toText = sentences map (_.toText) mkString "\n"
  def toTaggedText = sentences map (_.toTaggedText) mkString "\n"
  def tokens = sentences flatMap (_.tokens)
}




object Data {
  def main(args: Array[String]) {
    val source = "My name is Wolfe."

    val result = SISTAProcessors.mkDocument(source)

    implicit val graph = new SimpleObjectGraph
    val s = result.sentences.head
    println(s.$tokens.head.sentence$ == s)
    println(s.tokens.head.next$)

    //    val result2 = SISTAProcessors.annotate(source)
    //
    //    println(result2.toTaggedText)
    //    println(result2)


  }
}