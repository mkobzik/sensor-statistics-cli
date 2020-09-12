package com.github.mkobzik.sensorstatisticscli

import cats.Monad
import cats.effect.{Console, ExitCode, IO, IOApp}
import cats.syntax.all._

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val console: Console[IO] = Console.io

    program[IO].as(ExitCode.Success)
  }

  private def program[F[_]: Console: Monad]: F[Unit] = for {
    _ <- Console[F].putStrLn("Hello")
  } yield ()

}
