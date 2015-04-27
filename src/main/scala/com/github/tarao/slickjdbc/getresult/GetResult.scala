package com.github.tarao
package slickjdbc
package getresult

import scala.util.DynamicVariable
import scala.annotation.implicitNotFound
import slick.jdbc.{GetResult => GR, PositionedResult}
import java.sql.{ResultSet}

trait GetResult {
  private val positionedResult = new DynamicVariable[PositionedResult](null)
  private[getresult] def positionedResultValue =
    Option(positionedResult.value) getOrElse {
      throw new RuntimeException("Column access must be in getResult method")
    }

  def getResult[T](block: => T): GR[T] =
    GR[T] { r => positionedResult.withValue(r) { block } }

  def column[T](index: Int)(implicit
    check: CheckGetter[T],
    binder: TypeBinder[T]
  ): T = binder(positionedResultValue.rs, index)
  def column[T](field: String)(implicit
    check: CheckGetter[T],
    binder: TypeBinder[T]
  ): T = binder(positionedResultValue.rs, field)
  def skip = { positionedResultValue.skip; this }
  def <<[T](implicit
    check: CheckGetter[T],
    binder: TypeBinder[T]
  ): T = column[T](positionedResultValue.skip.currentPos)
  def <<?[T](implicit
    check: CheckGetter[Option[T]],
    binder: TypeBinder[Option[T]]
  ): Option[T] = <<[Option[T]]
}
object GetResult {
  def apply[T](block: GetResult => T): GR[T] =
    GR[T] { r => block(new GetResult {
      override def positionedResultValue = r
    }) }
}

@implicitNotFound(msg = "No conversion rule for type ${T}\n" +
  "[NOTE] You need an implicit of getresult.TypeBinder[${T}] to convert the result.")
sealed trait CheckGetter[+T]
object CheckGetter {
  implicit def valid[T](implicit binder: TypeBinder[T]): CheckGetter[T] =
    new CheckGetter[T] {}
}

trait TypeBinder[+T] {
  def apply(rs: ResultSet, index: Int): T
  def apply(rs: ResultSet, field: String): T
  def map[S](f: T => S): TypeBinder[S] = new TypeBinder[S] {
    def apply(rs: ResultSet, index: Int): S =
      f(TypeBinder.this.apply(rs, index))
    def apply(rs: ResultSet, field: String): S =
      f(TypeBinder.this.apply(rs, field))
  }
}

object TypeBinder {
  type Get[X, R] = (ResultSet, X) => R
  def apply[T](byIndex: Get[Int, T])(byField: Get[String, T]): TypeBinder[T] =
    new TypeBinder[T] {
      def apply(rs: ResultSet, index: Int): T = byIndex(rs, index)
      def apply(rs: ResultSet, field: String): T = byField(rs, field)
    }

  val any: TypeBinder[Any] =
    TypeBinder[Any](_ getObject _)(_ getObject _)

  implicit val blob: TypeBinder[java.sql.Blob] =
    TypeBinder(_ getBlob _)(_ getBlob _)
  implicit val clob: TypeBinder[java.sql.Clob] =
    TypeBinder(_ getClob _)(_ getClob _)
  implicit val nClob: TypeBinder[java.sql.NClob] =
    TypeBinder(_ getNClob _)(_ getNClob _)
  implicit val array: TypeBinder[java.sql.Array] =
    TypeBinder(_ getArray _)(_ getArray _)
  implicit val date: TypeBinder[java.sql.Date] =
    TypeBinder(_ getDate _)(_ getDate _)
  implicit val time: TypeBinder[java.sql.Time] =
    TypeBinder(_ getTime _)(_ getTime _)
  implicit val timestamp: TypeBinder[java.sql.Timestamp] =
    TypeBinder(_ getTimestamp _)(_ getTimestamp _)
  implicit val sqlxml: TypeBinder[java.sql.SQLXML] =
    TypeBinder(_ getSQLXML _)(_ getSQLXML _)
  implicit val ref: TypeBinder[java.sql.Ref] =
    TypeBinder(_ getRef _)(_ getRef _)
  implicit val rowId: TypeBinder[java.sql.RowId] =
    TypeBinder(_ getRowId _)(_ getRowId _)

  implicit val javaBoolean: TypeBinder[Option[java.lang.Boolean]] = any.map {
    case b if b == null => None
    case b: java.lang.Boolean => Some(b)
    case s: String => Some({
      try s.toInt != 0
      catch { case e: NumberFormatException => !s.isEmpty }
    }.asInstanceOf[java.lang.Boolean])
    case n: Number => Some((n.intValue != 0).asInstanceOf[java.lang.Boolean])
    case v => Some((v != 0).asInstanceOf[java.lang.Boolean])
  }
  implicit val scalaBoolean: TypeBinder[Option[Boolean]] =
    javaBoolean.map(_.map(_.asInstanceOf[Boolean]))

  def javaNumber[T](valueOf: String => T): TypeBinder[Option[T]] =
    any.map {
      case v if v == null => None
      case v => Some(valueOf(v.toString))
    }
  def javaFixedNumber[T](
    to: Number => T,
    valueOf: String => T
  ): TypeBinder[Option[T]] = any.map {
    case v if v == null => None
    case v: Number => Some(to(v))
    case v => Some(valueOf(v.toString))
  }

  implicit val javaByte: TypeBinder[Option[java.lang.Byte]] =
    javaFixedNumber({ n => n.byteValue }, { s => java.lang.Byte.valueOf(s) })
  implicit val scalaByte: TypeBinder[Option[Byte]] =
    javaByte.map(_.map(_.asInstanceOf[Byte]))
  implicit val javaShort: TypeBinder[Option[java.lang.Short]] =
    javaFixedNumber({ n => n.shortValue }, { s => java.lang.Short.valueOf(s) })
  implicit val scalaShort: TypeBinder[Option[Short]] =
    javaShort.map(_.map(_.asInstanceOf[Short]))
  implicit val javaInt: TypeBinder[Option[java.lang.Integer]] =
    javaFixedNumber({ n => n.intValue }, { s => java.lang.Integer.valueOf(s) })
  implicit val scalaInt: TypeBinder[Option[Int]] =
    javaInt.map(_.map(_.asInstanceOf[Int]))
  implicit val javaLong: TypeBinder[Option[java.lang.Long]] =
    javaFixedNumber({ n => n.longValue }, { s => java.lang.Long.valueOf(s) })
  implicit val scalaLong: TypeBinder[Option[Long]] =
    javaLong.map(_.map(_.asInstanceOf[Long]))
  implicit val javaDouble: TypeBinder[Option[java.lang.Double]] =
    javaNumber { s => java.lang.Double.valueOf(s) }
  implicit val scalaDouble: TypeBinder[Option[Double]] =
    javaDouble.map(_.map(_.asInstanceOf[Double]))
  implicit val javaFloat: TypeBinder[Option[java.lang.Float]] =
    javaNumber { s => java.lang.Float.valueOf(s) }
  implicit val scalaFloat: TypeBinder[Option[Float]] =
    javaFloat.map(_.map(_.asInstanceOf[Float]))

  implicit val javaBigDecimal: TypeBinder[Option[java.math.BigDecimal]] =
    TypeBinder(_ getBigDecimal _)(_ getBigDecimal _).map(Option(_))
  implicit val scalaBigDecimal: TypeBinder[Option[BigDecimal]] =
    javaBigDecimal.map(_.map(BigDecimal(_)))

  implicit val string: TypeBinder[String] =
    TypeBinder(_ getString _)(_ getString _)
  implicit val bytes: TypeBinder[Array[Byte]] =
    TypeBinder(_ getBytes _)(_ getBytes _)
  implicit val characterStream: TypeBinder[java.io.Reader] =
    TypeBinder(_ getCharacterStream _)(_ getCharacterStream _)
  implicit val binaryStream: TypeBinder[java.io.InputStream] =
    TypeBinder(_ getBinaryStream _)(_ getBinaryStream _)

  implicit val url: TypeBinder[java.net.URL] =
    TypeBinder(_ getURL _)(_ getURL _)
}

trait AutoUnwrapOption {
  implicit def some[T](implicit
    check: NoOption[T], // We do this to enable a diagnostic type
                        // error by CheckGetter.  Otherwise an
                        // implicit expansion of an unknown type fails
                        // on divergence.
    option: TypeBinder[Option[T]]
  ): TypeBinder[T] = option.map(_.get) // throws
}
object AutoUnwrapOption extends AutoUnwrapOption

sealed trait NoOption[+T]
object NoOption {
  implicit def some[T]: NoOption[T] = new NoOption[T] {}
  implicit def ambig1[T]: NoOption[Option[T]] = sys.error("unexpected")
  implicit def ambig2[T]: NoOption[Option[T]] = sys.error("unexpected")
}
