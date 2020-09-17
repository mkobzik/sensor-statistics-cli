package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, IO}
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
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles, 1L)
  )

  check(
    "Parsing multiple files",
    "/testcases/parsingmultiplefiles",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles, 2L)
  )

  checkFailing[RuntimeException](
    "Parsing corrupted file",
    "/testcases/parsingcorruptedfile"
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
