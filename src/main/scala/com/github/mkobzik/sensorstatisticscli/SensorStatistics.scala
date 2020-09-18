package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import atto.Atto._
import cats.ApplicativeError
import cats.effect.{Blocker, ContextShift, Sync}
import cats.syntax.all._
import cats.tagless.finalAlg
import com.github.mkobzik.sensorstatisticscli.errors.Error.{CorruptedCsv, HumidityOutOfRange, NotADirectory}
import com.github.mkobzik.sensorstatisticscli.models._
import fs2.{Pipe, text}

@finalAlg
trait SensorStatistics[F[_]] {
  def calculate(dailyReportDir: Path): F[Statistics]
}

object SensorStatistics {

  type FileIndex = Long

  implicit def instance[F[_]: SensorStatisticsProcessor: Sync: ContextShift](blocker: Blocker): SensorStatistics[F] =
    new SensorStatistics[F] {

      override def calculate(dailyReportDir: Path): F[Statistics] = {
        isDirectory(dailyReportDir) *>
          fs2.io.file
            .directoryStream(blocker, dailyReportDir, _.toFile.getName.endsWith(".csv"))
            .zipWithIndex
            .flatMap { case (report, fileIndex) =>
              fs2.io.file
                .readAll(report, blocker, 4096)
                .through(parseCsv)
                .map(sample => sample -> fileIndex)
            }
            .through(SensorStatisticsProcessor[F].createStatistics)
            .compile
            .lastOrError
      }

      private def isDirectory(dailyReportDir: Path): F[Unit] = {
        Sync[F].ifM(Sync[F].delay(dailyReportDir.toFile.isDirectory))(
          ifTrue = Sync[F].unit,
          ifFalse = Sync[F].raiseError(NotADirectory(dailyReportDir))
        )
      }

      private def parseCsv: Pipe[F, Byte, Sample] = {
        _.through(text.utf8Decode)
          .through(text.lines)
          .drop(1) //header
          .evalMap(lineToSample)
      }

      private def lineToSample(line: String): F[Sample] = {
        val idParser = (long <~ char(',')).map(Sensor.Id.apply)
        val humidityParser = (string("NaN") || int).map {
          case Left(_)      => Humidity.Failed
          case Right(value) => Humidity.Measured(value)
        }
        val sampleParser = (idParser ~ humidityParser).map((Sample.apply _).tupled)

        ApplicativeError[F, Throwable].fromEither(
          sampleParser
            .parseOnly(line)
            .either
            .leftMap(_ => CorruptedCsv(line))
            .flatMap(s =>
              s.humidity match {
                case h @ Humidity.Measured(value) if value < 0 || value > 100 => HumidityOutOfRange(h).asLeft
                case _                                                        => s.asRight
              }
            )
        )
      }

    }

}
