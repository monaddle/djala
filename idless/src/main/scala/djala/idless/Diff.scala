package djala.idless
import shapeless._
import cats.Id

sealed trait Diff[+A] extends Product with Serializable

final case class Nodiff[A](a: A) extends Diff[A]

final case class SomeDiff[A](l: A, r: A) extends Diff[A]

trait DiffHKDT[A[_[_]]] {
  def diff(l: A[Id], r: A[Id]): A[Diff]
}

trait DiffKLists[KL] {
  type B
  def diff(l: KL, r: KL): B
}

object DiffKLists {
  Option
  type Aux[KLIn, KLOut] = DiffKLists[KLIn] { type B = KLOut}

  implicit def apply[KLIn <: HList, KLOut <: HList](implicit dkl: DiffKLists.Aux[KLIn, KLOut]): DiffKLists.Aux[KLIn, KLOut] = dkl

  implicit def diffklist[H, T <: HList, OutTail <: HList](implicit dkl: DiffKLists.Aux[T, OutTail]): DiffKLists.Aux[Id[H] :: T, Diff[Id[H]] :: OutTail] = new DiffKLists[Id[H] :: T] {
    type B = Diff[Id[H]] :: OutTail
    def diff(l: Id[H] :: T, r: Id[H] :: T): Diff[Id[H]] :: OutTail = {
      if(l.head == r.head) {
        Nodiff(l.head) :: dkl.diff(l.tail, r.tail)
      }
      else {
        SomeDiff(l.head, r.head) :: dkl.diff(l.tail, r.tail)
      }
    }
  }

  implicit def diffknil: DiffKLists.Aux[HNil, HNil] = new DiffKLists[HNil] {
    type B = HNil
    def diff(l: HNil, r: HNil): HNil = HNil
  }
}

object DiffHKDT {
  def apply[A[_[_]]](implicit dhk: DiffHKDT[A]): DiffHKDT[A] = dhk

  implicit def hkdtToKlist[HK[_[_]], InKList <: HList, OutKList <: HList](
                                                                           implicit g: Generic.Aux[HK[Id], InKList],
                                                                           gout: Generic.Aux[HK[Diff], OutKList],
                                                                           dkl: DiffKLists.Aux[InKList, OutKList]
                                                                        ): DiffHKDT[HK] = new DiffHKDT[HK] {
    override def diff(l: HK[Id], r: HK[Id]): HK[Diff] = gout.from(dkl.diff(g.to(l), g.to(r)))
  }
}