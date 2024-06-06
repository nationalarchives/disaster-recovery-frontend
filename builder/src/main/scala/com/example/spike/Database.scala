package com.example.spike

import cats.effect.{Async, Concurrent, IO}
import cats.implicits.*
import com.example.spike.Main.Config
import com.example.spike.Utils.OcflFile
import doobie.Update
import doobie.implicits.*
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.*

trait Database[F[_]]:
  def write(files: List[OcflFile]): F[Int]

object Database:
  def apply[F[_]](using ev: Database[F]): Database[F] = ev

  given impl[F[_]: Async]: Database[F] = new Database[F] {

    val loadXa: F[Aux[F, Unit]] = ConfigSource.default.loadF[F, Config]().map { config =>
      Transactor.fromDriverManager[F](
        driver = "org.sqlite.JDBC", url = s"jdbc:sqlite:${config.databasePath}", logHandler = Option(LogHandler.jdkLogHandler)
      )
    }

    override def write(files: List[OcflFile]): F[Int] = {
      val sql = "insert into files (version, id, name, fileId, zref, path, fileName) values (?, ?, ?, ?, ?, ?, ?)"
      for {
        logger <- Slf4jLogger.create[F]
        xa <- loadXa
        ret <- Update[OcflFile](sql).updateMany(files).transact(xa)
        _ <- logger.info(s"Created $ret rows")
      } yield ret
    }
  }

