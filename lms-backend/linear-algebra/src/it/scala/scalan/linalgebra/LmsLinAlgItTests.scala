package scalan.linalgebra

import scalan._
import scalan.compilation.lms.linalgebra.{LinAlgLmsCompilerUni, LinAlgLmsCompilerScala}
import scalan.compilation.lms.scalac.LmsCompilerScalaConfig
import scalan.compilation.lms.source2bin.SbtConfig
import scalan.compilation.lms.uni._
import scalan.it.BaseItTests

abstract class LmsLinAlgItTests extends BaseItTests[LinearAlgebraExamples](new MatricesDslSeq with LinearAlgebraExamples) {
  class ProgExp extends MatricesDslExp with JNIExtractorOpsExp with LinearAlgebraExamples

  val progStaged = new LinAlgLmsCompilerScala(new ProgExp)

  val progStagedU = new LinAlgLmsCompilerUni(new ProgExp)

  val compilerConfigU = LmsCompilerScalaConfig().withSbtConfig(SbtConfig(scalaVersion = "2.11.2"))
  
  def sparseVectorData(arr: Array[Double]) = (arr.indices.toArray, (arr, arr.length))

  val defaultCompilers = compilers(progStaged, cwc(progStagedU)(compilerConfigU))
  val progStagedOnly = compilers(progStaged)
}

class LmsMvmItTests extends LmsLinAlgItTests {
  import progSeq._

  test("ddmvm") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.ddmvm)(Pair(inM, inV))
  }

  test("ddmvmList") {
    val inM = List(Array(1.0, 1.0), Array(0.0, 1.0))
    val inV = Array(2.0, 3.0)
    // TODO List support in LmsCompilerUni
    compareOutputWithSequential(_.ddmvmList, progStagedOnly)(Pair(inM, inV))
  }

  test("dsmvm") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.dsmvm)(Pair(inM, inV))
  }

  test("sdmvm") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0)).map(sparseVectorData)
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.sdmvm)(Pair(inM, inV))
  }

  test("ssmvm") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0)).map(sparseVectorData)
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.ssmvm)(Pair(inM, inV))
  }

  test("fdmvm") {
    val inM = (Array(1.0, 1.0, 0.0, 1.0), 2)
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.fdmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("fsmvm") {
    val inM = (Array(1.0, 1.0, 0.0, 1.0), 2)
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.fsmvm)(Pair(inM, inV))
  }

  test("cdmvm") {
    val inM = (3.0, (2, 2))
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.cdmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("csmvm") {
    val inM = (3.0, (2, 2))
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.csmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("dcmvm") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    val inV = (2.0, 2)
    compareOutputWithSequential(_.dcmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("ccmvm") {
    val inM = (3.0, (2, 2))
    val inV = (2.0, 2)
    compareOutputWithSequential(_.ccmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("dgdmvm") {
    val inM = Array(1.0, 2.0, 3.0)
    val inV = Array(2.0, 4.0, 6.0)
    compareOutputWithSequential(_.dgdmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("dgsmvm") {
    val inM = Array(1.0, 2.0, 3.0)
    val inV = sparseVectorData(Array(2.0, 4.0, 6.0))
    compareOutputWithSequential(_.dgsmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("dgcmvm") {
    val inM = Array(1.0, 2.0, 3.0)
    val inV = (2.0, 3)
    compareOutputWithSequential(_.dgcmvm, progStagedOnly)(Pair(inM, inV))
  }

  test("ddmvm0") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.ddmvm0)(Pair(inM, inV))
  }

  test("dsmvm0") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.dsmvm0)(Pair(inM, inV))
  }

  test("sdmvm0") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0)).map(sparseVectorData)
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.sdmvm0)(Pair(inM, inV))
  }

  test("ssmvm0") {
    val inM = Array(Array(1.0, 1.0), Array(0.0, 1.0)).map(sparseVectorData)
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.ssmvm0)(Pair(inM, inV))
  }

  test("fdmvm0") {
    val inM = (Array(1.0, 1.0, 0.0, 1.0), 2)
    val inV = Array(2.0, 3.0)
    compareOutputWithSequential(_.fdmvm0)(Pair(inM, inV))
  }

  test("fsmvm0") {
    val inM = (Array(1.0, 1.0, 0.0, 1.0), 2)
    val inV = sparseVectorData(Array(2.0, 3.0))
    compareOutputWithSequential(_.fsmvm0)(Pair(inM, inV))
  }
}

class LmsMmmItTests extends LmsLinAlgItTests {
  import progSeq._

  test("ddmmm") {
    val inM1 = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    val inM2 = Array(Array(1.0, 1.0), Array(0.0, 1.0))
    compareOutputWithSequential(_.ddmmm)(Pair(inM1, inM2))
  }

  test("ssmmm") {
    //pending
    val inM1 = Array(Array(1.0, 1.0), Array(0.0, 1.0)).map(sparseVectorData)
    val inM2 = Array(Array(1.0, 1.0), Array(0.0, 1.0)).map(sparseVectorData)
    compareOutputWithSequential(_.ssmmm)(Pair(inM1, inM2))
  }

  test("ffmmm") {
    val inM1 = (Array(1.0, 1.0, 0.0, 1.0), 2)
    val inM2 = (Array(1.0, 1.0, 0.0, 1.0), 2)
    compareOutputWithSequential(_.ffmmm)(Pair(inM1, inM2))
  }

  test("ccmmm") {
    val inM1 = (3.0, (2, 2))
    val inM2 = (2.0, (2, 2))
    compareOutputWithSequential(_.ccmmm, progStagedOnly)(Pair(inM1, inM2))
  }

  test("cfmmm") {
    val inM1 = (3.0, (2, 2))
    val inM2 = (Array(1.0, 1.0, 0.0, 1.0), 2)
    compareOutputWithSequential(_.cfmmm, progStagedOnly)(Pair(inM1, inM2))
  }

  test("dgfmmm") {
    val inM1 = Array(2.0, 3.0)
    val inM2 = (Array(1.0, 1.0, 0.0, 1.0), 2)
    compareOutputWithSequential(_.dgfmmm, progStagedOnly)(Pair(inM1, inM2))
  }

  test("dgdgmmm") {
    val inM1 = Array(2.0, 3.0)
    val inM2 = Array(2.0, 3.0)
    compareOutputWithSequential(_.dgdgmmm, progStagedOnly)(Pair(inM1, inM2))
  }

}

class AbstractElemItTests extends LmsLinAlgItTests {
  import progSeq._

  lazy val jArrTrain2x2 = Array(Array((0, 5.0), (1, 3.0)), Array((1, 4.0)))
  lazy val jArrTest2x2 = Array(Array((0, 5.0), (1, 3.0)), Array((0, 3.0), (1, 4.0)))

  def getNArrayWithSegmentsFromJaggedArray(jaggedArray: Array[Array[(Int, Double)]]) = {
    val arr = jaggedArray.flatMap(v => v)
    val lens = jaggedArray.map(i => i.length)
    val offs = lens.scanLeft(0)((x, y) => x + y).take(lens.length)
    (arr, offs zip lens)
  }

  test("pattern matching vectors with abstract elem works") {
    val arrTrain = Array((0, 5.0), (1, 3.0), (1, 4.0))
    lazy val width = 5

    getStagedOutput(_.dotWithAbstractElem, progStagedOnly)(Tuple(width, arrTrain))
  }

  test("elems divergence in if_then_else branches") {
    // Different branches of if_then_else operator produce different elems.
    // This causes Sums and SumViews to appear.
    // The test verifies iso lifting in this case (see IfThenElse.rewriteDef)
    buildGraphs(_.funSimpleSum, progStagedOnly)
  }

}

class VectorMethodsItTests extends LmsLinAlgItTests {

  import progSeq._

  lazy val vector1 = Array(Pair(0, 1.0), Pair(1, 2.0), Pair(2, 3.0), Pair(3, 4.0), Pair(4, 5.0))
  lazy val vector2 = Array(Pair(0, 1.0), Pair(7, 3.0), Pair(12, 5.0))

  test("applySparseVector1") {
    val len = 5
    val i = 2
    compareOutputWithSequential(_.applySparseVector, progStagedOnly)(Tuple(vector1, len, i))
  }

  test("applySparseVector2") {
    val len = 12
    val i = 12
    compareOutputWithSequential(_.applySparseVector, progStagedOnly)(Tuple(vector2, len, i))
  }

  test("transpose") {
    val nItems = 2
    val (arrTrain, segsTrain) = progSeq.getNArrayWithSegmentsFromJaggedArray(progSeq.jArrTrain2x2)

    compareOutputWithSequential(_.transpose, progStagedOnly)(Tuple(arrTrain, segsTrain, nItems))
  }

  // the below two tests are ignored because they can fail due to randomness
  // we could also just decrease the chance of this significantly
  ignore("random") {
    compareOutputWithSequential(_.funRandom.asRep[Any => Any], progStagedOnly)(1.0)
  }

  ignore("randomArray") {
    compareOutputWithSequential(_.funRandomArray, progStagedOnly)(1000)
  }

  test("ZipMapViewBoth") {
    compareOutputWithSequential(_.funZipMapViewBoth, progStagedOnly)(10)
  }

  test("ZipMapViewLeft") {
    compareOutputWithSequential(_.funZipMapViewLeft, progStagedOnly)(10)
  }

  test("ZipMapViewRight") {
    compareOutputWithSequential(_.funZipMapViewRight, progStagedOnly)(10)
  }

  test("collReplicateFilter") {
    compareOutputWithSequential(_.collReplicateFilter, progStagedOnly)(Array(1, 2, 4))
  }

  test("sumConvertSum") {
    compareOutputWithSequential(_.sumConvertSum, progStagedOnly)(Array(1.0, 2.0, 4.0))
  }

  test("Ddvva") {
    compareOutputWithSequential(_.ddvva, progStagedOnly)(Array(1.0, 2.0, 4.0))
  }

  test("Dsvva") {
    // fails as it:test with List(4.0, 8.0, 16.0) did not equal List(2.0, 4.0, 8.0)
    // but works as run generated code
    compareOutputWithSequential(_.dsvva, progStagedOnly)(Array(1.0, 2.0, 4.0))
  }

  test("Sdvva") {
    // fails as it:test with List(4.0, 8.0, 16.0) did not equal List(2.0, 4.0, 8.0)
    // but works as run generated code
    compareOutputWithSequential(_.sdvva, progStagedOnly)(Array(1.0, 2.0, 4.0))
  }

  test("Ssvva") {
    // fails with scalan.Base$StagingException: LMS method arrayOuterJoin not found
    compareOutputWithSequential(_.ssvva, progStagedOnly)(Array(1.0, 2.0, 4.0))
  }
}
