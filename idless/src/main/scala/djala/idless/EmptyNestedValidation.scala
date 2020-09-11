package djala.idless

import djala.idless.Validate.NoValidation
import shapeless._

trait EmptyNestedValidation[A] {
  val empty: A
}


object EmptyNestedValidation {
  implicit def apply[A[_[_]]](implicit t: EmptyNestedValidation[A[Validate]]): EmptyNestedValidation[A[Validate]] = t
  implicit def a[A[_[_]], HL <: HList](implicit g: Generic.Aux[A[Validate], HL], t: EmptyNestedValidation[HL]): EmptyNestedValidation[A[Validate]] = new EmptyNestedValidation[A[Validate]] {
    override val empty = g.from(t.empty)
  }

  implicit def hlistTest[H, T <: HList](implicit t: EmptyNestedValidation[T]): EmptyNestedValidation[Validate[H] :: T] = new EmptyNestedValidation[Validate[H] :: T] {
    val empty = NoValidation[H] :: t.empty
  }

  implicit def hnilTest[Nil <: HNil]: EmptyNestedValidation[HNil] = new EmptyNestedValidation[HNil] { val empty = HNil}


  case class KList[F[_]](a: F[Int], b: F[String])

}