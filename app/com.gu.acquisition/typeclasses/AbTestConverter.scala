package com.gu.acquisition.typeclasses

import ophan.thrift.event.AbTest
import simulacrum.typeclass

@typeclass trait AbTestConverter[A] {
  def asAbTest(a: A): AbTest
}

object AbTestConverter {
  def instance[A](f: A => AbTest): AbTestConverter[A] = new AbTestConverter[A] {
    override def asAbTest(a: A): AbTest = f(a)
  }
}