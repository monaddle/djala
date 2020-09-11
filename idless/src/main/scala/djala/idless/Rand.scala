package djala.idless
import shapeless._

sealed trait Validated[A]



trait Rand[A, B] {
  def withConstraints(b: B): A
  def withoutConstraints: A
}


object Rand {

  def apply[A[_[_]]](constraints: Option[A[Validate]]=None)(implicit rand: Rand[A[cats.Id], A[Validate]]): Rand[A[cats.Id], A[Validate]] = rand

  implicit def product[A[_[_]], L <: HList, LV <: HList](implicit
                                                         g: Generic.Aux[A[Id], L],
                                                         gv: Generic.Aux[A[Validate], LV],
                                                         r: Rand[L, LV]): Rand[A[Id], A[Validate]] = new Rand[A[Id], A[Validate]]{
    def withConstraints(b: A[Validate]): A[Id] = g.from(r.withConstraints(gv.to(b)))
    def withoutConstraints: A[Id] = g.from(r.withoutConstraints)

  }
  case class NumericConstraints[A](lt: Option[A], gt: Option[A], eq: Option[A])(implicit val n: Numeric[A]) {
    def + (that: NumericConstraints[A]) =  {
      val res = NumericConstraints(
        if(that.lt.isDefined) that.lt else this.lt,
        if(that.gt.isDefined) that.lt else this.gt,
        if(that.eq.isDefined) that.lt else this.eq
      )
      res
    }
  }

  import Validate._

  def getNumericContraints[A](v: Validate[A])(implicit n: Numeric[A]): NumericConstraints[A] = {
    v match {
      case Gt(i) => NumericConstraints(None, Some(i), None)
      case Lt(i) => NumericConstraints(Some(i), None, None)
      case And(l, r) => getNumericContraints(l) + getNumericContraints(r)
      case NoValidation() => NumericConstraints(None, None, None)
    }
  }

  implicit def number: Rand[Id[Int], Validate[Int]] = new Rand[Id[Int], Validate[Int]] {
    override def withoutConstraints: Id[Int] = scala.util.Random.nextInt
    def withConstraints(v: Validate[Int]): Int = {
      val constraints = getNumericContraints(v)
      val numeric = constraints.n

      if(constraints.eq.isDefined) {
        constraints.eq.get
      }
      else if(constraints.lt.isDefined) {
        val ceiling = constraints.lt.get
        if(constraints.gt.isDefined) {
          val floor = constraints.gt.get
          val dist = numeric.minus(ceiling, floor + 1)
          util.Random.nextInt(dist) + floor + 1
        } else {
          util.Random.nextInt(ceiling)
        }
      }
      else if(constraints.gt.isDefined) {
        val floor = constraints.gt.get
        if(floor > 0) {
          math.random() * (Int.MaxValue - floor) + floor toInt
        }
        else {
          math.random() * (Int.MaxValue + floor) + (math.random * math.abs(floor)) toInt
        }
      } else {
        util.Random.nextInt(Int.MaxValue)
      }
    }
  }

  implicit def str: Rand[Id[String], Validate[String]] = new Rand[Id[String], Validate[String]] {
    def withConstraints(v: Validate[String]) = {
      util.Random.alphanumeric.take(50).mkString
    }

    override def withoutConstraints: Id[String] = {
      util.Random.alphanumeric.take(50).mkString
    }
  }

  implicit def hlist[H, T <: HList, TV <: HList](implicit rh: Rand[Id[H], Validate[H]], rt:Rand[T, TV]) = new Rand[Id[H] :: T, Validate[H]:: TV] {
    def withConstraints(b: Validate[H]:: TV) = rh.withConstraints(b.head) :: rt.withConstraints(b.tail)

    override def withoutConstraints: Id[H] :: T = rh.withoutConstraints :: rt.withoutConstraints
  }



  implicit def hnil[T]: Rand[HNil, HNil] = new Rand[HNil, HNil] {
    def withConstraints(b: HNil) = HNil
    def withoutConstraints = HNil
  }
}
