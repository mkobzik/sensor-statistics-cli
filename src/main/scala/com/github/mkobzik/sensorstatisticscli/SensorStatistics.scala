package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import cats.effect.{Blocker, ContextShift, Sync}
import cats.tagless.finalAlg
import com.github.mkobzik.sensorstatisticscli.models._
import fs2.{Pipe, text}

@finalAlg
trait SensorStatistics[F[_]] {
  def calculate(dailyReportsDir: Path): F[Statistics]
}

object SensorStatistics {

  implicit def instance[F[_]: SensorStatisticsProcessor: Sync: ContextShift](blocker: Blocker): SensorStatistics[F] =
    new SensorStatistics[F] {

      override def calculate(dailyReportsDir: Path): F[Statistics] =
        fs2.io.file
          .directoryStream(blocker, dailyReportsDir)
          .flatMap { report =>
            fs2.io.file
              .readAll(report, blocker, 4096)
              .through(parseCsv)
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
        val Array(rawId, rawHumidity) = line.split(",")
        Sample(
          Sensor.Id(rawId.toInt),
          rawHumidity match {
            case "NaN" => Humidity.Failed
            case x     => Humidity.Measured(x.toInt)
          }
        )
      }

    }

}
