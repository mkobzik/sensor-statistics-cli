package com.github.mkobzik.sensorstatisticscli

import cats.Show
import cats.implicits.showInterpolator
import cats.kernel.Order
import cats.syntax.all._
import com.github.mkobzik.sensorstatisticscli.models.Sensor.{AvgHumidity, Id, MaxHumidity, MinHumidity}
import com.github.mkobzik.sensorstatisticscli.models.Statistics.{
  NumberOfFailedMeasurements,
  NumberOfProcessedFiles,
  NumberOfProcessedMeasurements
}
import io.estatico.newtype.macros.newtype

object models {

  final case class Sensor(id: Id, minHumidity: MinHumidity, avgHumidity: AvgHumidity, maxHumidity: MaxHumidity)

  object Sensor {
    @newtype final case class Id(value: Long)
    @newtype final case class MinHumidity(value: Humidity)
    @newtype final case class AvgHumidity(value: Humidity)
    @newtype final case class MaxHumidity(value: Humidity)

    implicit val sensorShow: Show[Sensor] =
      Show.show(sensor => show"${sensor.id.value},${sensor.minHumidity.value},${sensor.avgHumidity.value},${sensor.maxHumidity.value}")

  }

  sealed trait Humidity extends Product with Serializable

  object Humidity {
    final case class Measured(value: Double) extends Humidity
    final case object Failed extends Humidity

    implicit val humidityPrettyShow: Show[Humidity] = Show.show[Humidity] {
      case Measured(value) => value.toString
      case Failed          => "NaN"
    }

    implicit val humidityOrder: Order[Humidity] = Order.from {
      case (Measured(_), Failed)        => 1
      case (Failed, Measured(_))        => -1
      case (Measured(v0), Measured(v1)) => v0.compareTo(v1)
      case (Failed, Failed)             => 0
    }

  }

  final case class Sample(sensorId: Sensor.Id, humidity: Humidity)

  final case class Statistics(
      numberOfProcessedFiles: NumberOfProcessedFiles,
      numberOfProcessedMeasurements: NumberOfProcessedMeasurements,
      numberOfFailedMeasurements: NumberOfFailedMeasurements,
      sensors: List[Sensor]
  )

  object Statistics {
    @newtype final case class NumberOfProcessedFiles(value: Long)
    @newtype final case class NumberOfProcessedMeasurements(value: Long)
    @newtype final case class NumberOfFailedMeasurements(value: Long)

    implicit val statisticsShow: Show[Statistics] = Show.show(statistics => show"""
        |Number of processed files: ${statistics.numberOfProcessedFiles.value}
        |Number of processed measurements: ${statistics.numberOfProcessedMeasurements.value}
        |Number of failed measurements: ${statistics.numberOfFailedMeasurements.value}

        |Sensors with highest avg humidity:

        |sensor-id,min,avg,max
        |${statistics.sensors.map(_.show).mkString("\n")}
      """.stripMargin)

  }

}
