package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import com.github.mkobzik.sensorstatisticscli.models.Humidity

object errors {

  sealed trait Error extends Throwable

  object Error {
    final case class CorruptedCsv(line: String) extends Error
    final case class HumidityOutOfRange(humidity: Humidity.Measured) extends Error
    final case class NotADirectory(path: Path) extends Error
  }

}
