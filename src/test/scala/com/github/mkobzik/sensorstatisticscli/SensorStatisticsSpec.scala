package com.github.mkobzik.sensorstatisticscli

import java.nio.file.Paths

import cats.effect.IO
import com.github.mkobzik.sensorstatisticscli.errors.Error.CorruptedCsv
import com.github.mkobzik.sensorstatisticscli.errors.Error.NotADirectory
import com.github.mkobzik.sensorstatisticscli.models.Statistics
import munit.Location

import scala.reflect.ClassTag
import munit.CatsEffectSuite

class SensorStatisticsSpec extends CatsEffectSuite {

  check(
    "Parsing single file",
    "/testcases/parsingsinglefile",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value.value, 1L)
  )

  check(
    "Parsing multiple files",
    "/testcases/parsingmultiplefiles",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value.value, 2L)
  )

  check(
    "Parsing only csv files",
    "/testcases/parsingonlycsvfiles",
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value.value, 1L)
  )

  checkFailing[CorruptedCsv](
    "Parsing corrupted file",
    "/testcases/parsingcorruptedfile"
  )

  checkFailing[NotADirectory](
    "Not a directory path",
    "/testcases/parsingsinglefile/file0.csv"
  )

  private val sensorStatictics = SensorStatistics.instance[IO]

  private def check(name: String, testCasePath: String, assertions: Statistics => Unit)(implicit loc: Location): Unit =
    test(name) {
      sensorStatictics
        .calculate(Paths.get(getClass.getResource(testCasePath).toURI))
        .map(assertions)
    }

  private def checkFailing[E <: Throwable: ClassTag](name: String, testCasePath: String)(implicit loc: Location): Unit =
    test(name) {
      interceptIO[E] {
        sensorStatictics
          .calculate(Paths.get(getClass.getResource(testCasePath).toURI))
      }
    }

}
