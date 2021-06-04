package djala.sourceless.fquery

import minitest._
import matryoshka._
import matryoshka.implicits._
import LTQueryF._
import matryoshka.data._
import matryoshka.patterns.EnvT._
import matryoshka.data.Fix._
import scalaz._
import Scalaz._
import djala.idless.Validate
import Validate._
import djala.sourceless.ValidShape
import djala.sourceless.sources.PostgreSQLDB
import shapeless._

object FQueryTest extends SimpleTestSuite {

  case class FPersonalInfo[F[_]](name: F[String], age: F[Int])
  case class FEmployee[F[_]](name: F[String], job: F[String])

  case class FPersonNested[F[_]](name: F[String], nested: F[FEmployee[F]])

  val piTable = TableQuery("pii_table",
    FPersonalInfo[DBAttr](
      DBTableAttribute("name", "pii_table"),
      DBTableAttribute("age", "pii_table")),
    validations=Some(FPersonalInfo[Validate](NoValidation(), Gt(5) and Lt(120))), database= PostgreSQLDB())


  val employeeTable = TableQuery("employee_table",
    FEmployee[DBAttr](
      ForeignKey("name", "employee_table", piTable.shape.name),
      DBTableAttribute("job", "employee_table")), database=PostgreSQLDB())



  case class TableAsAttr[A[_[_]]](tbl: TableQuery[A]) extends DBAttr[A[DBAttr]] {

    val table = tbl.name

    val name = tbl.name

    val parent = tbl.parent
  }

  implicit def toAttr[A[_[_]]](tbl: TableQuery[A]): TableAsAttr[A] = TableAsAttr(tbl)

  implicit def convert[A[_[_]]](attr: DBAttr[A[DBAttr]])(implicit toTable: DBAttrToTableTrait[A]): A[DBAttr] = {
    toTable.toTable(attr)
  }


  implicit class DBAttrToTable[A[_[_]], L <: HList](attr: DBAttr[A[DBAttr]])(implicit toTable: DBAttrToTableTrait[A]){
    def fields = toTable.toTable(attr)

  }

  val nested = FPersonNested[DBAttr](DBTableAttribute("name", "nested_table"), employeeTable)

  trait DBAttrToTableTrait[A[_[_]]] {
    def toTable(attr: DBAttr[A[DBAttr]]): A[DBAttr] = {
      attr match {
        case TableAsAttr(tbl) => tbl.shape
      }
    }
  }

  object DBAttrToTableTrait {
    def apply[A[_[_]]](attr: DBAttr[A[DBAttr]])(implicit toTbl: DBAttrToTableTrait[A] ): DBAttrToTableTrait[A] = toTbl

    implicit def impl[A[_[_]]]: DBAttrToTableTrait[A] = new DBAttrToTableTrait[A] {}
  }



  val nestedTableAttrs =  FPersonNested[DBAttr](DBTableAttribute("name", "nested_table"), employeeTable)

  val nestedTable = TableQuery("nested_table", FPersonNested[DBAttr](DBTableAttribute("name", "nested_table"), employeeTable), database=PostgreSQLDB())




  import scala.reflect.runtime.universe._








  //  implicit def CCToTableQuery[A[_[_]]](cc: A[DBAttr])(implicit tt: TypeTag[A[DBAttr]], vs: ValidShape[A[DBAttr]]): TableQuery[A] = {
  //    import scala.reflect._
  //
  //    println("type tag", tt.toString.split("\\[").tail.head.split("\\.").last)
  //    TableQuery(tt.toString.split("\\[").tail.head.split("\\.").last, cc)
  //  }








  trait FlatConstraint

  test("create table") {
    println(piTable)
    println("in create table", piTable.toLTQ)
  }

  test("should map") {
    val mapped = piTable.map { x => x.age }

    val deepQuery = mapped.map{x=>x}.map{x=> x}.map{x=>x}.toLTQ

    println("simplified", LTQueryF.ltqfBiRecursive.cata(deepQuery)(simplify))

    println("translated", QueryToSQL.translate(deepQuery))

    println(QueryToSQL.translate(employeeTable.join(piTable)(_.name === _.name).toLTQ))


    val insert = piTable ++= List(FPersonalInfo[cats.Id]("Danil", 33), FPersonalInfo[cats.Id]("Dana", 32))
    println("this is an insert", QueryToSQL.translate(insert.toLTQ))





    import io.circe._
    import io.circe.generic._
    import io.circe.parser._
    import io.circe.syntax._
    import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

    println(FPersonalInfo[List](List("Daniel", "Dana", "Quincy"), List(29, 30, 14)).asJson)



  }

}
