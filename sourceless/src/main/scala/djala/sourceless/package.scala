package djala

import cats.data.Ior
import shapeless._
import shapeless.syntax.std.traversable._

package object sourceless {
  type StringIor[A] = Ior[String, A]
  type ErrorOr[A] = Either[Throwable, A]


  implicit class Listable[A[_[_]], LUB[_], L <: HList](a: A[LUB])(implicit g: Generic.Aux[A[LUB], L]) {
    def toList(implicit tt: shapeless.ops.hlist.ToTraversable.Aux[L,List,LUB[_]]): List[LUB[_]] = g.to(a).toList
  }


  case class ToCC[A[_[_]], F[_]]() {
    def listToCC[L <: HList]
    (l: List[F[_]])(implicit
                    g: Generic.Aux[A[F], L],
                    ft: shapeless.ops.traversable.FromTraversable[L])
    : Option[A[F]]
    = l.toHList map g.from
  }
}
