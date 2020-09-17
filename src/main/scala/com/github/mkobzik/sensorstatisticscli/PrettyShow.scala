package com.github.mkobzik.sensorstatisticscli

import simulacrum.typeclass

@typeclass trait PrettyShow[A] {
  def prettyShow(a: A): String
}

object PrettyShow {

  def prettyShow[A](f: A => String): PrettyShow[A] = (a: A) => f(a)

}
