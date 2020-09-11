package djala.idless
import minitest._
import Validate._
import cats.data.Ior
import djala.idless.EmptyNestedValidation._
import shapeless._


trait Shared extends SimpleTestSuite {
  case class FPersonalInfo[F[_]](name: F[String], age: F[Int])
  case class FEmployeeInfo[F[_]](job: F[String], personalInfo: F[FPersonalInfo[F]])
}
object ValidateTest extends Shared {
  test("validates nested") {
    val piValidations = FPersonalInfo[Validate](
        NoValidation(),
        Gt(5) and Lt(120)
      )


    val employeeInfoValidations = FEmployeeInfo[Validate](NoValidation(), NestedValidation(piValidations))
    val piO = FPersonalInfo[Option](Some("Daniel"), Some(2))

    val empInfo = FEmployeeInfo[Option](
      job = Some("Contractor"),
      personalInfo = Some(piO)
    )

    val piId = FPersonalInfo[cats.Id]("Daniel", 31)

    println(piValidations.validateF(piId))
    println(piValidations.validateF(piO))

    val employeeInfoNoPersonValidation = employeeInfoValidations.copy[Validate](personalInfo = NoValidation())

    println(employeeInfoNoPersonValidation.validateOpt(empInfo))
    println(employeeInfoNoPersonValidation.validateOpt(empInfo.copy[Option](personalInfo = None)))
  }

  test("validates data instantiated with Id") {

    val piValidations = FPersonalInfo[Validate](
      NoValidation(),
      Gt(5) and Lt(120)
    )

    val piO = FPersonalInfo[cats.Id](
      "Daniel",
      2
    )

    assert(piValidations.validateF(piO).age.isBoth)
  }

  test(name="Validates range") {
    val piValidations = FPersonalInfo[Validate](
      NoValidation(),
      Gt(5) and Lt(120)
    )
    val piO = FPersonalInfo[Option](
      Some("Daniel"),
      Some(2)
    )

    assert(piValidations.validateOpt(piO).age.isBoth)
  }

}

trait EmptyValidator[A] {

}