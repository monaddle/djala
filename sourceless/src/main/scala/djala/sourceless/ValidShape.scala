package djala.sourceless

import djala.sourceless.fquery.{DBAttr, DBTableAttribute}
import shapeless.{Generic, HList, UnaryTCConstraint}
import shapeless.ops.hlist.ToTraversable

trait ValidShape[A] {
  def toList(a: A): List[DBAttr[_]]
}

object ValidShape {
  def apply[A](implicit vs: ValidShape[A]): ValidShape[A] = vs

  implicit def vsCC[A, L <: HList](implicit g: Generic.Aux[A, L], utcc: UnaryTCConstraint[L, DBAttr], tt: ToTraversable.Aux[L, List, DBAttr[_]]) = new ValidShape[A] {
    def toList(a: A): List[DBAttr[_]] = g.to(a).toList[DBAttr[_]]
  }

  implicit def vsAttr1[A[_] <: DBTableAttribute[_], B]: ValidShape[DBAttr[B]] = new ValidShape[DBAttr[B]] {
    def toList(a: DBAttr[B]): List[DBAttr[_]] = List(a)
  }

  implicit def tupled[A, L <: HList, B, L2 <: HList](implicit vsA: ValidShape[A], vsB: ValidShape[B]): ValidShape[(A, B)] = new ValidShape[(A, B)] {
    def toList(a: (A, B)) = vsA.toList(a._1) ++ vsB.toList(a._2)
  }
}
