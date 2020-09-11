package djala.idless

import shapeless._
import cats._
import cats.data._
import ThreeKLists.IorStringOr
import djala.idless.Validate._
import shapeless.Generic.Aux

trait ThreeKLists[A[_[_]]] {
  type OKL
  type VKL
  type IOKL

  type F[A]
  val genericFunctorKList: Generic.Aux[A[F], OKL]
  val genericValidateKList: Generic.Aux[A[Validate], VKL]
  val genericResultKList: Generic.Aux[A[IorStringOr], IOKL]


}

object ThreeKLists  {
  type Aux[A[_[_]], OKL0, VKL0, F0[_], IOKL0] = ThreeKLists[A] {
    type OKL = OKL0
    type VKL = VKL0
    type IOKL = IOKL0
    type F[A] = F0[A]
  }
  type IorStringOr[A] = Ior[String, A]
  implicit def apply[A[_[_]], HL0 <: HList, VKL0 <: HList, F0[_], IORKL0 <: HList](implicit
                                              gFKList: Generic.Aux[A[F0], HL0],
                                               gVKList: Generic.Aux[A[Validate], VKL0],
                                               gIorKList: Generic.Aux[A[IorStringOr], IORKL0]):
  ThreeKLists.Aux[A, HL0, VKL0, F0, IORKL0] = new ThreeKLists[A] {
    override type OKL = HL0
    override type VKL = VKL0
    override type IOKL = IORKL0
    override type F[A] = F0[A]
    val genericFunctorKList = gFKList
    override val genericValidateKList: Generic.Aux[A[Validate], VKL0] = gVKList
    override val genericResultKList: Generic.Aux[A[IorStringOr], IORKL0] = gIorKList
  }
}

object DiveringImplicitMinimalReproduction {
  case class PersonK[F[_]](a: F[Int], b: F[String])

  implicit class Tester[A[_[_]], F[_]](a: A[F]) {
    def summontkl[OKL <: HList, VKL <: HList, IORKL <: HList](implicit tkl: ThreeKLists.Aux[A, OKL, VKL, F, IORKL]) = tkl
  }

  PersonK[Option](Some(1), None).summontkl.genericFunctorKList.to(PersonK[Option](Some(1), Some("None")))

}

import shapeless.{Generic, HList}


trait KListValidationRunner[LV <: HList, LO <: HList, LIOS <: HList] {
  def validate(l: LV, l1: LO): LIOS
}

object KListValidationRunner {

  implicit def apply[LV <: HList, LO <: HList, LIOS <: HList]: KListValidationRunner[LV, LO, LIOS] = new KListValidationRunner[LV, LO, LIOS] {
    override def validate(l: LV, l1: LO): LIOS = ???
  }
}

class ValidateTest[A]
trait IorString[A]

object Test {
  def validateOptWorks[A[_[_]], LV <: HList, LO <: HList, LIOS <: HList](av: A[Validate], ao: A[Option])(
    implicit
    gv: Generic.Aux[A[Validate], LV],
    go: Generic.Aux[A[Option], LO],
    gio: Generic.Aux[A[IorStringOr], LIOS],
    v: KListValidationRunner[LV, LO, LIOS]
  ): A[IorStringOr] = {
    ???
  }

  // where A[_[_]] is something like:
  case class PersonK[F[_]](id: F[Int], name: F[String])

  trait ThreeKListGenericWrangler[A[_[_]], LV <: HList, LO <: HList, LIOS <: HList] {
    def gv: Generic.Aux[A[Validate], LV]
    def go: Generic.Aux[A[Option], LO]
    def gio: Generic.Aux[A[IorStringOr], LIOS]
  }

  object ThreeKListGenericWrangler {
    implicit def apply[A[_[_]], LV <: HList, LO <: HList, LIOS <: HList](implicit gv0: Generic.Aux[A[Validate], LV],
                                                                         go0: Generic.Aux[A[Option], LO],
                                                                         gio0: Generic.Aux[A[IorStringOr], LIOS]): ThreeKListGenericWrangler[A, LV, LO, LIOS] = new ThreeKListGenericWrangler[A, LV, LO, LIOS] {
      override def gio: Aux[A[IorStringOr], LIOS] = gio0

      override def go: Aux[A[Option], LO] = go0

      override def gv: Aux[A[Validate], LV] = gv0
    }

  }


  def validateOptMaybe[A[_[_]], LV <: HList, LO <: HList, LIOS <: HList](av: A[Validate], a: A[Option])(
    implicit
    threeKListGenerics: ThreeKListGenericWrangler[A, LV, LO, LIOS],
    v: KListValidationRunner[LV, LO, LIOS]
  ): A[IorStringOr] = {
    threeKListGenerics.gio.from(v.validate(threeKListGenerics.gv.to(av), threeKListGenerics.go.to(a)))
  }
  val personKValidate = PersonK[Validate]( NoValidation(),  NoValidation())
  val personKOpt = PersonK[Option](Some(1), Some("Daniel"))

  validateOptMaybe(personKValidate, personKOpt)


  val seq = Seq("asdf")
  seq
}


object MinimalRepro {
  trait GenericWithAux[A[_[_]]] {
    type B <: HList
    type C <: HList
  }

  case class Validator[A](a: A)

  trait ValidationRunner[A[_[_]]] {

  }
  trait GenericWithAuxLowPriority {
    implicit def genericWithAuxProduct[A[_[_]], HL <: HList, HLV <: HList](
        implicit generic: Generic.Aux[A[Option], HL],
        genericValidator: Generic.Aux[A[Validator], HLV]): GenericWithAux.Aux[A, HL, HLV] = new GenericWithAux[A] {
      override type B = HL
      override type C = HLV
    }

  }

  object GenericWithAux extends GenericWithAuxLowPriority {
    type Aux[A0[_[_]], B0, C0] = GenericWithAux[A0] {
      type B = B0
      type C = C0
    }
    implicit def apply[A[_[_]], HLO <: HList, HLV <: HList](implicit genericWithAux: GenericWithAux.Aux[A, HLO, HLV]): GenericWithAux.Aux[A, HLO, HLV] = genericWithAux
  }

  import GenericWithAux._

  case class ToTest[F[_]](a: F[Int], b: F[String])
  implicitly[GenericWithAux[ToTest]]

  def instantiator[A[_[_]], HLO <: HList, HLV <: HList](a: A[Option]) (
    implicit genericWithAux: GenericWithAux.Aux[A, HLO, HLV]
  ) = {

  }

  instantiator(ToTest[Option](None, None))
}