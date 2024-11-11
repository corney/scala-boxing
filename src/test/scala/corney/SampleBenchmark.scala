package corney

import org.openjdk.jmh.annotations.{
  Benchmark,
  BenchmarkMode,
  Fork,
  Measurement,
  Mode,
  OutputTimeUnit,
  Scope,
  Setup,
  State,
  Warmup,
}

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ArraySeq
import scala.runtime.BoxesRunTime
import scala.util.Random

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(
  iterations = 2, // число итераций для прогрева нашей функции
  time = 2,       // время выполнения итерации прогрева в секундах
)
@Measurement(
  iterations = 5, // число проверочных итераций
  time = 5,       // время выполнения итерации в секундах
  // количество запусков метода помеченного @Benchmark, за одно инстанцированние класса.
  // кратно увеличивает результаты измерений
  batchSize = 1,
)
@Fork(value = 1) // число форков для каждого измерения
class SampleBenchmark {
  val N = 10000

  val array: Array[Long]           = new Array[Long](N)
  var arraySeq: ArraySeq[Long]     = _
  var indexedSeq: IndexedSeq[Long] = _

  val domainArray: Array[DomainType.Int64] =
    new Array[DomainType.Int64](array.length)

  var domainSeq: ArraySeq[DomainType.Int64] = _

  val intArray: Array[Int]                     = new Array[Int](N)
  var intArraySeq: ArraySeq[Int]               = _
  val domainIntArray: Array[DomainType.Int32]  = new Array[DomainType.Int32](N)
  var domainIntSeq: ArraySeq[DomainType.Int32] = _

  val arr: Array[Int]         = Array(3, 8)
  val arrSeq: ArraySeq[Int]   = ArraySeq.unsafeWrapArray[Int](arr)
  val idxSeq: IndexedSeq[Int] = IndexedSeq(3, 8)

  //  @Benchmark
  def askAndSumArrSeq(): Int = {
    arrSeq(0) + arrSeq(1)
  }

  //  @Benchmark
  def askAndSumArr(): Int = {
    arr(0) + arr(1)
  }

  //  @Benchmark
  def askAndSumIdxSeq(): Int = {
    idxSeq(0) + idxSeq(1)
  }

  @Setup
  def init(): Unit = {

    array
      .indices
      .foreach { i =>
        array(i) = Random.nextLong()
        domainArray(i) = DomainType.Int64(array(i))

        intArray(i) = Random.nextInt()
        domainIntArray(i) = DomainType.Int32(intArray(i))
      }

    arraySeq = ArraySeq.from(array)
    intArraySeq = ArraySeq.from(intArray)
    //    val domainAArray = array.asInstanceOf[Array[DomainType.Int64a]]

    domainSeq = ArraySeq.unsafeWrapArray(domainArray)
    domainIntSeq = ArraySeq.unsafeWrapArray((domainIntArray))
    indexedSeq = IndexedSeq.from(array)
  }

  //  @Benchmark
  def iterateOverArray(): Long = {
    var i     = 0
    var total = 0L
    while (i < array.length) {
      total += array(i)
      i += 1
    }
    total
  }

  @Benchmark
  def iterateOverArraySeq(): Long = {
    var i     = 0
    var total = 0L
    while (i < arraySeq.length) {
      total += arraySeq(i)
      i += 1
    }
    total
  }

  //  @Benchmark
  def iterateOverIntArraySeq(): Int = {
    var i     = 0
    var total = 0
    while (i < intArraySeq.length) {
      total += intArraySeq(i)
      i += 1
    }
    total
  }

  //  @Benchmark
  def iterateOverIndexedSeq(): Long = {
    var i     = 0
    var total = 0L
    while (i < indexedSeq.length) {
      total += indexedSeq(i)
      i += 1
    }
    total
  }

  //  @Benchmark
  def totalFoldLeftArr(): Long = {
    array.foldLeft(0L) { case (sum, data) =>
      sum + data
    }
  }

  // @Benchmark
  def totalFoldLeftArrSeq(): Long = {
    arraySeq.foldLeft(0L) { case (sum, data) =>
      sum + data
    }
  }

  //  @Benchmark
  def iterateOverRange(): Long = {
    var total = 0L
    arraySeq
      .indices
      .foreach { i =>
        total += arraySeq(i)
      }
    total
  }

  //  @Benchmark
  def totalFoldLeftPlus(): Long = {
    array.foldLeft(0L)(plusS[Long])
  }

  def plusS[@specialized T : Numeric](a: T, b: T): T =
    implicitly[Numeric[T]].plus(a, b)

  //  @Benchmark
  def totalWhileLoop(): Long = {
    val n     = array.length
    var i     = 0
    var total = 0L
    while (i < n) {
      total += array(i)
      i += 1
    }
    total
  }

  //  @Benchmark
  def totalWhileLoopS(): Long = {
    val n     = array.length
    var i     = 0
    var total = 0L
    while (i < n) {
      total = plusS[Long](total, array(i))
      i += 1
    }
    total
  }

  def foldLeftS[@specialized T](
    seq: ArraySeq[T],
  )(zero: T)(lambda: (T, T) => T): T = {

    var total = zero
    seq
      .indices
      .foreach { i =>
        total = lambda.apply(total, seq(i))
      }
    total
  }

  @Benchmark
  def totalFoldLeftS(): Long = {
    foldLeftS[Long](arraySeq)(0L)((a, b) => a + b)
  }

  def foldLeftI[@specialized T](
    seq: ArraySeq[T],
  )(zero: T)(lambda: (T, T) => T): T = {

    var total = zero
    seq
      .iterator
      .foreach { l =>
        total = lambda.apply(total, l)
      }
    total
  }

  @Benchmark
  def totalFoldLeftI(): Long = {
    foldLeftI[Long](arraySeq)(0L)((a, b) => a + b)
  }

  //  @Benchmark
  def totalDomain64A(): DomainType.Int64 = {
    var total: DomainType.Int64 = DomainType.Int64(0L)
    domainArray
      .indices
      .foreach(i =>
        total = DomainType.Int64(domainArray(i).carrier + total.carrier),
      )
    total
  }

  //  @Benchmark
  def totalDomain32A(): DomainType.Int32 = {
    var total: DomainType.Int32 = DomainType.Int32(0)
    domainIntArray
      .indices
      .foreach(i =>
        total = DomainType.Int32(domainIntArray(i).carrier + total.carrier),
      )
    total
  }

  //  @Benchmark
  def totalDomain64B(): DomainType.Int64 = {
    var total: DomainType.Int64 = DomainType.Int64(0L)
    domainSeq
      .indices
      .foreach(i =>
        total = DomainType.Int64(total.carrier + domainSeq(i).carrier),
      )
    total
  }

  //  @Benchmark
  def totalDomain32B(): DomainType.Int32 = {
    var total: DomainType.Int32 = DomainType.Int32(0)
    domainIntSeq
      .indices
      .foreach(i =>
        total = DomainType.Int32(total.carrier + domainIntSeq(i).carrier),
      )
    total
  }

  //  @Benchmark
  def totalDomainC(): DomainType.Int64 = {
    foldLeftS[DomainType.Int64](domainSeq)(DomainType.Int64(0L))((a, b) =>
      DomainType.Int64(a.carrier + b.carrier),
    )
  }

  def iLikeToBoxIt(): Int = {
    // BIPUSH 10
    // ISTORE 1
    val k = 10

    // ILOAD 1
    // INVOKESTATIC scala/runtime/BoxesRunTime.boxToInteger (I)Ljava/lang/Integer;
    // ASTORE 2
    val i = BoxesRunTime.boxToInteger(k)

    // ALOAD 2
    // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    // ISTORE 3
    val j = BoxesRunTime.unboxToInt(i)

    // ILOAD 3
    // IRETURN
    j
  }

}
