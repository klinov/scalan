package scalan

import scalan.collections.CollectionsDslExp

class ConverterTests extends BaseCtxTests {
  class ConvProgStaged extends TestContext with CollectionsDslExp

  def testConverter[A,B](ctx: ConvProgStaged, name: String, shouldConvert: Boolean = true)(implicit eA: ctx.Elem[A], eB: ctx.Elem[B]) = {
    import ctx._
    val conv = hasConverter(eA,eB)
    if (shouldConvert) {
      assert(conv.isDefined, s"no converter $eA --> $eB")
      ctx.emit(name, conv.get)
    } else {
      val fileName = "unexpected_" + name
      if (conv.isDefined)
        ctx.emit(fileName, conv.get)
      assert(!conv.isDefined, s"unexpected converter $eA --> $eB, see $fileName")
    }
  }

  test("hasConverter") {
    val ctx = new ConvProgStaged
    import ctx._
    testConverter[Array[Double], CollectionOverArray[Double]](ctx, "convArrToCollectionOA")
    testConverter[CollectionOverArray[Double], Array[Double]](ctx, "convCollectionOAtoArr")
  }
}
