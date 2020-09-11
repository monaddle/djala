package djala.sourceless.sources

import djala.idless.Validate
import djala.sourceless.ValidShape
import djala.sourceless.fquery.{DBAttr, DBTableAttribute, ForeignKey, TableQuery}
import shapeless.{Generic, HList, Nat}
import shapeless.ops.hlist.At

trait DataSource {
}

trait Database extends DataSource {
  val hostname: String
  val port: String
  val username: String
  val password: String
  val databaseName: String

  def table[A[_[_]], PK <: Nat](name: String,
                     shape: A[DBAttr],
                     parent: Option[DBAttr[_]]=None,
                     validations: Option[A[Validate]] = None)
                    (implicit vs: ValidShape[A[DBAttr]])  = TableQuery[A](name, shape, parent, validations, this)

}


case class PostgreSQLDB(hostname: String = "localhost",
                        port: String = "5432",
                        username: String = "postgres",
                        password: String = "postgres",
                        databaseName: String="postgres") extends Database {
}


case class Edge[A[_[_]], B[_[_]], C[_[_]]](from: TableQuery[A], edge: TableQuery[B], to: TableQuery[C]) {

}

case class Neo4jDB(hostname: String, port: String, username: String, password: String, databaseName: String="") extends Database {
  def edge[From[_[_]], E[_[_]], To[_[_]]](from: TableQuery[From], edge: TableQuery[E], to: TableQuery[To]): Edge[From, E, To] = Edge(from, edge, to)
}

object test {
  case class FF[F[_]](fst: F[Int], scd: F[String])
  case class FEmployee[F[_]](name: F[Option[String]], job: F[String])

  def tbl[A[_[_]], PK <: Nat, L <: HList, O](a: A[DBAttr], pkLoc: PK)(implicit vs: ValidShape[A[DBAttr]], g: Generic.Aux[A[DBAttr], L], at: At.Aux[L, PK, O]): O = at(g.to(a))

  def tbl[A[_[_]], I, O[_] <: Option[_], V[_] <: DBAttr[_], L <: HList](a: A[DBAttr])(implicit  g: Generic.Aux[A[DBAttr], L], at: At.Aux[L, Nat._0, V[O[I]]]):  V[O[I]] = at(g.to(a))
  private val res = tbl(FEmployee[DBAttr](
    DBTableAttribute[Option[String]]("name", "employee_table"),
    DBTableAttribute("job", "employee_table")), Nat._1)

  val res2: DBAttr[Option[String]] = tbl(FEmployee[DBAttr](
    DBTableAttribute("name", "employee_table"),
    DBTableAttribute("job", "employee_table")), Nat._0)
}
