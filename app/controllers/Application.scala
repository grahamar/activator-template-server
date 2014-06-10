package controllers

import java.io.File

import play.api.Logger
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import sbt.{Path, IO}

import util.{Settings, LoggedAction, GitUtils}
import activator._
import activator.ProcessSuccess
import activator.cache.CacheProperties

object Application extends Controller {

  def index = LoggedAction {
    Ok(views.html.index())
  }

  val templateForm = Form(
    mapping(
      "templateRepo" -> text
    )(TemplateContribData.apply)(TemplateContribData.unapply)
  )

  def template = LoggedAction { request =>
    try {
      templateForm.bindFromRequest()(request).fold(
        formWithErrors => {
          BadRequest(views.html.index())
        },
        data => {
          val metadata = GitUtils.cacheTemplateFromRepo(data)
          val writer = Settings.IndexProvider.write(Settings.IndexDir)
          try {
            writer.insert(metadata)
          } finally {
            writer.close()
          }
          Redirect(routes.Application.index())
        }
      )
    } catch {
      case ex: Exception =>
        Logger.error("", ex)
        throw ex
    }
  }

  def templateBundle(firstBit: String, secondBit: String, id: String, activatorVersion: String, templateName: String) = LoggedAction { request =>
    ???
  }

  def templateFile(firstBit: String, secondBit: String, templateName: String) = LoggedAction { request =>
    Ok.sendFile(
      content = Path(Settings.BaseFile) / s"templates/$firstBit/$secondBit/$templateName",
      inline = true
    )
  }

  def currentIndexFile = LoggedAction {
    try {
      val result: ProcessResult[File] = IO.withTemporaryFile("index", "zip") { indexZip =>
        val indexFiles = Settings.IndexDir.listFiles().map(f => f -> f.getName)
        ZipHelper.zip(indexFiles, indexZip)
        for {
          hash <- hashFile(indexZip)
          propsFile <- makeTmpFile
          props <- CacheProperties.write(propsFile, hash.hashCode, hash)
        } yield props
      }

      result match {
        case s: ProcessSuccess[File] =>
          Ok.sendFile(
            content = s.value,
            inline = true
          )

        case e: ProcessFailure =>
          e.failures.foreach(e => e.cause.map(Logger.error(e.msg, _)).getOrElse(Logger.error(e.msg)))
          InternalServerError(e.failures.map(f => f.msg).mkString("\n"))
      }
    } catch {
      case ex: Exception =>
        Logger.error("", ex)
        throw ex
    }
  }

  def indexFile(indexFile: String) = LoggedAction { request =>
    IO.withTemporaryFile("index", "zip") { indexZip =>
      val indexFiles = Settings.IndexDir.listFiles().map(f => f -> f.getName)
      ZipHelper.zip(indexFiles, indexZip)
      Ok.sendFile(
        content = indexZip,
        inline = true
      )
    }
  }

  private def hashFile(file: java.io.File): ProcessResult[String] = {
    Validating.withMsg(s"Failed to hash index files: $file") {
      hashing.hash(file)
    }
  }

  private def makeTmpFile: ProcessResult[java.io.File] = {
    Validating.withMsg(s"Unable to create temporary file") {
      val file = java.io.File.createTempFile("activator-cache", "properties")
      file.deleteOnExit()
      file
    }
  }

  case class TemplateContribData(repo: String)

}