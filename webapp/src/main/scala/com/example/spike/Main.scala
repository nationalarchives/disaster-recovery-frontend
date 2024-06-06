package com.example.spike

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] = FrontEndServer.run[IO]
    .handleError(err => {
      err.printStackTrace()
      ExitCode.Error
    })
