package com.example.spike

import cats.effect.kernel.Async
import cats.implicits.*
import com.example.spike.Main.Config
import com.example.spike.Utils.OcflFile
import io.ocfl.api.model.{DigestAlgorithm, ObjectVersionId, OcflObjectVersionFile}
import io.ocfl.api.{OcflConfig, OcflRepository}
import io.ocfl.core.OcflRepositoryBuilder
import io.ocfl.core.extension.storage.layout.config.HashedNTupleLayoutConfig
import io.ocfl.core.storage.{OcflStorage, OcflStorageBuilder}
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.*
import java.nio.file.{Files, Paths}
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.xml.XML

trait Ocfl[F[_]]:
  def generate(id: UUID): F[List[OcflFile]]

object Ocfl:
  def apply[F[_]](using ev: Ocfl[F]): Ocfl[F] = ev

  private def createRepo(config: Config): OcflRepository = {
    val repoDir = Paths.get(config.ocflRepoDir)
    val workDir = Paths.get(config.ocflWorkDir)
    val storage: OcflStorage = OcflStorageBuilder.builder().fileSystem(repoDir).build
    val ocflConfig: OcflConfig = new OcflConfig()
    ocflConfig.setDefaultDigestAlgorithm(DigestAlgorithm.sha256)
    new OcflRepositoryBuilder()
      .defaultLayoutConfig(new HashedNTupleLayoutConfig())
      .storage(storage)
      .ocflConfig(ocflConfig)
      .prettyPrintJson()
      .workDir(workDir)
      .build()
  }



  given impl[F[_] : Async]: Ocfl[F] = (id: UUID) => ConfigSource.default.loadF[F, Config]().map { config =>
    val repo = createRepo(config)
    val objectVersion = repo.getObject(ObjectVersionId.head(id.toString))
    val files = objectVersion.getFiles.asScala.toList

    def loadMetadataXml(file: OcflObjectVersionFile) = XML.loadString(Files.readString(Paths.get(config.ocflRepoDir, file.getStorageRelativePath)))

    val ioMetadata = files.find(_.getPath.contains(s"IO_Metadata.xml")).map(loadMetadataXml)
    val zrefOpt = ioMetadata.flatMap { metadata =>
      (metadata \ "Identifiers" \ "Identifier").toList
        .find(i => (i \ "Type").text == "BornDigitalRef")
        .map(i => (i \ "Value").text)
    }

    val titleOpt = ioMetadata.map { metadata =>
      (metadata \ "InformationObject" \ "Title").text
    }

    files.filter(_.getPath.contains(s"CO_Metadata.xml")).flatMap { coMetadataFile =>
      val coMetadataPath = coMetadataFile.getPath.split("/").dropRight(1).mkString("/")
      val filePrefix = s"$coMetadataPath/original/g1/"
      val coMetadata = loadMetadataXml(coMetadataFile)
      val name = (coMetadata \ "ContentObject" \ "Title").text
      val fileId = (coMetadata \ "ContentObject" \ "Ref").text
      for {
        zref <- zrefOpt
        title <- titleOpt
        preservedFile <- files.find(_.getPath.startsWith(filePrefix))
      } yield OcflFile(objectVersion.getVersionNum.getVersionNum, id.toString, title, fileId, zref, s"${config.ocflRepoDir}/${preservedFile.getStorageRelativePath}", name)
    }
  }
