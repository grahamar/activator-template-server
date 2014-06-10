package util

import java.util.{Properties, Date, UUID}
import java.io.ByteArrayOutputStream

import sbt.IO
import play.api.Logger

import controllers.Application.TemplateContribData

import org.eclipse.jgit.api.{Git, ArchiveCommand}
import org.eclipse.jgit.archive.ZipFormat
import activator.cache._
import org.apache.commons.compress.archivers.zip.ZipFile

object GitUtils {
  import play.api.Play._

  def cacheTemplateFromRepo(repoData: TemplateContribData): IndexStoredTemplateMetadata = {
    IO.withTemporaryDirectory { tmpDir =>
      val zipOut = new ByteArrayOutputStream()
      ArchiveCommand.registerFormat("zip", new ZipFormat())
      try {
        val git = Git.cloneRepository().setURI(repoData.repo).setDirectory(tmpDir).call()
        git.archive().
          setTree(git.getRepository.resolve("master")).
          setFormat("zip").
          setOutputStream(zipOut).
          call()
        Logger.info(s"Tmp Directory: ${tmpDir.getAbsolutePath}")
        IO.withTemporaryFile(s"${git.describe().call()}", "zip") { tmpZipFile =>
          Logger.info(s"Tmp Zip: ${tmpZipFile.getAbsolutePath}")
          IO.write(tmpZipFile, zipOut.toByteArray)
          val id = UUID.randomUUID()
          Settings.LocalRepo.publishTemplate(id, tmpZipFile)
          gitArchiveToMetadata(id, repoData.repo, new ZipFile(tmpZipFile))
        }
      } finally {
        zipOut.close()
        ArchiveCommand.unregisterFormat("zip")
      }
    }
  }

  private def gitArchiveToMetadata(id: UUID, repo: String, template: ZipFile): IndexStoredTemplateMetadata = {
    val metaEntry = template.getEntry(Constants.METADATA_FILENAME)
    val props = new Properties()
    props.load(template.getInputStream(metaEntry))
    val metadata = IndexStoredTemplateMetadata(
      id = id.toString,
      timeStamp = new Date().getTime,
      featured = false,
      usageCount = None,
      name = props.getProperty("name"),
      title = props.getProperty("title"),
      description = props.getProperty("description"),
      authorName = Option(props.getProperty("authorName")).filterNot(_.isEmpty).orElse(current.configuration.getString("defaultAuthorName")).getOrElse("Anonymous"),
      authorLink = Option(props.getProperty("authorLink")).filterNot(_.isEmpty).getOrElse(""),
      tags = props.getProperty("tags").split(','),
      templateTemplate = false,
      sourceLink = Option(props.getProperty("sourceLink")).filterNot(_.isEmpty).getOrElse(repo),
      authorLogo = Option(props.getProperty("authorLogo")).filterNot(_.isEmpty),
      authorBio = Option(props.getProperty("authorBio")).filterNot(_.isEmpty),
      authorTwitter = Option(props.getProperty("authorTwitter")).filterNot(_.isEmpty),
      category = TemplateMetadata.Category.COMPANY,
      creationTime = new Date().getTime
    )
    template.close()
    metadata
  }

}
