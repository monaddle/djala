package djala.idless
import minitest._
import Rand._
import Validate._

object RandSpec extends SimpleTestSuite {

  case class FPersonalInfo[F[_]](
    name: F[String],
    age: F[Int])
  test("generates random number within both") {


    val piValidations =
      FPersonalInfo[Validate](
        NoValidation[String](),
        Gt(5) and Lt(120))

    val res = (0 until 1000).map(_ => Rand[FPersonalInfo]().withConstraints(piValidations).age)
    println(res)
    println("res max and min", res.max, res.min)
    assert(res.max < 120 && res.min > 5)

  }

  test("generates random number greater than constraint") {
    val piValidations =
      FPersonalInfo[Validate](
        NoValidation[String](),
        Gt(5))
    val res = (0 until 1000).map(_ => Rand[FPersonalInfo]().withConstraints(piValidations).age)
    assert(res.min > 5)
  }

  test("generates random number less than constraint") {
    val piValidations =
      FPersonalInfo[Validate](
        NoValidation[String](),
        Lt(5))
    val res = (0 until 10000).map(_ => Rand[FPersonalInfo]().withConstraints(piValidations).age)
    assert(res.max < 5)
  }
}
