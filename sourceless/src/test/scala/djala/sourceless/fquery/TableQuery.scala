package djala.sourceless.fquery2
import djala.sourceless.fquery.{DBAttr, DBTableAttribute, TableQuery}
import djala.idless.Validate._
import djala.idless.Validate
import djala.sourceless.sources.PostgreSQLDB
import minitest._
import shapeless._

case class MyFF[F[_]](i: F[Int], o: F[String])
object TableQueryTest extends SimpleTestSuite {



  val tq = TableQuery("mytable",
    MyFF[DBAttr](DBTableAttribute("hello", "mytable"), DBTableAttribute("goodbye", "mytable")),
    database = PostgreSQLDB()
  )

  test("test1") {
    println("table query", tq)
  }


  trait ValidSchema[A] {
  }

  object ValidSchema {
    def apply[A](implicit vs: ValidSchema[A]): ValidSchema[A] = vs

    implicit def toHList[A, L <: HList](implicit g: Generic.Aux[A, L], vs: ValidSchema[L]): ValidSchema[A] = new ValidSchema[A]{}

    implicit def hlist[H[_[_]], L <:HList](implicit vs: ValidSchema[L]): ValidSchema[TableQuery[H] :: L] = new ValidSchema[TableQuery[H] :: L]{}
    implicit def hnil: ValidSchema[HNil] = new ValidSchema[HNil]{}
  }
  case class User[F[_]](id: F[Int], username: F[String], pw: F[String])


  case class ScalpelSchema(users: TableQuery[User])
  case class DB[Schema](a: Schema)(implicit vs: ValidSchema[Schema])


  implicit def dbToSchema[Schema](db: DB[Schema]): Schema = db.a


  val scalpelDB = DB(ScalpelSchema(TableQuery[User]("user", User[DBAttr](DBTableAttribute("id", "name"), DBTableAttribute("name", "user"), DBTableAttribute("pw", "user")), database=PostgreSQLDB())))

  println("scalpeldb", scalpelDB.users)






}
