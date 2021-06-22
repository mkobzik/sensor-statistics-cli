package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Path

import cats.ApplicativeError
import cats.effect.Sync
import cats.syntax.all._
import cats.tagless.finalAlg
import com.github.mkobzik.sensorstatisticscli.errors.Error.CorruptedCsv
import com.github.mkobzik.sensorstatisticscli.errors.Error.NotADirectory
import com.github.mkobzik.sensorstatisticscli.models.Humidity.ZeroToHundred
import com.github.mkobzik.sensorstatisticscli.models._
import fastparse.NoWhitespace._
import fastparse._
import fs2.Pipe
import fs2.text
import cats.effect.kernel.Async
import fs2.io.file.Files

@finalAlg
trait SensorStatistics[F[_]] {
  def calculate(dailyReportDir: Path): F[Statistics]
}

object SensorStatistics {

  type FileIndex = Long

  implicit def instance[F[_]: SensorStatisticsProcessor: Async]: SensorStatistics[F] =
    new SensorStatistics[F] {

      override def calculate(dailyReportDir: Path): F[Statistics] =
        isDirectory(dailyReportDir) *>
          Files[F]
            .directoryStream(dailyReportDir, _.toFile.getName.endsWith(".csv"))
            .zipWithIndex
            .flatMap {
              case (report, fileIndex) =>
                Files[F]
                  .readAll(report, 4096)
                  .through(parseCsv)
                  .map(sample => sample -> fileIndex)
            }
            .through(SensorStatisticsProcessor[F].createStatistics)
            .compile
            .lastOrError

      private def isDirectory(dailyReportDir: Path): F[Unit] =
        Sync[F].ifM(Sync[F].delay(dailyReportDir.toFile.isDirectory))(
          ifTrue = Sync[F].unit,
          ifFalse = Sync[F].raiseError(NotADirectory(dailyReportDir))
        )

      private def parseCsv: Pipe[F, Byte, Sample] =
        _.through(text.utf8Decode)
          .through(text.lines)
          .drop(1) //header
          .evalMap(lineToSample)

      private def lineToSample(line: String): F[Sample] = {
        def sensorIdParser[_: P] = P(CharIn("0-9").rep(1).!.map(v => Sensor.Id(v.toLong)))
        def commaParser[_: P] = P(",")
        def humidityParser[_: P] =
          P("NaN".!.map(_ => Humidity.Failed) | CharIn("0-9").rep(1).!.map(v => Humidity.Measured(ZeroToHundred.unsafeFrom(v.toDouble))))

        def lineParser[_: P] = P(sensorIdParser ~ commaParser ~ humidityParser ~ End)

        ApplicativeError[F, Throwable].fromEither(
          parse(line, lineParser(_)).fold(
            onFailure = (_, _, _) => CorruptedCsv(line).asLeft,
            onSuccess = { case ((sensorId, humidity), _) => Sample(sensorId, humidity).asRight }
          )
        )
      }

    }

}
