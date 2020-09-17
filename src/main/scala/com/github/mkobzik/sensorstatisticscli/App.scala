package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import cats.Monad
import cats.effect.Console.implicits._
import cats.effect.{Blocker, Console, ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline._
import com.monovore.decline.effect._
import PrettyShow.ops._

object App extends CommandIOApp(name = "sensor-statistics", header = "Calculate statistics from humidity sensor data") {

  override def main: Opts[IO[ExitCode]] =
    Opts.argument[Path]("report_directory_path").map { path =>
      run(path)
    }

  private def run(path: Path): IO[ExitCode] =
    Blocker[IO]
      .evalMap { blocker =>
        implicit val sensorStatistics: SensorStatistics[IO] = SensorStatistics.instance[IO](blocker)

        program[IO](path)
      }
      .use(_ => IO.unit)
      .as(ExitCode.Success)

  private def program[F[_]: SensorStatistics: Console: Monad](path: Path): F[Unit] = for {
    _     <- Console[F].putStrLn("Calculating statistics...")
    stats <- SensorStatistics[F].calculate(path)
    _     <- Console[F].putStrLn(stats.prettyShow)
  } yield ()

}
