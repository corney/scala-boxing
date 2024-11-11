# Boxing в Scala. Когда это плохо и как с ним бороться?

## Как запустить бенчмарки?

- Пометить аннотацией `@Benchmark` интересующие нас методы в классе [SampleBenchmark](src/test/scala/corney/SampleBenchmark.scala)
- Запустить бенчмарки:
```shell
sbt Jmh/run
```

## Постановка проблемы

```java
java.lang.Integer i;
int ii;
java.lang.Long l;
long ll;
```

```scala
val i: Int // Both java.lang.Integer and int
val l: Long // Both java.lang.Long and long
```

```java
package scala.runtime;

class BoxesRunTime {
    public static java.lang.Integer boxToInteger(int i);

    public static int unboxToInt(Object i);
}
```

- Типу `scala.Int` соответсвуют два типа JVM: примитивный тип `int` и обертка `java.lang.Integer`
- Между ними происходят автоматические преобразования:

```scala
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

```

Обратите внимание на типы, которые назначились переменным:

```
  public iLikeToBoxIt()I
   L0
    LINENUMBER 50 L0
    BIPUSH 10
    ISTORE 1
   L1
    LINENUMBER 51 L1
    ILOAD 1
    INVOKESTATIC scala/runtime/BoxesRunTime.boxToInteger (I)Ljava/lang/Integer;
    ASTORE 2
   L2
    LINENUMBER 52 L2
    ALOAD 2
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    ISTORE 3
   L3
    LINENUMBER 53 L3
    ILOAD 3
    IRETURN
   L4
    LOCALVARIABLE k I L1 L4 1
    LOCALVARIABLE i Ljava/lang/Integer; L2 L4 2
    LOCALVARIABLE j I L3 L4 3
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L4 0
    MAXSTACK = 1
    MAXLOCALS = 4
```

- Что произойдет, если мы в том же коде явно укажем тип переменных?

```scala
def iLikeToBoxIt(): Int = {

  // BIPUSH 10
  // ISTORE 1
  val k: Int = 10

  // ILOAD 1
  // INVOKESTATIC scala/runtime/BoxesRunTime.boxToInteger (I)Ljava/lang/Integer;
  // INVOKEVIRTUAL scala/Predef$.Integer2int (Ljava/lang/Integer;)I
  // ISTORE 2
  val i: Int = BoxesRunTime.boxToInteger(k)

  // ILOAD 2
  // INVOKESTATIC scala/runtime/BoxesRunTime.boxToInteger (I)Ljava/lang/Integer;
  // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
  // ISTORE 3
  val j: Int = BoxesRunTime.unboxToInt(i)
  j
}
```

```
  public iLikeToBoxIt()I
   L0
    LINENUMBER 50 L0
    BIPUSH 10
    ISTORE 1
   L1
    LINENUMBER 51 L1
    GETSTATIC scala/Predef$.MODULE$ : Lscala/Predef$;
    ILOAD 1
    INVOKESTATIC scala/runtime/BoxesRunTime.boxToInteger (I)Ljava/lang/Integer;
    INVOKEVIRTUAL scala/Predef$.Integer2int (Ljava/lang/Integer;)I
    ISTORE 2
   L2
    LINENUMBER 52 L2
    ILOAD 2
    INVOKESTATIC scala/runtime/BoxesRunTime.boxToInteger (I)Ljava/lang/Integer;
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    ISTORE 3
   L3
    LINENUMBER 53 L3
    ILOAD 3
    IRETURN
   L4
    LOCALVARIABLE k I L1 L4 1
    LOCALVARIABLE i I L2 L4 2
    LOCALVARIABLE j I L3 L4 3
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L4 0
    MAXSTACK = 2
    MAXLOCALS = 4
 ```

# обработка коллекций

Попробуем создать простейшую коллекцию.

```scala
val seq: IndexedSeq[Int] = IndexedSeq(3, 8)

def askAndSum(): Int = {
  seq(0) + seq(1)
}
```

Что под капотом?

```scala
val seq: IndexedSeq[Int] = IndexedSeq(3, 8)

def askAndSum(): Int = {
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.indexedSeq ()Lscala/collection/immutable/IndexedSeq;
  // ICONST_0
  // INVOKEINTERFACE scala/collection/immutable/IndexedSeq.apply (I)Ljava/lang/Object; (itf)
  // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.indexedSeq ()Lscala/collection/immutable/IndexedSeq;
  // ICONST_1
  // INVOKEINTERFACE scala/collection/immutable/IndexedSeq.apply (I)Ljava/lang/Object; (itf)
  // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
  // IADD
  // IRETURN
  seq(0) + seq(1)
}
```

```
  public askAndSumidxSeq()I  
   L0
    LINENUMBER 57 L0
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.indexedSeq ()Lscala/collection/immutable/IndexedSeq;
    ICONST_0
    INVOKEINTERFACE scala/collection/immutable/IndexedSeq.apply (I)Ljava/lang/Object; (itf)
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.indexedSeq ()Lscala/collection/immutable/IndexedSeq;
    ICONST_1
    INVOKEINTERFACE scala/collection/immutable/IndexedSeq.apply (I)Ljava/lang/Object; (itf)
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    IADD
    IRETURN
   L1
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L1 0
    MAXSTACK = 3
    MAXLOCALS = 1
```

Попробуем то же, но с ArraySeq (которую неявно оборачивают в SeqOps при преобразовании к Seq[Int])

```scala
val seq: ArraySeq[Int] = ArraySeq(3, 8)

def askAndSum(): Int = {
  seq(0) + seq(1)
}
```

```scala
val seq: ArraySeq[Int] = ArraySeq(3, 8)

def askAndSum(): Int = {
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.arrSeq ()Lscala/collection/immutable/ArraySeq;
  // ICONST_0
  // INVOKEVIRTUAL scala/collection/immutable/ArraySeq.apply (I)Ljava/lang/Object;
  // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.arrSeq ()Lscala/collection/immutable/ArraySeq;
  // ICONST_1
  // INVOKEVIRTUAL scala/collection/immutable/ArraySeq.apply (I)Ljava/lang/Object;
  // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
  // IADD
  // IRETURN
  seq(0) + seq(1)
}
```

Видим тот же результат:

```
 public askAndSumArrSeq()I  
   L0
    LINENUMBER 47 L0
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.arrSeq ()Lscala/collection/immutable/ArraySeq;
    ICONST_0
    INVOKEVIRTUAL scala/collection/immutable/ArraySeq.apply (I)Ljava/lang/Object;
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.arrSeq ()Lscala/collection/immutable/ArraySeq;
    ICONST_1
    INVOKEVIRTUAL scala/collection/immutable/ArraySeq.apply (I)Ljava/lang/Object;
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToInt (Ljava/lang/Object;)I
    IADD
    IRETURN
   L1
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L1 0
    MAXSTACK = 3
    MAXLOCALS = 1
```

Теперь попробуем с массивом:

```scala
val seq: Array[Int] = Array(3, 8)
def askAndSum(): Int = {
  seq(0) + seq(1)
}
```

Видим совсем другую картину:

```scala
val seq: Array[Int] = Array(3, 8)

def askAndSum(): Int = {
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.arr ()[I
  // ICONST_0
  // IALOAD
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.arr ()[I
  // ICONST_1
  // IALOAD
  // IADD
  // IRETURN
  seq(0) + seq(1)
}
```

```
  public askAndSumArr()I  
   L0
    LINENUMBER 52 L0
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.arr ()[I
    ICONST_0
    IALOAD
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.arr ()[I
    ICONST_1
    IALOAD
    IADD
    IRETURN
   L1
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L1 0
    MAXSTACK = 3
    MAXLOCALS = 1
```

## Бенчмарки

Прогоним все это через бенчмарки.

Сначала наши единичные функции:

```scala
@Benchmark
def askAndSumArrSeq(): Int = {
  arrSeq(0) + arrSeq(1)
}

@Benchmark
def askAndSumArr(): Int = {
  arr(0) + arr(1)
}

@Benchmark
def askAndSumIdxSeq(): Int = {
  indexedSeq(0) + indexedSeq(1)
}
```

Результат выглядит так:

```
[info] Benchmark                  Mode  Cnt  Score   Error  Units
[info] BoxingJmh.askAndSumArr     avgt    5  0.725 ± 0.049  ns/op
[info] BoxingJmh.askAndSumArrSeq  avgt    5  0.903 ± 0.028  ns/op
[info] BoxingJmh.askAndSumIdxSeq  avgt    5  1.461 ± 0.061  ns/op
```

Видно, что ArraySeq в данной операции чуть-чуть медленней, чем массив. IndexedSeq опаздывает не только из-за операции
анбоксинга, но еще и за счет более дорогой операции извлечения значения.

Попробуем ту же операцию, но на больших данных:

```scala
val array: Array[Long] = new Array[Long](10000)
var arraySeq: ArraySeq[Long] = _
var indexedSeq: IndexedSeq[Long] = _

@Benchmark
def iterateOverArray(): Long = {
  var i = 0
  var total = 0L
  while (i < array.length) {
    total += array(i)
    i += 1
  }
  total
}

@Benchmark
def iterateOverArraySeq(): Long = {
  var i = 0
  var total = 0L
  while (i < arraySeq.length) {
    total += arraySeq(i)
    i += 1
  }
  total
}

@Benchmark
def iterateOverIndexedSeq(): Long = {
  var i = 0
  var total = 0L
  while (i < indexedSeq.length) {
    total += indexedSeq(i)
    i += 1
  }
  total
}

```

```
[info] Benchmark                        Mode  Cnt   Score   Error  Units
[info] BoxingJmh.iterateOverArray       avgt    5   3.182 ± 0.056  us/op
[info] BoxingJmh.iterateOverArraySeq    avgt    5   3.156 ± 0.105  us/op
[info] BoxingJmh.iterateOverIndexedSeq  avgt    5  16.908 ± 0.327  us/op
```

Видно, что на больших числах скорость итерации по массиву и по ArraySeq практически одинакова.
Почему это так, мы поймем, если посмотрим на специализированную реализацию:

```scala
sealed abstract class ArraySeq[+A]
  extends AbstractSeq[A]
    with IndexedSeq[A]
    with IndexedSeqOps[A, ArraySeq, ArraySeq[A]]
    with StrictOptimizedSeqOps[A, ArraySeq, ArraySeq[A]]
    with EvidenceIterableFactoryDefaults[A, ArraySeq, ClassTag]
    with Serializable {
  // . . .
}

final class ofLong(val unsafeArray: Array[Long]) extends ArraySeq[Long] {
  def apply(i: Int): Long = unsafeArray(i)
}
```

```scala
final class ofLong(val unsafeArray: Array[Long]) extends ArraySeq[Long] {
  // ALOAD 0
  // INVOKEVIRTUAL scala/collection/immutable/ArraySeq$ofLong.unsafeArray ()[J
  // ILOAD 1
  // LALOAD
  // LRETURN
  def apply(i: Int): Long = unsafeArray(i)
}
```

```
public apply(I)J throws java/lang/ArrayIndexOutOfBoundsException 
    // parameter final  i
   L0
    LINENUMBER 533 L0
    ALOAD 0
    INVOKEVIRTUAL scala/collection/immutable/ArraySeq$ofLong.unsafeArray ()[J
    ILOAD 1
    LALOAD
   L1
    LINENUMBER 533 L1
    LRETURN
   L2
    LOCALVARIABLE this Lscala/collection/immutable/ArraySeq$ofLong; L0 L2 0
    LOCALVARIABLE i I L0 L2 1
    MAXSTACK = 2
    MAXLOCALS = 2
```

То есть, при работе со специализированной ArraySeq мы получаем доступ практически такой же, как и к Array

## Вывод

Если мы хотим избежать излишнего боксинга и анбоксинга, то примитивные типы мы должны хранить в Array или в ArraySeq

# Обход массива и лямбда-функции

Конечно, мы можем везде использовать `var i & while`, но это немножко не scala-way

Попробуем scala-way:

```scala
@Benchmark
def totalFoldLeftArr(): Long = {
  array.foldLeft(0L) { case (sum, data) =>
    sum + data
  }
}

@Benchmark
def totalFoldLeftArrSeq(): Long = {
  arraySeq.foldLeft(0L) { case (sum, data) =>
    sum + data
  }
}
```

Измерим их (для сравнения, рядом поставим старый добрый iterateOverArraySeq):

```
[info] Benchmark                      Mode  Cnt   Score    Error  Units
[info] BoxingJmh.iterateOverArraySeq  avgt    5   3.189 ±  0.168  us/op
[info] BoxingJmh.totalFoldLeftArr     avgt    5  39.272 ± 18.445  us/op
[info] BoxingJmh.totalFoldLeftArrSeq  avgt    5  37.610 ±  2.917  us/op
```

Видим возрастание времени операции больше, чем в 10 раз.
Почему? Смотрим байт-код (для примера возьмем байт-код totalFoldLeftArr)

```
public totalFoldLeftArr()J
   L0
    GETSTATIC scala/collection/ArrayOps$.MODULE$ : Lscala/collection/ArrayOps$;
   L1
    LINENUMBER 108 L1
    GETSTATIC scala/Predef$.MODULE$ : Lscala/Predef$;
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.array ()[J
    INVOKEVIRTUAL scala/Predef$.longArrayOps ([J)Ljava/lang/Object;
    LCONST_0
    INVOKESTATIC scala/runtime/BoxesRunTime.boxToLong (J)Ljava/lang/Long;
    INVOKEDYNAMIC apply$mcJJJ$sp()Lscala/runtime/java8/JFunction2$mcJJJ$sp; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.altMetafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
      // arguments:
      (JJ)J, 
      // handle kind 0x6 : INVOKESTATIC
      vs/core/BoxingJmh.$anonfun$totalFoldLeftArr$1(JJ)J, 
      (JJ)J, 
      1
    ]
    INVOKEVIRTUAL scala/collection/ArrayOps$.foldLeft$extension (Ljava/lang/Object;Ljava/lang/Object;Lscala/Function2;)Ljava/lang/Object;
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToLong (Ljava/lang/Object;)J
    LRETURN
   L2
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L2 0
    MAXSTACK = 4
    MAXLOCALS = 1
```

```scala
@Benchmark
def totalFoldLeftArrSeq(): Long = {
  // ALOAD 0
  // INVOKEVIRTUAL vs/core/BoxingJmh.array ()[J
  // INVOKEVIRTUAL scala/Predef$.longArrayOps ([J)Ljava/lang/Object;
  // LCONST_0
  // INVOKESTATIC scala/runtime/BoxesRunTime.boxToLong (J)Ljava/lang/Long;
  // INVOKEDYNAMIC apply$mcJJJ$sp()Lscala/runtime/java8/JFunction2$mcJJJ$sp; [
  // java/lang/invoke/LambdaMetafactory.altMetafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
  // arguments:
  // (JJ)J,
  // handle kind 0x6 : INVOKESTATIC
  // vs/core/BoxingJmh.$anonfun$totalFoldLeftArr$1(JJ)J,
  // (JJ)J,
  // 1
  // ]
  // INVOKEVIRTUAL scala/collection/ArrayOps$.foldLeft$extension (Ljava/lang/Object;Ljava/lang/Object;Lscala/Function2;)Ljava/lang/Object;
  // INVOKESTATIC scala/runtime/BoxesRunTime.unboxToLong (Ljava/lang/Object;)J
  arraySeq.foldLeft(0L) { case (sum, data) =>
    sum + data
  }
}
```

```scala
@Benchmark
def totalFoldLeftArrSeq(): Long = {
  arraySeq.foldLeft(0L) {
    // L0
    //   NEW scala/Tuple2$mcJJ$sp
    //   DUP
    //   LLOAD 0
    //   LLOAD 2
    //   INVOKESPECIAL scala/Tuple2$mcJJ$sp.<init> (JJ)V
    //   ASTORE 6
    //   ALOAD 6
    //   INVOKEVIRTUAL scala/Tuple2._1$mcJ$sp ()J
    //   LSTORE 7
    // L2
    //   ALOAD 6
    //   INVOKEVIRTUAL scala/Tuple2._2$mcJ$sp ()J
    //   LSTORE 9  
    case (sum, data) =>
      // L3
      //   LLOAD 7
      //   LLOAD 9
      //   LADD
      //   LRETURN
      sum + data
  }
}
```

``` 
public totalFoldLeftArrSeq()J
   L0
    LINENUMBER 149 L0
    ALOAD 0
    INVOKEVIRTUAL vs/core/BoxingJmh.arraySeq ()Lscala/collection/immutable/ArraySeq;
    LCONST_0
    INVOKESTATIC scala/runtime/BoxesRunTime.boxToLong (J)Ljava/lang/Long;
    INVOKEDYNAMIC apply$mcJJJ$sp()Lscala/runtime/java8/JFunction2$mcJJJ$sp; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/LambdaMetafactory.altMetafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
      // arguments:
      (JJ)J, 
      // handle kind 0x6 : INVOKESTATIC
      vs/core/BoxingJmh.$anonfun$totalFoldLeftArrSeq$1(JJ)J, 
      (JJ)J, 
      1
    ]
    INVOKEVIRTUAL scala/collection/immutable/ArraySeq.foldLeft (Ljava/lang/Object;Lscala/Function2;)Ljava/lang/Object;
    INVOKESTATIC scala/runtime/BoxesRunTime.unboxToLong (Ljava/lang/Object;)J
    LRETURN
   L1
    LOCALVARIABLE this Lvs/core/BoxingJmh; L0 L1 0
    MAXSTACK = 3
    MAXLOCALS = 1
    
    
public final static synthetic $anonfun$totalFoldLeftArrSeq$1(JJ)J
   L0
    LINENUMBER 149 L0
    NEW scala/Tuple2$mcJJ$sp
    DUP
    LLOAD 0
    LLOAD 2
    INVOKESPECIAL scala/Tuple2$mcJJ$sp.<init> (JJ)V
    ASTORE 6
    ALOAD 6
    IFNULL L1
    ALOAD 6
    INVOKEVIRTUAL scala/Tuple2._1$mcJ$sp ()J
    LSTORE 7
   L2
    ALOAD 6
    INVOKEVIRTUAL scala/Tuple2._2$mcJ$sp ()J
    LSTORE 9
   L3
    LINENUMBER 150 L3
    LLOAD 7
    LLOAD 9
    LADD
    LRETURN
   L1
    LINENUMBER 149 L1
   FRAME APPEND [T T scala/Tuple2$mcJJ$sp]
    GOTO L4
   L4
   FRAME SAME
    NEW scala/MatchError
    DUP
    ALOAD 6
    INVOKESPECIAL scala/MatchError.<init> (Ljava/lang/Object;)V
    ATHROW
   L5
    LOCALVARIABLE sum J L2 L1 7
    LOCALVARIABLE data J L3 L1 9
    LOCALVARIABLE x0$1 J L0 L5 0
    LOCALVARIABLE x1$1 J L0 L5 2
    MAXSTACK = 6
    MAXLOCALS = 11    
```

## Пробуем создать свой foldLeft

Сначала проверим, насколько итерация по Range тяжела по сравнению с итерацией с помощью `var i & while`:

```scala
def iterateOverRange(): Long = {
  var total = 0L
  arraySeq
    .indices
    .foreach { i =>
      total += arraySeq(i)
    }
  total
}
```

``` 
[info] Benchmark                      Mode  Cnt  Score   Error  Units
[info] BoxingJmh.iterateOverArraySeq  avgt    5  3.429 ± 0.805  us/op
[info] BoxingJmh.iterateOverRange     avgt    5  3.276 ± 0.432  us/op
```

Мы видим, что скорость выполнения методов практически не отличается. Сделаем собственную версию foreach,
воспользовавшись этим знанием:

```scala
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
```

Измерим производительность, сравнив с версиями `iterateOverArraySeq` и `totalFoldLeftArrSeq`:

``` 
[info] Benchmark                      Mode  Cnt   Score   Error  Units
[info] BoxingJmh.iterateOverArraySeq  avgt    5   3.262 ± 0.410  us/op
[info] BoxingJmh.totalFoldLeftArrSeq  avgt    5  41.215 ± 6.781  us/op
[info] BoxingJmh.totalFoldLeftS       avgt    5   3.324 ± 0.750  us/op
```

Мы видим, что наш foldLeftS работает с ожидаемой высокой производительностью.

Проверим работу итератора:

```scala
def iteratorOverArraySeq(): Long = {
  var total = 0L
  arraySeq.iterator.foreach(l => total += l)
  total
}
```

``` 
[info] Benchmark                       Mode  Cnt  Score   Error  Units
[info] BoxingJmh.iterateOverArraySeq   avgt    5  3.316 ± 0.610  us/op
[info] BoxingJmh.iteratorOverArraySeq  avgt    5  3.311 ± 0.414  us/op
```

Видим, что скорость итератора нас устраивает, перепишем наш foldLeft на итераторе:

```scala
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
```

Результаты тестирования:

``` 
[info] Benchmark                      Mode  Cnt  Score   Error  Units
[info] BoxingJmh.iterateOverArraySeq  avgt    5  3.261 ± 0.396  us/op
[info] BoxingJmh.totalFoldLeftI       avgt    5  3.145 ± 0.043  us/op
[info] BoxingJmh.totalFoldLeftS       avgt    5  3.139 ± 0.057  us/op
```

```
public foldLeftI(Lscala/collection/immutable/ArraySeq;Ljava/lang/Object;Lscala/Function2;)Ljava/lang/Object;
. . .
// boolean
public foldLeftI$mZc$sp(Lscala/collection/immutable/ArraySeq;ZLscala/Function2;)Z
. . .
// byte
public foldLeftI$mBc$sp(Lscala/collection/immutable/ArraySeq;BLscala/Function2;)B
. . .
// char
public foldLeftI$mCc$sp(Lscala/collection/immutable/ArraySeq;CLscala/Function2;)C
. . .
// double
public foldLeftI$mDc$sp(Lscala/collection/immutable/ArraySeq;DLscala/Function2;)D
. . .
// float
public foldLeftI$mFc$sp(Lscala/collection/immutable/ArraySeq;FLscala/Function2;)F
. . .
// int
public foldLeftI$mIc$sp(Lscala/collection/immutable/ArraySeq;ILscala/Function2;)I
. . . 
// long
public foldLeftI$mJc$sp(Lscala/collection/immutable/ArraySeq;JLscala/Function2;)J
. . .
// short
public foldLeftI$mSc$sp(Lscala/collection/immutable/ArraySeq;SLscala/Function2;)S
. . .
// void
public foldLeftI$mVc$sp(Lscala/collection/immutable/ArraySeq;Lscala/runtime/BoxedUnit;Lscala/Function2;)V 
```

```scala
def totalFoldLeftI(): Long = {
  // ...
  // INVOKEVIRTUAL vs/core/BoxingJmh.foldLeftI$mJc$sp (Lscala/collection/immutable/ArraySeq;JLscala/Function2;)J
  // ...
  foldLeftI[Long](arraySeq)(0L)((a, b) => a + b)
}
```

### Почему foldLeft не специализирован?

```scala
sealed abstract class ArraySeq[+A]
  extends AbstractSeq[A]
    with IndexedSeq[A]
    with IndexedSeqOps[A, ArraySeq, ArraySeq[A]]
    with StrictOptimizedSeqOps[A, ArraySeq, ArraySeq[A]]
    with EvidenceIterableFactoryDefaults[A, ArraySeq, ClassTag]
    with Serializable {

  def foldLeft[B](z: B)(f: (B, A) => B): B
}
```

## А как быть с более сложными типами?

### AnyVal

```scala
sealed trait DomainType[@specialized T] extends Any {
  def carrier: T
}

object DomainType {
  case class Int8(carrier: Byte) extends AnyVal with DomainType[Byte]

  case class Int16(carrier: Short) extends AnyVal with DomainType[Short]

  case class Int32(carrier: Int) extends AnyVal with DomainType[Int]

  case class Int64(carrier: Long) extends AnyVal with DomainType[Long]
}
```

Попробуем просуммировать `ArraySeq[DomainType.Int64]`

```scala
@Benchmark
def totalDomain64A(): DomainType.Int64 = {
  var total: DomainType.Int64 = DomainType.Int64(0L)
  domainArray
    .indices
    .foreach(i =>
      total = DomainType.Int64(domainArray(i).carrier + total.carrier),
    )
  total
}

@Benchmark
def totalDomain32A(): DomainType.Int32 = {
  var total: DomainType.Int32 = DomainType.Int32(0)
  domainIntArray
    .indices
    .foreach(i =>
      total = DomainType.Int32(domainIntArray(i).carrier + total.carrier),
    )
  total
}

@Benchmark
def totalDomain64B(): DomainType.Int64 = {
  var total: DomainType.Int64 = DomainType.Int64(0L)
  domainSeq
    .indices
    .foreach(i =>
      total = DomainType.Int64(total.carrier + domainSeq(i).carrier),
    )
  total
}

@Benchmark
def totalDomain32B(): DomainType.Int32 = {
  var total: DomainType.Int32 = DomainType.Int32(0)
  domainIntSeq
    .indices
    .foreach(i =>
      total = DomainType.Int32(total.carrier + domainIntSeq(i).carrier),
    )
  total
}
```

Пройдемся бенчмарками:

``` 
[info] Benchmark                         Mode  Cnt  Score   Error  Units
[info] BoxingJmh.iterateOverArraySeq     avgt    5  3.242 ± 0.249  us/op
[info] BoxingJmh.iterateOverIntArraySeq  avgt    5  3.288 ± 0.320  us/op
[info] BoxingJmh.totalDomain32A          avgt    5  4.780 ± 0.166  us/op
[info] BoxingJmh.totalDomain32B          avgt    5  6.573 ± 1.537  us/op
[info] BoxingJmh.totalDomain64A          avgt    5  5.115 ± 0.554  us/op
[info] BoxingJmh.totalDomain64B          avgt    5  6.164 ± 0.292  us/op
```

Из странного:

```scala
//    LINENUMBER 235 L0
//    LCONST_0
//    INVOKESTATIC scala/runtime/LongRef.create (J)Lscala/runtime/LongRef;
//    ASTORE 1
var total: DomainType.Int64 = DomainType.Int64(0L)

//     LINENUMBER 246 L0
//     ICONST_0
//     INVOKESTATIC scala/runtime/IntRef.create (I)Lscala/runtime/IntRef;
//     ASTORE 1
var total: DomainType.Int32 = DomainType.Int32(0)
```

