package djala.sourceless.fquery

import djala.idless.Validate
import djala.sourceless.ValidShape
import djala.sourceless.sources.Database
import shapeless._
import shapeless.ops.hlist.ToTraversable

import scala.languageFeature.higherKinds

trait FQuery[A] {
  import FQuery._
  type B = A
  val shape: A

  def shapeToList: List[DBAttr[_]]

  def map[B](f: A => B)(implicit vs: ValidShape[B]): FQuery[B] = {
    flatMap{ a => ShapeQuery(f(a))}
  }

  def flatMap[B](f: A => FQuery[B]): FQuery[B] = {
    WrappingQuery(f(shape), this)
  }

  def filter(f: A => Predicate): FQuery[A] = {
    Filter(f(shape), this)
  }


  def join[B](that: FQuery[B])(on: (A, B) => Predicate)(implicit vs: ValidShape[(A, B)]): FQuery[(A, B)] = {
    JoinQuery(this, that, on(this.shape, that.shape))
  }


  def toLTQ = {
    LTQuery.fromFQuery(this)
  }
}

case class TableQuery[A[_[_]]](name: String,
                               override val shape: A[DBAttr],
                               parent: Option[DBAttr[_]]=None,
                               validations: Option[A[Validate]] = None,
                               database: Database)
                              (implicit vs: ValidShape[A[DBAttr]]) extends FQuery[A[DBAttr]]  {

  override def shapeToList: List[DBAttr[_]] = vs.toList(shape)

  def validationsToList[L <: HList](implicit
                                    g: Generic.Aux[A[Validate], L],
                                    tt: ToTraversable.Aux[L, List, Validate[_]]): List[Validate[_]] = {
    validations.map{ x =>  tt(g.to(x))} getOrElse Nil
  }
  val table = name

  def ++=(inst:List[A[cats.Id]])(implicit vi: ValidInsert[A[Id]]) = InsertQuery(inst, this)(vi)

  def ++=(inst:Seq[A[cats.Id]])(implicit vi: ValidInsert[A[Id]]) = InsertQuery(inst.toList, this)(vi)

  def ++=(inst: A[cats.Id])(implicit vi: ValidInsert[A[Id]]) = InsertQuery(List(inst), this)(vi)
}

case class InsertQuery[A[_[_]]](toInsert: List[A[Id]], table: TableQuery[A])(implicit vis: ValidInsert[A[Id]]) extends FQuery[A[DBAttr]] {
  def shapeToList: List[DBAttr[_]] = table.shapeToList
  val shape = table.shape

  def valuesToLTQuery: List[List[LTQuery]] = toInsert.map(vis.valuesAsLTQuery)

  def ++=(inst: List[A[Id]]) = this.copy(toInsert = inst ++ toInsert)

  def ++=(inst: A[Id]) = this.copy(toInsert = inst :: toInsert)



}


trait DBAttrToLTValue[A]{
  def toLTValue(a: A): ValueLT
}

object DBAttrToLTValue {
  implicit def apply[A](implicit v: DBAttrToLTValue[A]): DBAttrToLTValue[A] = v
  implicit def stringToDBAttr: DBAttrToLTValue[String] = new DBAttrToLTValue[String] {
    override def toLTValue(a: String): ValueLT = StringValueLT(a)
  }
  implicit def intToDBAttr: DBAttrToLTValue[Int] = new DBAttrToLTValue[Int] {
    override def toLTValue(a: Int): ValueLT = IntValueLT(a)
  }
}
trait ValidInsert[A] {
  def valuesAsLTQuery(a: A): List[ValueLT]
}

trait ValidInsertHList[A] {
  def valuesAsLTQuery(a: A): List[ValueLT]
}

object ValidInsertHList {
  def apply[A](implicit vi: ValidInsert[A]): ValidInsert[A] = vi

  implicit def validinsertHList[H, T <: HList](implicit dbAttrToLTValue: DBAttrToLTValue[H], vi: ValidInsertHList[T]): ValidInsertHList[H :: T] = new  ValidInsertHList[H :: T] {
    override def valuesAsLTQuery(a: H :: T): List[ValueLT] = dbAttrToLTValue.toLTValue(a.head) :: vi.valuesAsLTQuery(a.tail)
  }

  implicit def validinserthnil: ValidInsertHList[HNil] = new ValidInsertHList[HNil] {
    override def valuesAsLTQuery(a: HNil): List[ValueLT] = Nil
  }
}

trait ValidInsertLowPriority {
  implicit def toHList[A[_[_]], HL <: HList](implicit g: Generic.Aux[A[Id], HL], vi: ValidInsertHList[HL]): ValidInsert[A[Id]] = new ValidInsert[A[Id]] {
    override def valuesAsLTQuery(a: A[Id]): List[ValueLT] =  vi.valuesAsLTQuery(g.to(a))
  }
}

object ValidInsert extends ValidInsertLowPriority {
  implicit def apply[A[_[_]]](implicit vi: ValidInsert[A[Id]]): ValidInsert[A[Id]] = vi
}



object FQuery {

  case class Filter[A](p: Predicate, q: FQuery[A]) extends FQuery[A] {
    val shape = q.shape

    def shapeToList: List[DBAttr[_]] = q.shapeToList
  }

  case class ShapeQuery[A](shape: A)(implicit vs: ValidShape[A]) extends FQuery[A] {
    def shapeToList: List[DBAttr[_]] = vs.toList(shape)
  }

  case class WrappingQuery[A, B](l: FQuery[A], r: FQuery[B]) extends FQuery[A] {
    val shape = l.shape

    def shapeToList: List[DBAttr[_]] = l.shapeToList
  }


  case class JoinQuery[A, B](l: FQuery[A], r: FQuery[B], on: Predicate) extends FQuery[(A, B)] {
    val shape = (l.shape, r.shape)
    override def shapeToList: List[DBAttr[_]] = l.shapeToList ++ r.shapeToList
  }
}


trait Rep[A] {
  def show: String
}

object Rep {
  implicit def repString(str: String) = new Rep[String] {
    def show = str
  }
}



trait DBAttr[A] extends FQuery[DBAttr[A]] {
  val name: String
  val table: String
  val parent: Option[DBAttr[_]]


  def shapeToList: List[DBAttr[_]] = List(this)
  val shape = this

  implicit def attrRep(dbattr: DBAttr[A]): Rep[A] = new Rep[A] {
    def show = name
  }

  def ===(that: DBAttr[A]): Predicate = Eq(this, that)
}

case class DBTableAttribute[A](name: String, override val table: String, parent: Option[DBAttr[_]]=None) extends DBAttr[A] with Rep[A] {
  def show = name
}

case class ForeignKey[A](name: String, override val table: String, otherCol: DBAttr[A], parent: Option[DBAttr[_]]=None) extends DBAttr[A] {
}

case class DBAttribute[A](name: String, parent: Option[DBAttr[_]] = None) extends DBAttr[A] with Rep[A] {
  def show = name
  val table = ""

  def asTableAttribute(tableName: String) = DBTableAttribute[A](name, tableName)
}



trait Predicate extends FQuery[Boolean] {
  override def shapeToList = Nil

  val shape = true
}
case class Eq[A](l: FQuery[A], r: FQuery[A]) extends Predicate