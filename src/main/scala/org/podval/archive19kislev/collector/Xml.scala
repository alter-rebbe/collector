package org.podval.archive19kislev.collector

import java.io.File

import scala.xml.{Elem, Node, PrettyPrinter, Text, TopScope, Utility, XML}

object Xml {

  def load(file: File): Elem = try {
    Utility.trimProper(XML.loadFile(file)).asInstanceOf[Elem]
  } catch {
    case e: org.xml.sax.SAXParseException =>
      throw new IllegalArgumentException(s"In file $file:", e)
  }

  def contentOf(element: Option[Elem]): Seq[Node] =
    element.fold[Seq[Node]](Text(""))(_.child.map(removeNamespace))

  private def removeNamespace(node: Node): Node = node match {
    case e: Elem => e.copy(scope = TopScope, child = e.child.map(removeNamespace))
    case n => n
  }

  implicit class Ops(elem: Elem) {

    def elems(name: String): Seq[Elem] = {
      val result = elem.elements
      result.foreach(_.check(name))
      result
    }

    def elemsFilter(name: String): Seq[Elem] = elem.elements.filter(_.label == name)

    def elements: Seq[Elem] = elem.child.filter(_.isInstanceOf[Elem]).map(_.asInstanceOf[Elem]).toSeq

    def descendants(name: String): Seq[Elem] = elem.flatMap(_ \\ name).filter(_.isInstanceOf[Elem]).map(_.asInstanceOf[Elem])

    def getAttribute(name: String): String = attributeOption(name).getOrElse(throw new NoSuchElementException(s"No attribute $name"))

    def attributeOption(name: String): Option[String] = elem.attributes.asAttrMap.get(name)

    def intAttributeOption(name: String): Option[Int] = attributeOption(name).map { value =>
      try { value.toInt } catch { case e: NumberFormatException => throw new IllegalArgumentException(s"$value is not a number", e)}
    }

    def intAttribute(name: String): Int = intAttributeOption(name).getOrElse(throw new NoSuchElementException(s"No attribute $name"))

    def booleanAttribute(name: String): Boolean = {
      val value = attributeOption(name)
      value.isDefined && ((value.get == "true") || (value.get == "yes"))
    }

    def oneChild(name: String): Elem = oneOptionalChild(name, required = true).get
    def optionalChild(name: String): Option[Elem] = oneOptionalChild(name, required = false)

    private[this] def oneOptionalChild(name: String, required: Boolean = true): Option[Elem] = {
      val children = elem \ name

      if (children.size > 1) throw new NoSuchElementException(s"To many children with name '$name'")
      if (required && children.isEmpty) throw new NoSuchElementException(s"No child with name '$name'")

      if (children.isEmpty) None else Some(children.head.asInstanceOf[Elem])
    }

    def check(name: String): Elem = {
      if (elem.label != name) throw new NoSuchElementException(s"Expected name $name but got $elem.label")
      elem
    }

    def write(
      directory: File,
      fileName: String,
    ): Unit = Util.write(directory, fileName + ".xml", content =
      """<?xml version="1.0" encoding="UTF-8"?>""" + "\n" +
        """<?xml-model href="http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/tei_all.rng" schematypens="http://relaxng.org/ns/structure/1.0"?>""" + "\n" +
        Xml.prettyPrinter.format(elem)
    )
  }

  private val prettyPrinter: PrettyPrinter = new PrettyPrinter(120, 2)
}
