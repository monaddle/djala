package djala.idless

import cats.data._
import djala.idless.Validate.{IorStringOr, NestedValidation, NoValidation}
import shapeless._

trait ValidateF[A[_[_]]] {
}
object ValidateF {
  def apply[A[_[_]]](implicit v: ValidateF[A]): ValidateF[A] = v

}


trait Validate[A] {
  import Validate._
  def and(that: Validate[A]): Validate[A] = And(this, that)

  def validate(a: A): List[String]

  def getPoint: Option[A] = None
}

trait HListValidator[LV] {
  type LO
  type LIOS

  def validate(lv: LV, lo: LO): LIOS

}

object HListValidator {
  type Aux[LV, LO0, LIOS0] = HListValidator[LV] { type LO = LO0; type LIOS = LIOS0; }

  def apply[LV, LO, LIOS](implicit hlv: HListValidator.Aux[LV, LO, LIOS]): HListValidator.Aux[LV, LO, LIOS] = hlv

  implicit def nested[
  M[_],
  H[_[_]],
  VT <: HList,
  OT <: HList,
  IOT <: HList](
                  implicit
                  thlv: HListValidator.Aux[VT, OT, IOT],
                  pfto: NestedFunctorToIor[H, M] ):
  HListValidator.Aux[Validate[H[Validate]]::VT, M[H[M]]::OT, IorStringOr[H[IorStringOr]]::IOT] = new HListValidator[Validate[H[Validate]] :: VT] {

    override type LO = M[H[M]] :: OT
    override type LIOS = IorStringOr[H[IorStringOr]] :: IOT

    def validate(lv: Validate[H[Validate]] :: VT, lo: M[H[M]] :: OT): IorStringOr[H[IorStringOr]] :: IOT = {
      pfto.fToIor(lv.head, lo.head) :: thlv.validate(lv.tail, lo.tail)
    }

  }


  trait NestedFunctorToIor[T[_[_]], M[_]] {
    def fToIor(v: Validate[T[Validate]], m: M[T[M]]): IorStringOr[T[IorStringOr]]
  }
  object NestedFunctorToIor {
    implicit def optionToIor[T[_[_]], TVL <: HList, TOL <: HList, TIOL <: HList](implicit
                                                                         hvg: Generic.Aux[T[Validate], TVL],
                                                                         hog: Generic.Aux[T[Option], TOL],
                                                                         hiog: Generic.Aux[T[IorStringOr], TIOL],
                                                                         hhlv: HListValidator.Aux[TVL, TOL, TIOL],
                                                                         ev: EmptyNestedValidation[T[Validate]]): NestedFunctorToIor[T, Option] = new NestedFunctorToIor[T, Option] {
      override def fToIor(v: Validate[T[Validate]], m: Option[T[Option]]): IorStringOr[T[IorStringOr]] = {
        (v match {
          case x @NestedValidation(_) => m.map { t =>  Ior.Right(hiog.from(hhlv.validate(hvg.to(x.a), hog.to(t)))) }
          case _ @ NoValidation() => m.map { t => Ior.Right(hiog.from(hhlv.validate(hvg.to(ev.empty), hog.to(t)))) }
          case _ => ??? // This should never happen
        }) getOrElse Ior.Left("No values present for nested thing")
      }
    }
  }

  trait ValidateMonadToIor[M[_]] {
    def mToIor[T](v: Validate[T], m: M[T]): IorStringOr[T]
  }

  object ValidateMonadToIor {
    def apply[M[_]](implicit validateMonadToIor: ValidateMonadToIor[M]): ValidateMonadToIor[M] = validateMonadToIor

    implicit def opt: ValidateMonadToIor[Option] = new ValidateMonadToIor[Option] {
      def mToIor[T](v: Validate[T], m: Option[T]): IorStringOr[T] = {
        m match {
          case Some(x) => val validations: List[String] = v.validate(x)
            if(validations.isEmpty) Ior.Right(x)
            else Ior.both(validations.mkString(","), x)
          case None => Ior.Left("No value")
        }
      }
    }

    implicit def id: ValidateMonadToIor[Id] = new ValidateMonadToIor[Id] {
      override def mToIor[T](v: Validate[T], m: Id[T]): IorStringOr[T] = {
        val validations = v.validate(m)
        if(validations.isEmpty) Ior.Right(m)
        else Ior.both(validations.mkString, m)
      }
    }
  }

  implicit def hlv[
  F[_],
  H,
  VT <: HList,
  OT <: HList,
  IOT <: HList]
  (implicit hlv: HListValidator.Aux[VT, OT, IOT], opt: ValidateMonadToIor[F]):
  HListValidator.Aux[
    Validate[H] :: VT,
    F[H]:: OT,
    IorStringOr[H]:: IOT] = new HListValidator[Validate[H] :: VT] {
    override type LO = F[H] :: OT
    override type LIOS = IorStringOr[H] :: IOT
    def validate(vl: Validate[H]::VT, ol: F[H]::OT): IorStringOr[H] ::IOT = {
      val res = opt.mToIor(vl.head, ol.head) :: hlv.validate(vl.tail, ol.tail)
      res
    }
  }

  implicit def hnil[V <: HNil, O <: HNil, IO <: HNil]: HListValidator.Aux[HNil, HNil, HNil] = new HListValidator[HNil] {
    type LO = HNil
    type LIOS = HNil
    def validate(v: HNil, o: HNil) = HNil
  }
}


object Validate {

  type IorStringOr[A] = cats.data.Ior[String, A]



  implicit class Validatable[A[_[_]]](av: A[Validate]) {
    def validateOpt[LV <: HList, LO <: HList, LIOS <: HList](ao: A[Option])(
      implicit gv: Generic.Aux[A[Validate], LV],
      go: Generic.Aux[A[Option], LO],
      gio: Generic.Aux[A[IorStringOr], LIOS],
      v: HListValidator.Aux[LV, LO, LIOS]
    ): A[IorStringOr] = {
      gio.from(v.validate(gv.to(av), go.to(ao)))
    }



    def validateF[F[_], LV <: HList, LO <: HList, LIOS <: HList](ao: A[F])(
      implicit gv: Generic.Aux[A[Validate], LV],
      go: Generic.Aux[A[F], LO],
      gio: Generic.Aux[A[IorStringOr], LIOS],
      v: HListValidator.Aux[LV, LO, LIOS],
      tkl: ThreeKLists.Aux[A, LO, LV, F, LIOS]): A[IorStringOr] = {
      gio.from(v.validate(gv.to(av), go.to(ao)))
    }

    def apply[LV <: HList, LO <: HList, LIOS <: HList](ao: A[Option])(
      implicit gv: Generic.Aux[A[Validate], LV],
      go: Generic.Aux[A[Option], LO],
      gio: Generic.Aux[A[IorStringOr], LIOS],

      v: HListValidator.Aux[LV, LO, LIOS]): A[IorStringOr] = {
      gio.from(v.validate(gv.to(av), go.to(ao)))
    }
  }
  def apply[A](implicit v: Validate[A]): Validate[A] = v

  implicit def constructValidate[A](f: A => List[String]): Validate[A] = new Validate[A]{ def validate(a: A) = f(a)}


  final case class NoValidation[A]() extends Validate[A] {
    def validate(a: A) = Nil


  }

  final case class NestedValidation[A[_[_]]](a: A[Validate]) extends Validate[A[Validate]] {
    def validate(a: A[Validate]) = Nil

    override def getPoint = Some(a)
  }


  final case class Gt[A](a: A)(implicit n: Numeric[A]) extends Validate[A] {
    val numericInst = n
    def validate(b: A) = if(n.gt(b, a)) Nil else List(s"$b must be greater than $a")

  }


  final case class Lt[A](a: A)(implicit n: Numeric[A]) extends Validate[A] {
    def validate(b: A) = if(n.lt(b, a)) Nil else List(s"$b must be less than $a.")
  }


  final case class NonEmpty[A]()(implicit t: Traversable[A]) extends Validate[A] {
    def validate(a: A) = if(t.count {_ => true} > 0) Nil else List("Cannot be empty.")
  }


  final case class MatchesRegex(re: String) extends Validate[String] {
    val compiled = re.r
    def validate(s: String) = if(compiled.findFirstMatchIn(s).nonEmpty) Nil else List(s"$s must be matched by $re")
  }


  final case class ContainedIn[A](s: Set[A]) extends Validate[A] {
    def validate(a: A) = if(s.contains(a)) Nil else List(s"$a must be in $s")
  }


  final case class NotIn[A](s: Set[A]) extends Validate[A] {
    def validate(a: A) = if(s.contains(a)) List(s"$a cannot be in $s") else Nil
  }


  final case class And[A](l: Validate[A], r: Validate[A]) extends Validate[A] {
    def validate(a: A) = l.validate(a) ++ r.validate(a)
  }
}

