package djala.sourceless.sources.postgresql

import matryoshka._
import matryoshka.implicits._
import PGSQLF._
import djala.sourceless.fquery.InsertQuery
import shapeless._
import shapeless.ops.hlist.ToTraversable

object InsertQuery {
  import PGRenderable._
  def getInsert[A[_[_]], L <: HList](iq: InsertQuery[A])(implicit r: PGRenderable[A[Id]]) = {

    val fields = iq.shapeToList.map{ x => x.name}
    val values = iq.toInsert map {r.render}



    s"""
       |insert into ${iq.table.name} (${fields.mkString(", ")})
       |VALUES (${values.map {row => row.mkString(", ")}.mkString("), \r\n(")});
     """.stripMargin


  }

}

trait PGRenderable[A] {

  def render(a: A): List[String]

}
class LowPriorityPGRenderable {
  implicit def hnil: PGRenderable[HNil] = new PGRenderable[HNil]{def render(a: HNil): List[String] = Nil}

}
object PGRenderable extends LowPriorityPGRenderable {


  def apply[A](implicit pgr: PGRenderable[A]): PGRenderable[A] = pgr


  def idConstructor[A](f: Id[A] => String): PGRenderable[Id[A]] = new PGRenderable[Id[A]] {
    def render(a: Id[A]): List[String] = List(f(a))
  }

  implicit def str: PGRenderable[String] = idConstructor[String](x => s"'$x'")


  implicit def int: PGRenderable[slamdata.Predef.Int] = idConstructor[slamdata.Predef.Int](x => x.toString)

  implicit def product[A, L <: HList](implicit g: Lazy[Generic.Aux[A, L]], pgr: Lazy[PGRenderable[L]]): PGRenderable[A] = new PGRenderable[A] {
    def render(a: A): List[String] = pgr.value.render(g.value.to(a))
  }

  implicit def hlist[H, T <: HList](implicit pgrH: PGRenderable[H], pgrT: PGRenderable[T]): PGRenderable[H::T] = new PGRenderable[H :: T] {
    def render(a: H::T): List[String] = pgrH.render(a.head) ++ pgrT.render(a.tail)
  }

}