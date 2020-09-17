package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import atto.Atto._
import cats.effect.{Blocker, ContextShift, Sync}
import cats.tagless.finalAlg
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

      override def calculate(dailyReportDir: Path): F[Statistics] =
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

      private def parseCsv: Pipe[F, Byte, Sample] =
        _.through(text.utf8Decode)
          .through(text.lines)
          .drop(1) //header
          .map(lineToSample)

      private def lineToSample(line: String): Sample = {
        val idParser = (long <~ char(',')).map(Sensor.Id.apply)
        val humidityParser = (string("NaN") || int).map {
          case Left(_)      => Humidity.Failed
          case Right(value) => Humidity.Measured(value)
        }
        val sampleParser = (idParser ~ humidityParser).map((Sample.apply _).tupled)

        sampleParser
          .parseOnly(line)
          .either
          .getOrElse(
            throw new RuntimeException(
              s"Failed to parse line: '$line'. Line should be in format '<sensor_id:long>,<humidity:{NaN,[0-100]}>'"
            )
          )
      }

    }

}
