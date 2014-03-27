package ml.wolfe.macros

import scala.reflect.macros.Context

/**
 * @author Sebastian Riedel
 */
trait MetaSeqStructures[C<:Context] {
  this:MetaStructures[C] =>

  import context.universe._

  trait MetaSeqStructure extends MetaStructure {
    def elementMetaStructure:MetaStructure
    def lengthInitializer:Tree

    var lengthInitialized = lengthInitializer != EmptyTree

    lazy val className = newTypeName(context.fresh("SeqStructure"))
    def classDef(graphName:TermName) = {
      val elementDef = elementMetaStructure.classDef(graphName)

      q"""
        final class $className extends Structure[$argType]{

          $elementDef

          private var iterator:Iterator[Unit] = _

          private var _elements:Array[${elementMetaStructure.className}] = null
          def setLength(length:Int) {
            _elements = Array.fill(length)(new ${elementMetaStructure.className})
          }
          def elements = _elements
          def children() = elements.iterator
          def nodes = _elements.iterator.flatMap(_.nodes)
          def graph = $graphName

          def resetSetting() { iterator = Structure.settingsIterator(_elements.toList)()}
          def hasNextSetting = iterator.hasNext
          def nextSetting = iterator.next

          def setToArgmax() {_elements.foreach(_.setToArgmax())}
          def value() = _elements.view.map(_.value()).toList

          def observe(value:$argType) {
            if (_elements == null || _elements.length != value.size) setLength(value.size)
            for (i <- value.indices) _elements(i).observe(value(i))
          }

          $lengthInitializer
        }
        """
    }
    def children = List(elementMetaStructure)
    def domainDefs = Nil
    /**
     * A matcher takes a tree and returns a tree that represents the sub-structure that corresponds to the
     * tree, if any. This method creates matchers for the structures of this meta-structure.
     * @param parent a matcher for the parent structure.
     * @param result the current result matcher.
     * @return a matcher...
     */
    def matcher(parent: Tree => Option[StructurePointer], result: Tree => Option[StructurePointer]) = {
      def matchElementOf(tree:Tree):Option[StructurePointer] = tree match {
        case q"$f.apply($arg)" => parent(f) match {
          case Some(pointer) => Some(StructurePointer(q"${pointer.structure}.elements($arg)",elementMetaStructure))
          case _ => None
        }
        case _ => None
      }
      elementMetaStructure.matcher(matchElementOf, (t: Tree) => matchElementOf(t).orElse(result(t)))
    }
  }

}