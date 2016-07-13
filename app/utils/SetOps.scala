package utils

object SetOps {
  implicit class SetOperations[A](in: Set[A]) {
    def containsAll(all: A*) =
      in.intersect(all.toSet) == all.toSet
    def containsAny(any: A*) =
      any.exists(in.contains)
    def containsAny(any: TraversableOnce[A]) =
      any.exists(in.contains)
    def containsNone(any: A*) =
      !any.forall(in.contains)
  }
}
