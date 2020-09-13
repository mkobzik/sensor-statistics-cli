package com.github.mkobzik.sensorstatisticscli

import cats.Show
import cats.implicits.showInterpolator
import com.github.mkobzik.sensorstatisticscli.models.Sensor.Id
import io.estatico.newtype.macros.newtype

object models {

  final case class Sensor(id: Id)

  object Sensor {
    @newtype final case class Id(value: Int)

    object Id {
      implicit val showId: Show[Id] = Show.show(id => s"Sensor.Id($id)")
    }

    implicit val showSensor: Show[Sensor] = Show.show(sensor => show"Sensor(${sensor.id})")

  }

  sealed trait Humidity

  object Humidity {
    final case class Measured(value: Int) extends Humidity
    final case object Failed extends Humidity

    implicit val showHumidity: Show[Humidity] = Show.show[Humidity] {
      case Measured(value) => s"Humidity.Measured($value)"
      case Failed          => "Humidity.Failed"
    }

  }

  final case class Sample(sensor: Sensor, humidity: Humidity)

  object Sample {
    implicit val showSample: Show[Sample] = Show.show(sample => show"Sample(${sample.sensor}, ${sample.humidity})")
  }

}
