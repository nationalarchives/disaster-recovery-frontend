package com.example.spike

import cats.effect.Async
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import org.http4s.server.staticcontent.resourceServiceBuilder

object FrontEndServer:

  def run[F[_]: Async]: F[Nothing] = {
    val httpApp = (
      FrontEndRoutes.ocflRoutes[F] <+> resourceServiceBuilder[F]("/").toRoutes
      ).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    for {
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.

      _ <- 
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build          
    } yield ()
  }.useForever
