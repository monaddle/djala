package djala.sourceless.sources.postgresql

import djala.sourceless.fquery.DBAttr
import matryoshka.{Algebra, Birecursive, Coalgebra}
import shapeless.{::, Generic, HList, HNil}

import scala.annotation.implicitNotFound
import scalaz.{Applicative, Traverse}

@implicitNotFound("Implicit not found: ValidPGShape[A]. This means we couldn't determine the PostgreSQL type. Make sure your table doesn't have a nested data structure.")
trait ValidPGShape[A] {
  def getType(attr: A): List[PGSQL]

  def apply(attr: A) = getType(attr)
}

class LowPriorityValidGPShape {
  implicit def hnil: ValidPGShape[HNil] = new ValidPGShape[HNil] {
    def getType(hnil: HNil): List[PGSQL] = Nil
  }

}

object ValidPGShape extends LowPriorityValidGPShape {
  def apply[A](implicit vpg: ValidPGShape[A]): ValidPGShape[A] = vpg

  implicit def product[A, L <: HList](implicit
                                      g: Generic.Aux[A, L],
                                      vpgT: ValidPGShape[L]): ValidPGShape[A] = new ValidPGShape[A] {

    def getType(attr: A): List[PGSQL] ={
      val hl = g.to(attr)
      println("hlist", hl)
      vpgT.getType(hl)
    }
  }

  def constructor[A](f: DBAttr[A] => List[PGSQL]) = {
    new ValidPGShape[DBAttr[A]] {
      def getType(l: DBAttr[A]) = f(l)
    }
  }

  implicit def int: ValidPGShape[DBAttr[slamdata.Predef.Int]] = constructor{ x=>
    println("in int")
    List(Field(x.name, PGInt()))}

  implicit def str: ValidPGShape[DBAttr[String]] = constructor{ x=> List(Field(x.name, Text()))}




  implicit def hlist[H, T <: HList](implicit vpgh: ValidPGShape[DBAttr[H]], vpgt: ValidPGShape[T]): ValidPGShape[DBAttr[H]:: T] = {
    new ValidPGShape[DBAttr[H] :: T] {

      def getType(attr: DBAttr[H] :: T): List[PGSQL] = {
        println("getting type in hlist", attr)

        vpgh.getType(attr.head) ++ vpgt.getType(attr.tail)
      }
    }
  }

}



