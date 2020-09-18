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
import monocle.macros.syntax.lens._

@finalAlg
trait SensorStatisticsProcessor[F[_]] {
  def createStatistics: Pipe[F, (Sample, FileIndex), Statistics]
}

object SensorStatisticsProcessor {

  implicit def instance[F[_]]: SensorStatisticsProcessor[F] = new SensorStatisticsProcessor[F] {

    override def createStatistics: Pipe[F, (Sample, FileIndex), Statistics] = {
      _.fold(emptyStatistics) { case (statistics, (sample, fileIndex)) =>
        recalculateStatistics(statistics, sample, fileIndex)
      }
        .map(sortByAvgHumidityDesc)
    }

    private def recalculateStatistics(statistics: Statistics, sample: Sample, fileIndex: FileIndex) = {
      statistics
        .lens(_.numberOfProcessedFiles)
        .modify(nopf => NumberOfProcessedFiles(math.max(nopf.value, fileIndex + 1)))
        .lens(_.numberOfProcessedMeasurements)
        .modify(nopm => NumberOfProcessedMeasurements(nopm.value + 1))
        .lens(_.numberOfFailedMeasurements)
        .modify(nofm => if (sample.humidity =!= Humidity.Failed) nofm else NumberOfFailedMeasurements(nofm.value + 1))
        .lens(_.sensors)
        .modify(s =>
          s
            .find(_.id === sample.sensorId)
            .tupleRight(sample)
            .map((updateSensor _).tupled)
            .getOrElse(sensorFrom(sample)) :: s.filterNot(_.id === sample.sensorId)
        )
    }

    private def updateSensor(sensor: Sensor, sample: Sample) = {
      sensor
        .lens(_.minHumidity)
        .modify(min => minHumidity(min, sample.humidity))
        .lens(_.maxHumidity)
        .modify(max => maxHumidity(max, sample.humidity))
        .lens(_.avgHumidity)
        .modify(avg => avgHumidity(avg, sample.humidity, sensor.numberOfProcessedMeasurements.value + 1))
        .lens(_.numberOfProcessedMeasurements)
        .modify(nopm => if (sample.humidity === Humidity.Failed) nopm else Sensor.NumberOfProcessedMeasurements(nopm.value + 1))
    }

    private def sortByAvgHumidityDesc(statistics: Statistics): Statistics = {
      statistics.lens(_.sensors).modify(_.sortBy(_.avgHumidity.value)(Order.reverse(Order[Humidity]).toOrdering))
    }

    private def minHumidity(minHumidity: MinHumidity, humidity: Humidity) = MinHumidity(
      (ignoreFailed orElse overrideFailed).applyOrElse((minHumidity.value, humidity), (Order[Humidity].min _).tupled)
    )

    private def maxHumidity(minHumidity: MaxHumidity, humidity: Humidity) = MaxHumidity(
      (ignoreFailed orElse overrideFailed).applyOrElse((minHumidity.value, humidity), (Order[Humidity].max _).tupled)
    )

    private def avgHumidity(avgHumidity: AvgHumidity, humidity: Humidity, n: Long) = {
      val calculateAvg: PartialFunction[(Humidity, Humidity), Humidity] = {
        case (Humidity.Measured(currentValue), Humidity.Measured(newValue)) =>
          Humidity.Measured(currentValue + (newValue - currentValue) / n)
      }

      AvgHumidity((ignoreFailed orElse overrideFailed orElse calculateAvg)((avgHumidity.value, humidity)))
    }

    private val ignoreFailed: PartialFunction[(Humidity, Humidity), Humidity] = { case (h, Humidity.Failed) => h }
    private val overrideFailed: PartialFunction[(Humidity, Humidity), Humidity] = { case (Humidity.Failed, m: Humidity.Measured) => m }

    private def sensorFrom(sample: Sample) = Sensor(
      sample.sensorId,
      Sensor.NumberOfProcessedMeasurements(if (sample.humidity == Humidity.Failed) 0 else 1),
      Sensor.MinHumidity(sample.humidity),
      Sensor.AvgHumidity(sample.humidity),
      Sensor.MaxHumidity(sample.humidity)
    )

    private val emptyStatistics =
      Statistics(NumberOfProcessedFiles(0L), NumberOfProcessedMeasurements(0L), NumberOfFailedMeasurements(0L), List.empty)

  }

}
