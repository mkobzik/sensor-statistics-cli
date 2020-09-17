package com.github.mkobzik.sensorstatisticscli

import cats.Id
import com.github.mkobzik.sensorstatisticscli.SensorStatistics.FileIndex
import com.github.mkobzik.sensorstatisticscli.models.Sensor.{AvgHumidity, MaxHumidity, MinHumidity}
import com.github.mkobzik.sensorstatisticscli.models.{Humidity, Sample, Sensor, Statistics}
import munit.{FunSuite, Location}

class SensorStatisticsProcessorSpec extends FunSuite {

  private val sensorStatisticsProcessor = SensorStatisticsProcessor.instance[Id]

  check(
    "Calculating number of files",
    List(
      (Sample(Sensor.Id(1), Humidity.Measured(10)), 0L),
      (Sample(Sensor.Id(1), Humidity.Measured(10)), 0L),
      (Sample(Sensor.Id(1), Humidity.Measured(30)), 2L),
      (Sample(Sensor.Id(1), Humidity.Measured(20)), 1L)
    ),
    obtainedStatistics => assertEquals(obtainedStatistics.numberOfProcessedFiles.value, 3L)
  )

  check(
    "Calculating number of measurements",
    List(
      (Sample(Sensor.Id(1), Humidity.Failed), 0L),
      (Sample(Sensor.Id(2), Humidity.Measured(30)), 2L),
      (Sample(Sensor.Id(3), Humidity.Failed), 1L),
      (Sample(Sensor.Id(3), Humidity.Measured(20)), 1L)
    ),
    obtainedStatistics => {
      assertEquals(obtainedStatistics.numberOfProcessedMeasurements.value, 4L)
      assertEquals(obtainedStatistics.numberOfFailedMeasurements.value, 2L)
    }
  )

  check(
    "Sorting sensors by max avg",
    List(
      (Sample(Sensor.Id(1), Humidity.Failed), 0L),
      (Sample(Sensor.Id(2), Humidity.Measured(30)), 2L),
      (Sample(Sensor.Id(3), Humidity.Failed), 1L),
      (Sample(Sensor.Id(3), Humidity.Measured(20)), 1L)
    ),
    obtainedStatistics => assertEquals(obtainedStatistics.sensors.map(_.id), List(Sensor.Id(2), Sensor.Id(3), Sensor.Id(1)))
  )

  check(
    "Calculating min humidity",
    List(
      (Sample(Sensor.Id(1), Humidity.Failed), 0L),
      (Sample(Sensor.Id(2), Humidity.Measured(30)), 2L),
      (Sample(Sensor.Id(2), Humidity.Measured(10)), 2L),
      (Sample(Sensor.Id(3), Humidity.Failed), 1L),
      (Sample(Sensor.Id(3), Humidity.Measured(20)), 1L)
    ),
    obtainedStatistics =>
      assertEquals(
        obtainedStatistics.sensors.sortBy(_.id.value).map(s => s.id -> s.minHumidity),
        List(
          Sensor.Id(1) -> MinHumidity(Humidity.Failed),
          Sensor.Id(2) -> MinHumidity(Humidity.Measured(10)),
          Sensor.Id(3) -> MinHumidity(Humidity.Measured(20))
        )
      )
  )

  check(
    "Calculating max humidity",
    List(
      (Sample(Sensor.Id(1), Humidity.Failed), 0L),
      (Sample(Sensor.Id(2), Humidity.Measured(30)), 2L),
      (Sample(Sensor.Id(2), Humidity.Measured(50)), 2L),
      (Sample(Sensor.Id(3), Humidity.Failed), 1L),
      (Sample(Sensor.Id(3), Humidity.Measured(20)), 1L)
    ),
    obtainedStatistics =>
      assertEquals(
        obtainedStatistics.sensors.sortBy(_.id.value).map(s => s.id -> s.maxHumidity),
        List(
          Sensor.Id(1) -> MaxHumidity(Humidity.Failed),
          Sensor.Id(2) -> MaxHumidity(Humidity.Measured(50)),
          Sensor.Id(3) -> MaxHumidity(Humidity.Measured(20))
        )
      )
  )

  check(
    "Calculating avg humidity",
    List(
      (Sample(Sensor.Id(1), Humidity.Failed), 0L),
      (Sample(Sensor.Id(2), Humidity.Measured(30)), 2L),
      (Sample(Sensor.Id(2), Humidity.Measured(20)), 2L),
      (Sample(Sensor.Id(3), Humidity.Failed), 1L),
      (Sample(Sensor.Id(3), Humidity.Measured(20)), 1L)
    ),
    obtainedStatistics =>
      assertEquals(
        obtainedStatistics.sensors.sortBy(_.id.value).map(s => s.id -> s.avgHumidity),
        List(
          Sensor.Id(1) -> AvgHumidity(Humidity.Failed),
          Sensor.Id(2) -> AvgHumidity(Humidity.Measured(25)),
          Sensor.Id(3) -> AvgHumidity(Humidity.Measured(20))
        )
      )
  )

  private def check(name: String, samples: List[(Sample, FileIndex)], assertions: Statistics => Unit)(implicit loc: Location): Unit =
    test(name) {
      fs2.Stream
        .emits(samples)
        .through(sensorStatisticsProcessor.createStatistics)
        .map(assertions)
        .compile
        .drain
    }

}
