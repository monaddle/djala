package djala.sourceless
import java.io.PrintWriter

import minitest._
import cats._
import cats.syntax._
import cats.implicits._
import shapeless._
object CSV extends SimpleTestSuite {


  test("Compiles"){





    case class Dollars(a: Double)

    implicit val dollarsReader = CSVField.csvFieldFactory{ s =>
      Dollars(s.replaceAll("[^\\d.]+", "").toDouble)
    }
    case class Payment[F[_]](
                              tpe: F[String],
                              donor: F[String],
                              recipient: F[String],
                              amount: F[Dollars],
                              year: F[String],
                              affiliatedPerson: F[String],
                              source: F[String])


    case class TSV[A[_[_]]](implicit val csvprd: CSVProduct[A[CSVField], A[ErrorOr]]) {
      val toCC = ToCC[A,cats.Id]

      def fromPath[LErr <: HList, LId <: HList]
      (path: String)
      (implicit
       g: shapeless.Generic.Aux[A[djala.sourceless.ErrorOr],LErr],
       g2: shapeless.Generic.Aux[A[cats.Id],LId],
       tt: shapeless.ops.hlist.ToTraversable.Aux[LErr,List,djala.sourceless.ErrorOr[_]],
       ft: shapeless.ops.traversable.FromTraversable[LId]) = {
        import djala.sourceless.Listable
        scala.io.Source
          .fromResource(path)
          .getLines
          .map(_.split("\t")).drop(1)
          .map{_.toList}
          .map{row =>
            val rowRes = Listable(csvprd.read(row)).toList
            val errors = rowRes.collect{ case Left(x) => x }
            errors match {
              case Nil => Right(toCC.listToCC(rowRes.toList.collect { case Right(x) => x }).get)
              case x => Left(errors)
            }
          }
      }

      def fromLines[LErr <: HList, LId <: HList]
      (lines: Seq[String])
      (implicit g: shapeless.Generic.Aux[A[djala.sourceless.ErrorOr],LErr],
      g2: shapeless.Generic.Aux[A[cats.Id],LId],
      tt: shapeless.ops.hlist.ToTraversable.Aux[LErr,List,djala.sourceless.ErrorOr[_]],
      ft: shapeless.ops.traversable.FromTraversable[LId]) = {

        lines.map(_.split("\t")).drop(1)
          .map{_.toList}
          .map{row =>
            val rowRes = Listable(csvprd.read(row)).toList
            val errors = rowRes.collect{ case Left(x) => x }
            errors match {
              case Nil => Right(toCC.listToCC(rowRes.toList.collect { case Right(x) => x }).get)
              case x => Left(errors)
            }
          }

      }
    }

    val payments = TSV[Payment].fromPath("payments.tsv").toList
    println(TSV[Payment].fromPath("payments.tsv").toList.take(10))

    val validpayments = payments.collect{ case Right(x) => x}

    val invalidEntries = payments.zipWithIndex.collect{ case (Left(x), i) => (x, i)}

    validpayments.groupBy{payment => s"${payment.recipient},${payment.year},${payment.donor}"}

    val groupedPayments = validpayments.groupBy({payment => payment.recipient})
      .map{case (recipient, payments) =>
        (recipient, payments
          .groupBy {payment => payment.year}
          .map{case (year, payments) => (year, payments.groupBy(p => p.donor).map{ case (donor, payments) => (donor, payments.foldRight(0.0){(p, i) => i + p.amount.a})})})}


    import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

    sealed trait Foo
    case class Bar(xs: Vector[String]) extends Foo
    case class Qux(i: Int, d: Option[Double]) extends Foo

    val foo: Foo = Qux(13, Some(14.0))

    val json = foo.asJson.noSpaces
    println(json)

    val paymentjson = groupedPayments.asJson.noSpaces

    new PrintWriter("/Users/danielporter/grouped_payments.json"){write(paymentjson); close();}


    case class InstitutionAffiliation[F[_]](
                                             poi: F[String],
                                             institution: F[String],
                                             institutionType: F[String],
                                             affiliationType: F[String],
                                             title: F[String],
                                             startDate: F[String],
                                             endDate: F[String],
                                             primaryAffiliation: F[String],
                                             notes: F[String],
                                             source: F[String])
    println(TSV[InstitutionAffiliation].fromPath("institution_affiliations.tsv").toList.take(10))

  }
}
