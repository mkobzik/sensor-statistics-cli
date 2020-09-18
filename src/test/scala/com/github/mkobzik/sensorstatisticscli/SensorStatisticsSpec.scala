package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, IO}
import com.github.mkobzik.sensorstatisticscli.errors.Error.{CorruptedCsv, HumidityOutOfRange, NotADirectory}
import com.github.mkobzik.sensorstatisticscli.models.Statistics
import munit.{FunSuite, Location}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class SensorStatisticsSpec extends FunSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  private val makeSensorStatistics = Blocker[IO].map(blocker => SensorStatistics.instance[IO](blocker))

  check(
    "Parsing single file",
    "/testcases/parsingsinglefile",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value, 1L)
  )

  check(
    "Parsing multiple files",
    "/testcases/parsingmultiplefiles",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value, 2L)
  )

  check(
    "Parsing only csv files",
    "/testcases/parsingonlycsvfiles",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value, 1L)
  )

  checkFailing[CorruptedCsv](
    "Parsing corrupted file",
    "/testcases/parsingcorruptedfile"
  )

  checkFailing[HumidityOutOfRange](
    "Parsing file with wrong humidity value",
    "/testcases/parsingfilewithwronghumidity"
  )

  checkFailing[NotADirectory](
    "Not a directory path",
    "/testcases/parsingsinglefile/file0.csv"
  )

  private def check(name: String, testCasePath: String, assertions: Statistics => Unit)(implicit loc: Location): Unit =
    test(name) {
      makeSensorStatistics
        .evalMap(_.calculate(Paths.get(getClass.getResource(testCasePath).toURI)))
        .map(assertions)
        .use(_ => IO.unit)
        .unsafeRunSync()
    }

  private def checkFailing[E <: Throwable: ClassTag](name: String, testCasePath: String)(implicit loc: Location): Unit =
    test(name) {
      intercept[E] {
        makeSensorStatistics
          .evalMap(_.calculate(Paths.get(getClass.getResource(testCasePath).toURI)))
          .use(_ => IO.unit)
          .unsafeRunSync()
      }
    }

}
