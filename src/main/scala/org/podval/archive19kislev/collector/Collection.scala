package org.podval.archive19kislev.collector

import java.io.File

import scala.xml.{Elem, Node, Text}
import Xml.Ops

final class Collection(directory: File, xml: Elem) {
  def directoryName: String = directory.getName

  def reference: String = xml.optionalChild("reference").map(_.text).getOrElse(directoryName)
  def title: String = xml.optionalChild("title").map(_.text).getOrElse(reference)
  def description: Seq[Elem] = xml.oneChild("description").elements

  val teiDirectory = new File(directory, Layout.Collection.teiDirectoryName)
  private val facsimilesDirectory = new File(directory, Layout.Collection.facsimilesDirectoryName)
  val documentsDirectory = new File(directory, Layout.Collection.docsDirectoryName)
  val viewersDirectory = new File(directory, Layout.Collection.facsDirectoryName)

  def documentUrl(name: String): String =
    "/" + directoryName + "/" + Layout.Collection.docsDirectoryName + "/" + name + ".html"

  val documents: Seq[Document] = {
    def splitLang(name: String): (String, Option[String]) = {
      val dash: Int = name.lastIndexOf('-')
      if ((dash == -1) || (dash != name.length-3)) (name, None) else (name.substring(0, dash), Some(name.substring(dash+1)))
    }
    val namesWithLang: Seq[(String, Option[String])] =
      Collection.listNames(teiDirectory, ".xml", Page.checkBase).map(splitLang)

    val translations: Map[String, Seq[String]] = namesWithLang
      .filter(_._2.isDefined)
      .map(e => (e._1, e._2.get))
      .groupBy(_._1)
      .view.mapValues(_.map(_._2)).toMap

    val names: Seq[String] = namesWithLang.filter(_._2.isEmpty).map(_._1)

    val namesWithSiblings: Seq[(String, (Option[String], Option[String]))] = if (names.isEmpty) Seq.empty else {
      val documentOptions: Seq[Option[String]] = names.map(Some(_))
      val prev = None +: documentOptions.init
      val next = documentOptions.tail :+ None
      names.zip(prev.zip(next))
    }

    for ((name, (prev, next)) <- namesWithSiblings)
    yield new Document(this, name, prev, next, translations.getOrElse(name, Seq.empty))
  }

  private val pages: Seq[Page] = documents.flatMap(_.pages)

  private val missingPages: Seq[String] = pages.filterNot(_.isPresent).map(_.displayName)

  /// Check consistency
  check()

  private def check(): Unit = {
    // Check for duplicates
    val name2page = collection.mutable.Map[String, Page]()
    for (page <- pages) {
      // TODO allow duplicates in consecutive documents
      //      if (name2page.contains(page.name)) throw new IllegalArgumentException(s"Duplicate page: ${page.name}")
      name2page.put(page.name, page)
    }

    // Check that all the images are accounted for
    val imageNames: Seq[String] = Collection.listNames(facsimilesDirectory, ".jpg", Page.check)
    val orphanImages: Seq[String] = (imageNames.toSet -- pages.map(_.name).toSet).toSeq.sorted
    if (orphanImages.nonEmpty) throw new IllegalArgumentException(s"Orphan images: $orphanImages")
  }

  // TODO check order

  private def writeIndex(): Unit = {
    val content =
      <TEI xmlns="http://www.tei-c.org/ns/1.0">
        <teiHeader>
          <fileDesc>
            <publicationStmt>
              <publisher><ptr target="www.alter-rebbe.org"/></publisher>
              <availability status="free">
                <licence><ab><ref n="license" target="http://creativecommons.org/licenses/by/4.0/">
                  Creative Commons Attribution 4.0 International License </ref></ab></licence>
              </availability>
            </publicationStmt>
            <sourceDesc><p>Facsimile</p></sourceDesc>
          </fileDesc>
          <profileDesc><calendarDesc><calendar xml:id="julian"><p>Julian calendar</p></calendar></calendarDesc></profileDesc>
        </teiHeader>
        <text>
          <body>
            <title>{title}</title>
            {description}
            {Collection.table.toTei(documents)}
            {if (missingPages.isEmpty) Seq.empty
             else <p>Отсутствуют фотографии {missingPages.length} страниц: {missingPages.mkString(" ")}</p>}
          </body>
        </text>
      </TEI>


    // Index
    content.write(directory, "index")

    // Index wrapper
    Util.write(directory, "index.html", Seq(
      "title" -> reference,
      "layout" -> "tei",
      "tei" -> "index.xml",
      "wide" -> "true",
      "target" -> "collectionViewer"
    ))
  }

  private def writeWrappers(): Unit = {
    // TODO clean out documentsDirectory and viewersDirectory.
    for (document <- documents) document.writeWrappers()
  }

  def process(): Unit = {
    writeIndex()
    writeWrappers()
  }
}

object Collection {

  private val table: Table[Document] = new Table[Document](
    _.partTitle.toSeq.map(partTitle =>  <span rendition="part-title">{partTitle.child}</span>),
    Column.elem("Описание", "description", _.description),
    Column.string("Дата", "date", _.date),
    Column.elem("Кто", "author", _.author),
    new Column("Кому", "addressee",  _.addressee.fold[Seq[Node]](Text(""))(addressee =>
      <persName ref={addressee.ref.map("#" + _).orNull}>{addressee.name}</persName>)),
    Column.string("Язык", "language", _.language),
    new Column("Документ", "document", document =>
      <ref target={documentPath(document)} role="documentViewer">{document.name}</ref>),
    new Column("Страницы", "pages", document => for (page <- document.pages) yield
      <ref target={documentPath(document) + s"#p${page.name}"} role="documentViewer"
           rendition={if (page.isPresent) "page" else "missing-page"}>{page.displayName}</ref>),
    Column.elem("Расшифровка", "transcriber", _.transcriber)
  )

  private def documentPath(document: Document): String =
    s"${Layout.Collection.docsDirectoryName}/${document.name}.html"

  private def listNames(directory: File, extension: String, check: String => Unit): Seq[String] = {
    val result = Util.filesWithExtensions(directory, extension)
    //    result.foreach(check)
    result.sorted
  }
}
