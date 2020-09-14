package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import cats.effect.{Blocker, ContextShift, Sync}
import cats.kernel.Order
import cats.tagless.finalAlg
import com.github.mkobzik.sensorstatisticscli.models.{Humidity, Sample, Sensor, Statistics, SumHumidity}
import fs2.{Pipe, text}
import cats.syntax.all._
import com.github.mkobzik.sensorstatisticscli.models.Sensor.{AvgHumidity, MaxHumidity, MinHumidity}

@finalAlg
trait SensorStatistics[F[_]] {
  def calculate(dailyReportsDir: Path): F[Statistics]
}

object SensorStatistics {

  implicit def instance[F[_]: Sync: ContextShift](blocker: Blocker): SensorStatistics[F] = new SensorStatistics[F] {

    override def calculate(dailyReportsDir: Path): F[Statistics] =
      fs2.io.file
        .directoryStream(blocker, dailyReportsDir)
        .flatMap { report =>
          fs2.io.file
            .readAll(report, blocker, 4096)
            .through(parseCsv)
        }
        .through(createStatistics)
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

    private def createStatistics: Pipe[F, Sample, Statistics] =
      _.fold((0, 0, 0, Map.empty[Sensor.Id, (Int, SumHumidity, MinHumidity, MaxHumidity)])) {
        case ((numberOfProcessedFiles, numberOfProcessedMeasurements, numberOfFailedMeasurements, sensors), sample) =>
          (
            numberOfProcessedFiles,
            numberOfProcessedMeasurements + 1,
            if (sample.humidity == Humidity.Failed) numberOfFailedMeasurements + 1 else numberOfFailedMeasurements,
            sensors + (sample.sensorId -> sensors
              .get(sample.sensorId)
              .map { case (n, sumHumidity, minHumidity, maxHumidity) =>
                (
                  if (sample.humidity == Humidity.Failed) n else n + 1,
                  this.sumHumidity(sumHumidity, sample.humidity),
                  this.minHumidity(minHumidity, sample.humidity),
                  this.maxHumidity(maxHumidity, sample.humidity)
                )
              }
              .getOrElse(
                (
                  if (sample.humidity == Humidity.Failed) 0 else 1,
                  SumHumidity(sample.humidity),
                  MinHumidity(sample.humidity),
                  MaxHumidity(sample.humidity)
                )
              ))
          )
      }.map { case (numberOfProcessedFiles, numberOfProcessedMeasurements, numberOfFailedMeasurements, sensors) =>
        (
          numberOfProcessedFiles,
          numberOfProcessedMeasurements,
          numberOfFailedMeasurements,
          sensors.toList.map { case (id, (n, sumHumidity, minHumidity, maxHumidity)) =>
            Sensor(id, minHumidity, avgHumidity(n, sumHumidity), maxHumidity)
          }
        )
      }.map((Statistics.apply _).tupled)

    private def minHumidity(minHumidity: MinHumidity, humidity: Humidity) = MinHumidity((minHumidity.value, humidity) match {
      case (currentAny, Humidity.Failed)                                        => currentAny
      case (Humidity.Failed, newMeasured: Humidity.Measured)                    => newMeasured
      case (currentMeasured: Humidity.Measured, newMeasured: Humidity.Measured) => Order[Humidity].min(currentMeasured, newMeasured)
    })

    private def maxHumidity(maxHumidity: MaxHumidity, humidity: Humidity) = MaxHumidity((maxHumidity.value, humidity) match {
      case (currentAny, Humidity.Failed)                                        => currentAny
      case (Humidity.Failed, newMeasured: Humidity.Measured)                    => newMeasured
      case (currentMeasured: Humidity.Measured, newMeasured: Humidity.Measured) => Order[Humidity].max(currentMeasured, newMeasured)
    })

    private def sumHumidity(sumHumidity: SumHumidity, humidity: Humidity) = SumHumidity((sumHumidity.value, humidity) match {
      case (currentAny, Humidity.Failed)                     => currentAny
      case (Humidity.Failed, newMeasured: Humidity.Measured) => newMeasured
      case (Humidity.Measured(v0), Humidity.Measured(v1))    => Humidity.Measured(v0 + v1)
    })

    private def avgHumidity(n: Int, sumHumidity: SumHumidity) = AvgHumidity(sumHumidity.value match {
      case Humidity.Failed       => Humidity.Failed
      case Humidity.Measured(v0) => Humidity.Measured(v0 / n)
    })

  }

}
