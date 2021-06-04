package djala.idless
import minitest._

object DiffTest extends SimpleTestSuite {
  case class FPersonalInfo[F[_]](name: F[String], age: F[Int])
  test("diffs effectively") {
    val person = FPersonalInfo[cats.Id]("Danil", 23)
    val person2 = FPersonalInfo[cats.Id]("Danil", 24)
    val person3 = FPersonalInfo[cats.Id]("Daniel", 24)

    val differ = implicitly[DiffHKDT[FPersonalInfo]]
    println(differ.diff(person, person2))
  }
}