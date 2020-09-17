package com.github.mkobzik.sensorstatisticscli

import cats.kernel.Order
import cats.syntax.all._
import cats.tagless.finalAlg
import com.github.mkobzik.sensorstatisticscli.SensorStatistics.FileIndex
import com.github.mkobzik.sensorstatisticscli.models.Sensor.{AvgHumidity, MaxHumidity, MinHumidity}
import com.github.mkobzik.sensorstatisticscli.models.Statistics.{
  NumberOfFailedMeasurements,
  NumberOfProcessedFiles,
  NumberOfProcessedMeasurements
}
import com.github.mkobzik.sensorstatisticscli.models._
import fs2.Pipe

@finalAlg
trait SensorStatisticsProcessor[F[_]] {
  def createStatistics: Pipe[F, (Sample, FileIndex), Statistics]
}

object SensorStatisticsProcessor {

  implicit def instance[F[_]]: SensorStatisticsProcessor[F] = new SensorStatisticsProcessor[F] {

    override def createStatistics: Pipe[F, (Sample, FileIndex), Statistics] =
      _.fold(
        (
          NumberOfProcessedFiles(0L),
          NumberOfProcessedMeasurements(0L),
          NumberOfFailedMeasurements(0L),
          Map.empty[Sensor.Id, (Int, SumHumidity, MinHumidity, MaxHumidity)]
        )
      ) { case ((numberOfProcessedFiles, numberOfProcessedMeasurements, numberOfFailedMeasurements, sensors), (sample, fileIndex)) =>
        (
          NumberOfProcessedFiles(Order[Long].max(numberOfProcessedFiles.value, fileIndex + 1)),
          NumberOfProcessedMeasurements(numberOfProcessedMeasurements.value + 1),
          if (sample.humidity == Humidity.Failed) NumberOfFailedMeasurements(numberOfFailedMeasurements.value + 1)
          else numberOfFailedMeasurements,
          sensors + (sample.sensorId -> sensors
            .get(sample.sensorId)
            .map { case (n, sumHumidity, minHumidity, maxHumidity) =>
              (
                if (sample.humidity == Humidity.Failed) n else n + 1,
                SumHumidity(sumHumidity.value |+| sample.humidity),
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
          sensors.toList
            .map { case (id, (n, sumHumidity, minHumidity, maxHumidity)) =>
              Sensor(id, minHumidity, avgHumidity(n, sumHumidity), maxHumidity)
            }
            .sortBy(_.avgHumidity.value)(Order.reverse(Order[Humidity]).toOrdering)
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

    private def avgHumidity(n: Int, sumHumidity: SumHumidity) = AvgHumidity(sumHumidity.value match {
      case Humidity.Failed       => Humidity.Failed
      case Humidity.Measured(v0) => Humidity.Measured(v0 / n)
    })

  }

}
