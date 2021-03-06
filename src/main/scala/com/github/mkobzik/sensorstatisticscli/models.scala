package com.github.mkobzik.sensorstatisticscli

import cats.Show
import cats.implicits.showInterpolator
import cats.kernel.{Eq, Order}
import cats.syntax.all._
import com.github.mkobzik.sensorstatisticscli.instances.CatsInstances
import eu.timepit.refined.W
import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.types.numeric.NonNegLong
import io.estatico.newtype.macros.newtype

object models extends CatsInstances {

  final case class Sensor(
      id: Sensor.Id,
      numberOfProcessedMeasurements: Sensor.NumberOfProcessedMeasurements,
      minHumidity: Sensor.MinHumidity,
      avgHumidity: Sensor.AvgHumidity,
      maxHumidity: Sensor.MaxHumidity
  )

  object Sensor {
    @newtype final case class Id(value: Long)

    object Id {
      implicit val idEq: Eq[Id] = Eq.by(_.value)
    }

    @newtype final case class NumberOfProcessedMeasurements(value: NonNegLong)
    @newtype final case class MinHumidity(value: Humidity)
    @newtype final case class AvgHumidity(value: Humidity)
    @newtype final case class MaxHumidity(value: Humidity)

    implicit val sensorShow: Show[Sensor] =
      Show.show(sensor => show"${sensor.id.value},${sensor.minHumidity.value},${sensor.avgHumidity.value},${sensor.maxHumidity.value}")

  }

  sealed trait Humidity extends Product with Serializable

  object Humidity {
    type ZeroToHundred = Interval.Closed[W.`0.0`.T, W.`100.0`.T]
    object ZeroToHundred extends RefinedTypeOps[Double Refined ZeroToHundred, Double]

    final case class Measured(value: Double Refined ZeroToHundred) extends Humidity
    final case object Failed extends Humidity

    implicit val humidityPrettyShow: Show[Humidity] = Show.show[Humidity] {
      case Measured(value) => value.toInt.toString
      case Failed          => "NaN"
    }

    implicit val humidityOrder: Order[Humidity] = Order.from {
      case (Measured(_), Failed)        => 1
      case (Failed, Measured(_))        => -1
      case (Measured(v0), Measured(v1)) => v0.value.compareTo(v1.value)
      case (Failed, Failed)             => 0
    }

  }

  final case class Sample(sensorId: Sensor.Id, humidity: Humidity)

  final case class Statistics(
      numberOfProcessedFiles: Statistics.NumberOfProcessedFiles,
      numberOfProcessedMeasurements: Statistics.NumberOfProcessedMeasurements,
      numberOfFailedMeasurements: Statistics.NumberOfFailedMeasurements,
      sensors: List[Sensor]
  )

  object Statistics {
    @newtype final case class NumberOfProcessedFiles(value: NonNegLong)
    @newtype final case class NumberOfProcessedMeasurements(value: NonNegLong)
    @newtype final case class NumberOfFailedMeasurements(value: NonNegLong)

    implicit val statisticsShow: Show[Statistics] = Show.show(statistics => show"""
        |Number of processed files: ${statistics.numberOfProcessedFiles.value}
        |Number of processed measurements: ${statistics.numberOfProcessedMeasurements.value}
        |Number of failed measurements: ${statistics.numberOfFailedMeasurements.value}

        |Sensors with highest avg humidity:

        |\u001b[4msensor-id,min,avg,max\u001b[0m
        |${statistics.sensors.map(_.show).mkString("\n")}
      """.stripMargin)

  }

}
