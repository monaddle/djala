package djala.sourceless.fquery

import LTQueryF._
import matryoshka._
import matryoshka.implicits._
import scalaz._
import Scalaz._
object QueryToSQL {
  def translate(q: LTQuery): String = {
    val simplified = LTQueryF.ltqfBiRecursive.cata(q)(simplify)
    LTQueryF.ltqfBiRecursive.cata(simplified)(toSQL).asString
  }


  def toSQL: Algebra[LTQueryF, SQL] = {
    case ShapeQueryLTF(fields) => Select(fields.map{x=> s"${x.table}.${x.name}"})

    case WrappingQueryLTF(s @ Select(fields, _, _), from @Select(_, _, _)) => from.copy(fields=fields)

    case WrappingQueryLTF(w @ Where(_), s @ Select(_, _, _)) => s.copy(where=Some(w))

    case DBTableLTF(name, fields) => Select(fields.map{x=>s"${x.table}.${x.name}"}, Some(Table(name)))

    case FilterLTF(p, s: Select) =>
      val where = s.where.map{f => And(f, p)} getOrElse p
      s.copy(where=Some(where))

    case EqF(l, r) => Equals(l, r)

    case DBAttrLTF(name, tbl) => Column(name: String, Table(tbl))

    case ForeignKeyLTF(name, tbl, other) => ColumnFK(name, Table(tbl), other)

    case JoinQueryLTF(l: Select, r: Select, p) => Select(l.fields ++ r.fields, Some(Join(l.from.get, r.from.get, p)), None)
  }

  def dataDefinition: Algebra[LTQueryF, String] = {
      case DBAttrLTF(name, _) => name
  }
}


trait SQL {
  import SQLF._
  def asString = {
    println("converting to string", this)
    SQLF.algebraIso.cata(this)(SQLF.mkString)
  }
}

case class Where(predicates: List[SQL]) extends SQL
case class From(str: SQL) extends SQL
case class Table(name: String) extends SQL
case class Select(fields: List[String], from: Option[SQL]=None, where: Option[SQL]=None) extends SQL
case class Equals(l: SQL, r: SQL) extends SQL
case class And(l: SQL, r: SQL) extends SQL
case class Column(name: String, table: Table) extends SQL
case class ColumnFK(name: String, tbl: Table, target: SQL) extends SQL
case class Join(l: SQL, r: SQL, on: SQL) extends SQL

trait SQLF[A]
case class WhereF[A](predicates: List[A]) extends SQLF[A]
case class FromF[A](f: A) extends SQLF[A]
case class TableF[A](name: String) extends SQLF[A]
case class SelectF[A](fields: List[String], from: Option[A], where: Option[A]) extends SQLF[A]
case class EqualsF[A](l: A, r: A) extends SQLF[A]
case class AndF[A](l: A, r: A) extends SQLF[A]
case class ColumnF[A](name: String, table: Table) extends SQLF[A]
case class ColumnFKF[A](name: String, tbl: Table, target: A) extends SQLF[A]
case class JoinF[A](l: A, r: A, on: A) extends SQLF[A]

object SQLF {
  def alg: Algebra[SQLF, SQL] = {
    case WhereF(p) => Where(p)
    case FromF(f) => From(f)
    case TableF(name) => Table(name)
    case SelectF(fields, from, where) => Select(fields, from, where)
    case JoinF(l, r, on) => Join(l, r, on)
    case EqualsF(l, r) => Equals(l, r)
    case ColumnFKF(name, tbl, other) => ColumnFK(name, tbl, other)
    case ColumnF(name, tbl) => Column(name, tbl)
  }

  def coalg: Coalgebra[SQLF, SQL] = {
    case Where(p) => WhereF(p)
    case From(f) => FromF(f)
    case Table(name) => TableF(name)
    case Select(fields, from, where) => SelectF(fields, from, where)
    case Join(l, r, on) => JoinF(l, r, on)
    case Equals(l, r) => EqualsF(l, r)
    case ColumnFK(name, tbl, other) => ColumnFKF(name, tbl, other)
    case Column(name, tbl) => ColumnF(name, tbl)
  }

  implicit val traverse: Traverse[SQLF] = new Traverse[SQLF] {
    override def traverseImpl[F[_], A, B]
      (fa: SQLF[A])
      (f: (A) => F[B])
      (implicit F: Applicative[F]): F[SQLF[B]] = {
      fa match {
        case WhereF(p) => F.sequence(p.map(f)).map{x=> WhereF(x)}
        case FromF(from) => f(from).map{FromF(_)}
        case TableF(name) => F.point(TableF(name))
        case SelectF(fields, from, where) => (from.map{f}.sequence |@| where.map{f}.sequence)(SelectF(fields, _, _))
        case JoinF(l, r, on) => (f(l) |@| f(r) |@| f(on))(JoinF(_, _, _))
        case EqualsF(l, r) => (f(l) |@| f(r))(EqualsF(_, _))
        case ColumnFKF(name, tbl, other) => f(other).map(ColumnFKF(name, tbl, _))
        case ColumnF(name, tbl) => F.point(ColumnF(name, tbl))
      }
    }
  }


  implicit val algebraIso = Birecursive.algebraIso(alg, coalg)

  def mkString: Algebra[SQLF, String] = {
    case SelectF(fields, fromLoc, whereStr) =>
      val from = fromLoc getOrElse ""
      val where = whereStr getOrElse ""
      val selection = fields.mkString(", ")
      s"select $selection from $from $where"
    case TableF(name) => println("table name", name)
      name
    case WhereF(p) => s"where ${p.mkString(" and ")}"
    case ColumnF(name, Table(tbl)) => s"$tbl.$name"
    case ColumnFKF(name, Table(tbl), _) => s"$tbl.$name"
    case EqualsF(l, r) => s"$l = $r"
    case JoinF(l, r, p) => s"$l inner join $r on $p"
  }
}

trait ScalplError
case class InvalidQueryFormError(msg: String)