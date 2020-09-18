package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import cats.Show
import com.github.mkobzik.sensorstatisticscli.models.Humidity

object errors {

  sealed trait Error extends Throwable

  object Error {
    final case class CorruptedCsv(line: String) extends Error
    final case class HumidityOutOfRange(humidity: Humidity.Measured) extends Error
    final case class NotADirectory(path: Path) extends Error

    implicit val errorShow: Show[Error] = Show.show(err =>
      "\u001b[31m" + (err match {
        case CorruptedCsv(line)           => s"Failed to parse cvs file. Corrupted line = '$line'"
        case HumidityOutOfRange(humidity) => s"Wrong humidity value. Humidity should be in range [0, 100]. Value = ${humidity.value}"
        case NotADirectory(path)          => s"Provided path does not point to directory. Path = '$path'"
      }) + "\u001b[0m"
    )

  }

}
