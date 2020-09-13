package com.github.mkobzik.sensorstatisticscli

import com.github.mkobzik.sensorstatisticscli.models.Sensor.Id
import io.estatico.newtype.macros.newtype

object models {

  final case class Sensor(id: Id)

  object Sensor {
    @newtype final case class Id(value: Int)
  }

  sealed trait Humidity

  object Humidity {
    final case class Measured(value: Int) extends Humidity
    final case object Failed extends Humidity
  }

  final case class Sample(sensor: Sensor, humidity: Humidity)

}
