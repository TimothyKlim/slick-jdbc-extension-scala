package com.github.tarao
package slickjdbc
package interpolation

import util.NonEmpty
import scala.annotation.implicitNotFound
import slick.jdbc.{SetParameter => SP, PositionedParameters}

trait CompoundParameter {
  implicit val createSetProduct: SP[Product] = SetProduct

  @inline implicit
  def createSetList[T](implicit c: SP[T]): SetList[T, NonEmpty[T]] =
    new SetList[T, NonEmpty[T]](c)
}
object CompoundParameter extends CompoundParameter

/** SetParameter for non-empty list types. */
class SetList[S, -T <: NonEmpty[S]](val c: SP[S]) extends SP[T] {
  def apply(param: T, pp: PositionedParameters): Unit = {
    param.foreach { item => c.asInstanceOf[SP[Any]](item, pp) }
  }
}

/** SetParameter for product types especially for case classes. */
object SetProduct extends SP[Product] {
  def apply(prod: Product, pp: PositionedParameters): Unit =
    for (v <- prod.productIterator) v match {
      case p: Product => SetProduct(p, pp)
      case v => SP.SetSimpleProduct(Tuple1(v), pp)
    }
}

@implicitNotFound(msg = "Unsupported parameter type: ${T}.\n" +
  "[NOTE] You need an implicit of slick.jdbc.SetParameter[${T}] to pass a value of the type.")
sealed trait CheckParameter[-T]
object CheckParameter {
  implicit def valid[T](implicit c: SP[T]): CheckParameter[T] =
    new CheckParameter[T] {}
}

@implicitNotFound(msg = "A product is passed.\n" +
  "[NOTE] Use interpolation.CompoundParameter trait to enable passing a product.")
sealed trait CheckProduct[-T]
object CheckProduct {
  implicit def valid[T](implicit c: SP[T]): CheckProduct[T] =
    new CheckProduct[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}.\n" +
  "[NOTE] A list is not allowed since it may be empty and breaks the query.\n" +
  "[NOTE] Pass a util.NonEmpty[] if you know that it is not empty.")
sealed trait CheckList[-T]
object CheckList {
  implicit def valid[T](implicit c: SP[T]): CheckList[T] =
    new CheckList[T] {}
}

@implicitNotFound(msg = "A non-empty list is passed.\n" +
  "[NOTE] Use interpolation.CompoundParameter trait to enable passing a non-empty list.")
sealed trait CheckNonEmpty[-T]
object CheckNonEmpty {
  implicit def valid[T](implicit c: SP[T]): CheckNonEmpty[T] =
    new CheckNonEmpty[T] {}
}

sealed trait NoOptionNonEmpty[-T]
object NoOptionNonEmpty {
  implicit def valid[T]: NoOptionNonEmpty[T] = new NoOptionNonEmpty[T] {}
  // $COVERAGE-OFF$
  implicit def ambig1[T]: NoOptionNonEmpty[Option[NonEmpty[T]]] =
    sys.error("unexpected")
  implicit def ambig2[T]: NoOptionNonEmpty[Option[NonEmpty[T]]] =
    sys.error("unexpected")
  // $COVERAGE-ON$
}

@implicitNotFound(msg = "A maybe-non-empty list is passed.\n" +
  "[NOTE] Break it into Some(_) or None to confirm that it is not empty.")
sealed trait CheckOptionNonEmpty[-T]
object CheckOptionNonEmpty {
  implicit def valid[T](implicit
    check: NoOptionNonEmpty[T],
    c: SP[T]
  ): CheckOptionNonEmpty[T] = new CheckOptionNonEmpty[T] {}
}

sealed trait NoOption[-T]
object NoOption {
  implicit def valid[T]: NoOption[T] = new NoOption[T] {}
  // $COVERAGE-OFF$
  implicit def ambig1[S]: NoOption[Option[S]] = sys.error("unexpected")
  implicit def ambig2[S]: NoOption[Option[S]] = sys.error("unexpected")
  // $COVERAGE-ON$
}

@implicitNotFound(msg = "Illegal parameter type: ${T}\n" +
  "[NOTE] An option is not allowed since it may be none and breaks the query.\n" +
  "[NOTE] Break it into Some(_) or None to confirm that it has a value.")
sealed trait CheckOption[-T]
object CheckOption {
  implicit def valid[T](implicit
    check: NoOption[T],
    c: SP[T]
  ): CheckOption[T] = new CheckOption[T] {}
}
