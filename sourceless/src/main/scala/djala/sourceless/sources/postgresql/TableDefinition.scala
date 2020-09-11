package djala.sourceless.sources.postgresql

import matryoshka._
import matryoshka.implicits._
import shapeless._
import shapeless.ops.hlist.ToTraversable
import scalaz._
import Scalaz._
import djala.idless.Validate
import djala.sourceless.fquery.{DBAttr, TableQuery}
import Validate._
object TableDefinition {


  def getDefinition[A[_[_]], L <: HList](tbl: TableQuery[A])(implicit
                                                             vpg: ValidPGShape[A[DBAttr]],
                                                             g: Generic.Aux[A[Validate], L],
                                                             tt: ToTraversable.Aux[L, List, Validate[_]]

  ): String = {


    PGSQLF.algebraIso.cata(Table(tbl.name, vpg(tbl.shape), tbl.validationsToList[L].map { x => validationToPGValidation(x)}))(renderTableDefinition)
  }

  def renderTableDefinition: Algebra[PGSQLF, String] = {
    case TableF(name, fields, validations) =>
      val fieldsString =
        if(validations.isEmpty) fields
      else {
        fields.zip(validations).map {case (f, v) =>
          println("print F and V", f, v)
          if(v.isEmpty) f
          else s"$f CHECK ($v)".replace("PLACEHOLDERFIELDNAME", f)
        }
      }

      s"""
        |create table $name (
        |  ${fieldsString mkString ",\r\n  "}
        |)
      """.stripMargin

    case FieldF(name, t) =>
      s"$name $t"

    case TextF() => "text"
    case IntF() => "integer"
    case PGGtF(x) => s"PLACEHOLDERFIELDNAME > $x"
    case PGLtF(x) => s"PLACEHOLDERFIELDNAME < $x"
    case PGAndF(l, r) => s"$l AND $r"
    case NoConstraintF() => ""
  }

  def validationToPGValidation[A](v: Validate[A]): PGSQL = {
    v match {
      case NoValidation() => NoConstraint()
      case Gt(a) => PGGt(a)
      case Lt(a) => PGLt(a)
      case And(l, r) => PGAnd(l, r)
    }
  }
}

import scala.annotation.implicitNotFound

