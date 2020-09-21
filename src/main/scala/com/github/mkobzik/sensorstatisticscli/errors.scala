package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import cats.Show

object errors {

  sealed trait Error extends Throwable

  object Error {
    final case class CorruptedCsv(line: String) extends Error
    final case class NotADirectory(path: Path) extends Error

    implicit val errorShow: Show[Error] = Show.show(err =>
      "\u001b[31m" + (err match {
        case CorruptedCsv(line)  => s"Failed to parse cvs file. Corrupted line = '$line'"
        case NotADirectory(path) => s"Provided path does not point to directory. Path = '$path'"
      }) + "\u001b[0m"
    )

  }

}
