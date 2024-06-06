package com.example.spike

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import uk.gov.nationalarchives.DASQSClient
import io.circe.generic.auto.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*
import pureconfig.module.catseffect.syntax.*
import cats.implicits.*
import scala.concurrent.duration.*
import java.util.UUID

object Main extends IOApp {

  case class Config(databasePath: String, queueUrl: String, ocflRepoDir: String, ocflWorkDir: String)derives ConfigReader

  case class Message(id: UUID)

  private def logError(err: Throwable) = for {
    logger <- Slf4jLogger.create[IO]
    _ <- logger.error(err)("Error running disaster recovery")
  } yield ()

  override def run(args: List[String]): IO[ExitCode] = for {
    sqs <- IO(DASQSClient[IO]())
    config <- ConfigSource.default.loadF[IO, Config]()
    _ <- {
      Stream.fixedRateStartImmediately[IO](20.seconds) >>
        runBuilder(sqs, config)
          .handleErrorWith(err => Stream.eval(logError(err)))
    }.compile.drain
  } yield ExitCode.Success

  private def runBuilder(sqs: DASQSClient[IO], config: Config): Stream[IO, Unit] =
    Stream
      .eval(sqs.receiveMessages[Message](config.queueUrl))
      .evalMap { messages =>
        for {
          _ <- IO.whenA(messages.nonEmpty)(Builder[IO].run(messages))
          _ <- messages.map(message => sqs.deleteMessage(config.queueUrl, message.receiptHandle)).sequence
        } yield ()
      }
}
