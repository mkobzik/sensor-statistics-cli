package com.github.mkobzik.sensorstatisticscli

import cats.Show
import cats.implicits.showInterpolator
import cats.kernel.Order
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

  @newtype final case class SumHumidity(value: Humidity)

  object SumHumidity {
    implicit val sumHumidityShow: Show[SumHumidity] = Show.show(sumHumidity => show"Sum${sumHumidity.value}")
  }

  sealed trait Humidity extends Product with Serializable

  object Humidity {
    final case class Measured(value: Int) extends Humidity
    final case object Failed extends Humidity

    implicit val humidityShow: Show[Humidity] = Show.show[Humidity] {
      case Measured(value) => s"Humidity.Measured($value)"
      case Failed          => "Humidity.Failed"
    }

    implicit val humidityOrder: Order[Humidity] = Order.from {
      case (Measured(_), Failed)        => 1
      case (Failed, Measured(_))        => -1
      case (Measured(v0), Measured(v1)) => v0.compareTo(v1)
      case (Failed, Failed)             => 0
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
