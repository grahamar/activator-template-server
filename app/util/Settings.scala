package util

import java.io.File

import play.api.GlobalSettings
import akka.event.NoLogging
import sbt.IO

import activator.cache.IndexDbProvider

object Settings extends GlobalSettings {

  val BaseFile = new File("cache").getAbsoluteFile
  val IndexDir = new File(BaseFile, "index")

  IO.createDirectory(BaseFile)
  IO.createDirectory(IndexDir)

  val LocalRepo = new LocalPublishableTemplateRepository(NoLogging, BaseFile.toURI)
  val IndexProvider = IndexDbProvider.default

}
