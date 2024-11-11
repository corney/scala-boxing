package corney

sealed trait DomainType[@specialized T] extends Any {
  def carrier: T
}

object DomainType {
  case class Int8(carrier: Byte)   extends AnyVal with DomainType[Byte]
  case class Int16(carrier: Short) extends AnyVal with DomainType[Short]
  case class Int32(carrier: Int)   extends AnyVal with DomainType[Int]
  case class Int64(carrier: Long)  extends AnyVal with DomainType[Long]

  case class Int32a(carrier: Int)  extends AnyVal
  case class Int64a(carrier: Long) extends AnyVal
  class Int32b(val c: Int)         extends AnyVal
}
