package com.github.mkobzik.sensorstatisticscli

import cats.Show
import cats.implicits.showInterpolator
import com.github.mkobzik.sensorstatisticscli.models.Sensor.{AvgHumidity, Id, MaxHumidity, MinHumidity}
import io.estatico.newtype.macros.newtype

object models {

  final case class Sensor(id: Id, minHumidity: MinHumidity, avgHumidity: AvgHumidity, maxHumidity: MaxHumidity)

  object Sensor {
    @newtype final case class Id(value: Int)

    object Id {
      implicit val idShow: Show[Id] = Show.show(id => s"Sensor.Id($id)")
    }

    @newtype final case class MinHumidity(value: Humidity)

    object MinHumidity {
      implicit val minHumidityShow: Show[MinHumidity] = Show.show(minHumidity => show"Min${minHumidity.value}")
    }

    @newtype final case class AvgHumidity(value: Humidity)

    object AvgHumidity {
      implicit val avgHumidityShow: Show[AvgHumidity] = Show.show(avgHumidity => show"Avg${avgHumidity.value}")
    }

    @newtype final case class MaxHumidity(value: Humidity)

    object MaxHumidity {
      implicit val maxHumidityShow: Show[MaxHumidity] = Show.show(maxHumidity => show"Max${maxHumidity.value}")
    }

    implicit val sensorShow: Show[Sensor] =
      Show.show(sensor => show"Sensor(${sensor.id}, ${sensor.minHumidity}, ${sensor.avgHumidity}, ${sensor.maxHumidity})")

  }

  sealed trait Humidity

  object Humidity {
    final case class Measured(value: Int) extends Humidity
    final case object Failed extends Humidity

    implicit val humidityShow: Show[Humidity] = Show.show[Humidity] {
      case Measured(value) => s"Humidity.Measured($value)"
      case Failed          => "Humidity.Failed"
    }

  }

  final case class Sample(sensorId: Sensor.Id, humidity: Humidity)

  object Sample {
    implicit val sampleShow: Show[Sample] = Show.show(sample => show"Sample(${sample.sensorId}, ${sample.humidity})")
  }

  final case class Statistics(
      numberOfProcessedFiles: Int,
      numberOfProcessedMeasurements: Int,
      numberOfFailedMeasurements: Int,
      sensors: List[Sensor]
  )

  object Statistics {

    implicit val statisticsShow: Show[Statistics] = Show.show(statistics =>
      "Statistics(" +
        show"${statistics.numberOfProcessedFiles}, " +
        show"${statistics.numberOfProcessedMeasurements}, " +
        show"${statistics.numberOfFailedMeasurements}, " +
        show"${statistics.sensors})"
    )

  }

}
