package com.github.mkobzik.sensorstatisticscli

import cats.Show
import eu.timepit.refined.api.Refined

object instances {

  object refined extends RefinedInstances

  trait RefinedInstances {

    implicit def refinedShow[T, P](implicit ev: Show[T]): Show[T Refined P] = Show.show(t => ev.show(t.value))

  }

}
