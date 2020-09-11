package djala.sourceless.sources.postgresql

import matryoshka._
import scalaz._
import Scalaz._


trait PGSQL

trait FieldType extends PGSQL

case class Text() extends PGSQL
case class PGInt() extends PGSQL

case class Field(name: String, ft: PGSQL) extends PGSQL
case class Table(name: String, fields: List[PGSQL], constraints: List[PGSQL]) extends PGSQL
case class PGInsertQuery[A[_[_]]](toInsert: List[A[Id]], tbl: PGSQL) extends PGSQL

// constraints
case class PGGt[A](i: A) extends PGSQL
case class PGLt[A](i: A) extends PGSQL
case class NoConstraint() extends PGSQL
case class PGAnd[A](l: A, r: A) extends PGSQL




// pattern functor
trait PGSQLF[A]
case class FieldF[A](name: String, ft: A) extends PGSQLF[A]
case class TableF[A](name: String, fields: List[A], validations: List[A]) extends PGSQLF[A]
case class TextF[A]() extends PGSQLF[A]
case class IntF[A]() extends PGSQLF[A]
case class PGInsertQueryF[A[_[_]], B](toInsert: List[A[Id]], tbl: B) extends PGSQLF[B]

// constraints
case class PGGtF[A, B](i: A) extends PGSQLF[B]
case class PGLtF[A, B](i: A) extends PGSQLF[B]
case class NoConstraintF[B]() extends PGSQLF[B]
case class PGAndF[A, B](l: A, r: A) extends PGSQLF[B]

object PGSQLF {
  def alg: Algebra[PGSQLF, PGSQL] = {
    case FieldF(name, ft) => Field(name, ft)
    case TableF(name, fields, validations) => Table(name, fields, validations)
    case TextF() => Text()
    case IntF() => PGInt()
    case PGInsertQueryF(ins, tbl) => PGInsertQuery(ins, tbl)
    case PGGtF(v) => PGGt(v)
    case PGLtF(v) => PGLt(v)
    case PGAndF(l, r) => PGAnd(l, r)
    case NoConstraintF() => NoConstraint()
  }

  def coalg: Coalgebra[PGSQLF, PGSQL] = {
    case Field(name, ft) => FieldF(name, ft)
    case Table(name, fields, v) => TableF(name, fields, v)
    case Text() => TextF()
    case PGInt() => IntF()
    case PGInsertQuery(ins, tbl) => PGInsertQueryF(ins, tbl)
    case PGGt(v) => PGGtF(v)
    case PGLt(v) => PGLtF(v)
    case PGAnd(l, r) => PGAndF(l, r)
    case NoConstraint() => NoConstraintF()
  }

  implicit val traverse: Traverse[PGSQLF] = new Traverse[PGSQLF] {
    override def traverseImpl[G[_], A, B]
    (fa: PGSQLF[A])
    (f: (A) => G[B])
    (implicit G: Applicative[G]): G[PGSQLF[B]] = {
      fa match {
        case FieldF(name, ft) => f(ft).map(FieldF(name, _))
        case TableF(name, fields, v) => (G.sequence(fields.map(f)) |@| G.sequence(v.map(f)))(TableF(name, _, _))
        case TextF() => G.point(TextF())
        case IntF() => G.point(IntF())
        case PGGtF(v) => G.point(PGGtF(v))
        case PGLtF(v) => G.point(PGLtF(v))
        case PGAndF(l, r) => G.point(PGAndF(l, r))
        case NoConstraintF() => G.point(NoConstraintF())
      }
    }
  }
  val algebraIso = Birecursive.algebraIso(alg, coalg)
}