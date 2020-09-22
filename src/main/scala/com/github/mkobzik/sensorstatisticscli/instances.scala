package com.github.mkobzik.sensorstatisticscli

import cats.Show
import eu.timepit.refined.api.Refined

object instances {

  object cats extends CatsInstances

  trait CatsInstances {

    implicit def refinedShow[T, P](implicit ev: Show[T]): Show[T Refined P] = Show.show(t => ev.show(t.value))

  }

}
