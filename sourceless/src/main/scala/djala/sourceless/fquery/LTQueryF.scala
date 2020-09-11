package djala.sourceless.fquery

import scalaz._
import Scalaz._

import matryoshka._

trait LTQueryF[A]

final case class DBTableLTF[A](name: String, fields: List[DBAttr[_]]) extends LTQueryF[A]
final case class WrappingQueryLTF[A](l: A, r: A) extends LTQueryF[A]
final case class FilterLTF[A](predicate: A, q: A) extends LTQueryF[A]
final case class ShapeQueryLTF[A](list: List[DBAttr[_]]) extends LTQueryF[A]
final case class JoinQueryLTF[A](l: A, r: A, predicate: A) extends LTQueryF[A]
final case class EqF[A](l: A, r: A) extends LTQueryF[A]
final case class ForeignKeyLTF[A](name: String, table: String, other: A) extends LTQueryF[A]
final case class DBAttrLTF[A](name: String, table: String) extends LTQueryF[A]

object LTQueryF {
  def coalg: Coalgebra[LTQueryF, LTQuery] = {
    case DBTableLT(name, fields) => DBTableLTF(name, fields)
    case WrappingQueryLT(l, r) => WrappingQueryLTF(l, r)
    case FilterLT(p, q) => FilterLTF(p, q)
    case ShapeQueryLT(s) => ShapeQueryLTF(s)
    case JoinQueryLT(l, r, p) => JoinQueryLTF(l, r, p)
    case EqualsLT(l, r) => EqF(l, r)
    case ForeignKeyLT(name, tbl, other, _) => ForeignKeyLTF(name, tbl, other)
    case DBAttrLT(name, tbl, _) => DBAttrLTF(name, tbl)
  }

  def alg: Algebra[LTQueryF, LTQuery] = {
    case DBTableLTF(n, f) => DBTableLT(n, f)
    case WrappingQueryLTF(l, r) => WrappingQueryLT(l, r)
    case FilterLTF(p, q) => FilterLT(p, q)
    case ShapeQueryLTF(s) => ShapeQueryLT(s)
    case JoinQueryLTF(l, r, p) => JoinQueryLT(l, r, p)
    case EqF(l, r) => EqualsLT(l, r)
    case ForeignKeyLTF(name, tbl, other) => ForeignKeyLT(name, tbl, other)
    case DBAttrLTF(name, table) => DBAttrLT(name, table)
  }

  implicit val traverse: scalaz.Traverse[LTQueryF] = new scalaz.Traverse[LTQueryF] {
    override def traverseImpl[G[_], A, B]
    (fa: LTQueryF[A])(f: (A) => G[B])
    (implicit G: scalaz.Applicative[G]): G[LTQueryF[B]] = {
      fa match {
        case DBTableLTF(n, fields) => G.point(DBTableLTF(n, fields))
        case WrappingQueryLTF(l, r) => (f(l) |@| f(r)) (WrappingQueryLTF(_, _))
        case FilterLTF(p, query) => (f(p) |@| f(query)){FilterLTF(_, _)}
        case ShapeQueryLTF(s) => G.point(ShapeQueryLTF(s))
        case JoinQueryLTF(l, r, p) => (f(l) |@| f(r) |@| f(p))(JoinQueryLTF(_, _, _))
        case EqF(l, r) => (f(l) |@| f(r))(EqF(_, _))
        case ForeignKeyLTF(name, tbl, other) => f(other).map{ForeignKeyLTF(name, tbl, _)}
        case DBAttrLTF(name, tbl) => G.point(DBAttrLTF(name, tbl))
      }
    }
  }

  val r = coalg >>> simplify


  implicit val ltqfBiRecursive = Birecursive.algebraIso(alg, coalg)





  def simplify: Algebra[LTQueryF, LTQuery] = {
    case WrappingQueryLTF(s @ ShapeQueryLT(_), WrappingQueryLT(ShapeQueryLT(_), t)) =>
      WrappingQueryLT(s, t)

    case x => alg(x)
  }
}
