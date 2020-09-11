package djala.sourceless
import cats.data.Ior
import cats.syntax._
import cats._
import cats.implicits._
import shapeless._

trait CSV[A[_[_]]] {

  def toCC(path: String): List[Either[A[StringIor], A[cats.Id]]]



}

trait CSVProduct[Fields, Out] {
  def read(row: List[String]): Out
}

object CSVProduct {
  def apply[A[_[_]]](implicit csvp: CSVProduct[A[CSVField], A[ErrorOr]]): CSVProduct[A[CSVField], A[ErrorOr]] = csvp

  implicit def toHList[A[_[_]], Fields <: HList, Results <:HList]
  (implicit gf: Generic.Aux[A[CSVField], Fields], gr: Generic.Aux[A[ErrorOr], Results],
   csvp: CSVProduct[Fields, Results]): CSVProduct[A[CSVField], A[ErrorOr]] =
    new CSVProduct[A[CSVField], A[ErrorOr]]{
      def read(row: List[String]): A[ErrorOr] = gr.from(csvp.read(row))
    }


  implicit def hlist[H,
  Fields <: HList,
  Results <: HList]
  (implicit csvF: CSVField[H], csvp: CSVProduct[Fields, Results]):
  CSVProduct[CSVField[H] :: Fields, ErrorOr[H] :: Results] =
    new CSVProduct[CSVField[H] :: Fields, ErrorOr[H] :: Results] {
      def read(row: List[String]): ErrorOr[H] :: Results = {
        if(row.isEmpty) csvF.read("") :: csvp.read(Nil)
        else csvF.read(row.head) :: csvp.read(row.tail)
      }
    }

  implicit def hnil: CSVProduct[HNil, HNil] = new CSVProduct[HNil, HNil]{
    def read(row: List[String]): HNil = HNil
  }
}


trait CSVField[A] {
  def read(s: String): Either[Throwable, A]
  def readUnsafe(s: String): A
}

object CSVField {
  def apply[A](implicit csvf: CSVField[A]): CSVField[A] = csvf

  def csvFieldFactory[A](f: String => A): CSVField[A] = new CSVField[A] {

    def read(s: String): Either[Throwable, A] = Either.catchNonFatal(f(s))

    def readUnsafe(s: String): A = f(s)
  }

  implicit def str: CSVField[String] = csvFieldFactory(x => x)

  implicit def int: CSVField[Int] = csvFieldFactory(x => x.toInt)

  implicit def double: CSVField[Double] = csvFieldFactory(x => x.toDouble)

  implicit def bool: CSVField[Boolean] = csvFieldFactory(x => x.toBoolean)


}

object CSV {
  def apply[A[_[_]]](implicit csv: CSV[A]): CSV[A] = csv

}



