package com.gu.acquisition.syntax

import com.gu.acquisition.typeclasses.AbTestConverter
import ophan.thrift.event.AbTestInfo

trait IterableSyntax {

  implicit def iterableSyntax[A](as: Iterable[A]): IterableOps[A] = new IterableOps[A](as)
}

final class IterableOps[A](val as: Iterable[A]) extends AnyVal {

  def asAbTestInfo(implicit converter: AbTestConverter[A]): AbTestInfo = {
    import AbTestConverter.ops._
    AbTestInfo(as.map(_.asAbTest).toSet)
  }
}
