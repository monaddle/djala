package djala.sourceless.fquery

import FQuery._

trait LTQuery
object LTQuery {
  def fromFQuery[A](fquery: FQuery[A]): LTQuery = {
    fquery match {
      case tbl @ TableQuery(name, _, _, _, _) => DBTableLT(name, tbl.shapeToList)
      case WrappingQuery(l, r) => WrappingQueryLT(fromFQuery(l), fromFQuery(r))
      case Filter(Eq(l, r), f) => FilterLT(EqualsLT(fromFQuery(l), fromFQuery(r)), fromFQuery(f))
      case x: ShapeQuery[A] => ShapeQueryLT(x.shapeToList)
      case JoinQuery(l, r, on) => JoinQueryLT(fromFQuery(l), fromFQuery(r), fromFQuery(on))
      case Eq(l, r) => EqualsLT(fromFQuery(l), fromFQuery(r))
      case DBTableAttribute(name, tbl, _) => DBAttrLT(name, tbl)
      case ForeignKey(name, table, other, _) => ForeignKeyLT(name, table, fromFQuery(other))
    }
  }
}


final case class DBTableLT(name: String, fields: List[DBAttr[_]]) extends LTQuery
final case class WrappingQueryLT(l: LTQuery, r: LTQuery) extends LTQuery

final case class FilterLT(predicate: LTQuery, q: LTQuery) extends LTQuery
final case class ShapeQueryLT(fields: List[DBAttr[_]]) extends LTQuery

final case class JoinQueryLT(l: LTQuery, r: LTQuery, on: LTQuery) extends LTQuery

final case class EqualsLT(l: LTQuery, r: LTQuery) extends LTQuery

final case class DBAttrLT(name: String, table: String, `type`: String= "") extends LTQuery
final case class ForeignKeyLT(name: String, table: String, target: LTQuery, `type`: String = "") extends LTQuery