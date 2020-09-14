package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Paths

import cats.Monad
import cats.effect.Console.implicits._
import cats.effect.{Blocker, Console, ExitCode, IO, IOApp}
import cats.syntax.all._

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .evalMap { blocker =>
        implicit val sensorStatistics: SensorStatistics[IO] = SensorStatistics.instance[IO](blocker)

        program[IO](args.head)
      }
      .use(_ => IO.unit)
      .as(ExitCode.Success)

  private def program[F[_]: SensorStatistics: Console: Monad](path: String): F[Unit] = for {
    _     <- Console[F].putStrLn("Calculating statistics...")
    stats <- SensorStatistics[F].calculate(Paths.get(path))
    _     <- Console[F].putStrLn(stats)
    _     <- stats.sensors.traverse_(sensor => Console[F].putStrLn(sensor))
  } yield ()

}
