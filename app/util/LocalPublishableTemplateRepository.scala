package util

import java.net.URI
import java.util.UUID
import java.io.File

import sbt.IO

import activator.{hashing, Validating, ProcessResult}
import activator.cache.CacheProperties
import activator.templates.repository.{PublishableTemplateRepository, UriRemoteTemplateRepository}
import play.api.Logger

class LocalPublishableTemplateRepository(log: akka.event.LoggingAdapter, baseUri: URI) extends UriRemoteTemplateRepository(baseUri, log) with PublishableTemplateRepository {

  override def publishIndex(indexZip: File, serial: Long): ProcessResult[Unit] = {
    for {
      hash <- hashFile(indexZip)
      _ <- publish(layout.index(hash), indexZip)
      propsFile <- makeTmpFile
      props <- CacheProperties.write(propsFile, serial, hash)
      _ <- publish(layout.currentIndexTag, props)
    } yield ()
  }

  override def publishTemplateBundle(activatorVersion: String, uuid: UUID, templateName: String, zipFile: File): ProcessResult[Unit] = {
    for {
      location <- Validating.withMsg(s"Unable to publish template bundle: $templateName $uuid") {
        layout.templateBundle(activatorVersion, uuid.toString, templateName)
      }
      result <- publish(location, zipFile)
    } yield result
  }

  override def publishAuthorLogo(uuid: UUID, logoFile: File, contentType: String): ProcessResult[Unit] = {
    for {
      location <- Validating.withMsg(s"Unable to publish authorLogo for template instance $uuid") {
        layout.authorLogo(uuid.toString)
      }
      result <- publish(location, logoFile)
    } yield result
  }

  override def publishTemplate(uuid: UUID, zipFile: File): ProcessResult[Unit] = {
    for {
      location <- Validating.withMsg(s"Unable to publish template: $uuid") {
        layout.template(uuid.toString)
      }
      result <- publish(location, zipFile)
    } yield result
  }

  override def hasCurrentRemoteIndexSerial: ProcessResult[Boolean] = {
    Validating.withMsg("Unable to access repository") {
      true
    }
  }

  override def currentRemoteIndexSerial: ProcessResult[Long] = {
    Validating.withMsg(s"Unable to get current index serial") {
      new CacheProperties(new File(layout.currentIndexTag)).cacheIndexSerial
    }
  }


  private def hashFile(file: java.io.File): ProcessResult[String] = {
    Validating.withMsg(s"Failed to hash index file: $file") {
      val h = hashing hash file
      log.debug(s"index $file hashes to $h")
      h
    }
  }

  private def makeTmpFile: ProcessResult[java.io.File] = {
    Validating.withMsg(s"Unable to create temporary file") {
      val file = java.io.File.createTempFile("activator-cache", "properties")
      file.deleteOnExit()
      file
    }
  }

  private def publish(dest: URI, src: java.io.File): ProcessResult[Unit] = {
    Validating.withMsg(s"Failed to publish $src to $dest") {
      Logger.info(s"Copy ${src.getAbsolutePath} to $dest")
      IO.copyFile(src, new File(dest))
    }
  }

}
