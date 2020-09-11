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

  def ++=(inst:List[A[cats.Id]]) = InsertQuery(inst, this)

  def ++=(inst:Seq[A[cats.Id]]) = InsertQuery(inst.toList, this)

  def ++=(inst: A[cats.Id]) = InsertQuery(List(inst), this)
}

case class InsertQuery[A[_[_]]](toInsert: List[A[Id]], table: TableQuery[A]) extends FQuery[A[DBAttr]] {
  def shapeToList: List[DBAttr[_]] = table.shapeToList
  val shape = table.shape

  def ++=(inst: List[A[Id]]) = this.copy(toInsert = inst ++ toInsert)

  def ++=(inst: A[Id]) = this.copy(toInsert = inst :: toInsert)
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