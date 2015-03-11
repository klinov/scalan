package scalan.compilation.lms.cxx.sharedptr

import scala.virtualization.lms.common._
import scala.virtualization.lms.epfl.test7.{ArrayLoopsExp, ArrayLoopsFatExp}
import scalan.compilation.lms.common.JNILmsOpsExp

trait CxxShptrGenFatArrayLoopsFusionOpt extends CxxShptrGenArrayLoopsFat with LoopFusionOpt {
  val IR: ArrayLoopsFatExp with IfThenElseFatExp with JNILmsOpsExp
  import IR._

  override def unapplySimpleIndex(e: Def[Any]) = e match {
    case ArrayIndex(a, i) => Some((a,i))
    case _ => super.unapplySimpleIndex(e)
  }
  override def unapplySimpleDomain(e: Def[Int]): Option[Exp[Any]] = e match {
    case ArrayLength(a) => Some(a)
    case _ => super.unapplySimpleDomain(e)
  }

  override def unapplySimpleCollect(e: Def[Any]) = e match {
    case ArrayElem(Block(a)) => Some(a) //TODO: block??
    case JNIArrayElem(x,Block(a)) => Some(a)
    case _ => super.unapplySimpleCollect(e)
  }

  override def unapplySimpleCollectIf(e: Def[Any]) = e match {
    case ArrayIfElem(c,Block(a)) => Some((a,List(c))) //TODO: block?
    case _ => super.unapplySimpleCollectIf(e)
  }

  override def applyAddCondition(e: Def[Any], c: List[Exp[Boolean]]) = e match { //TODO: should c be list or not?
    case ArrayElem(a) if c.length == 1 => ArrayIfElem(c(0),a)
    case JNIArrayElem(x,a) if c.length == 1 => JNIArrayIfElem(x,c(0),a)
    case ReduceElem(a) if c.length == 1 => ReduceIfElem(c(0),a)
    case _ => super.applyAddCondition(e,c)
  }
}

trait CxxShptrGenArrayLoops extends CLikeGenLoops with CxxShptrCodegen {
  val IR: ArrayLoopsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case SimpleLoop(s,x,ArrayElem(y)) =>
      stream.println("val " + quote(sym) + " = LoopArray("+quote(s)+") { " + quote(x) + " => ")
      emitBlock(y)
      stream.println(quote(getBlockResult(y)))
      stream.println("}")
    case SimpleLoop(s,x,ReduceElem(y)) =>
      stream.println("val " + quote(sym) + " = LoopReduce("+quote(s)+") { " + quote(x) + " => ")
      emitBlock(y)
      stream.println(quote(getBlockResult(y)))
      stream.println("}")
    // TODO: conditional variants ...
    case SimpleLoop(s,x,FlattenElem(y)) =>
      stream.println("val " + quote(sym) + " = LoopFlatten("+quote(s)+") { " + quote(x) + " => ")
      emitBlock(y)
      stream.println(quote(getBlockResult(y)))
      stream.println("}")
    case ArrayIndex(a,i) =>
      emitValDef(sym, src"(*$a)[$i]")
    case ArrayLength(a) =>
      emitValDef(quote(sym), manifest[size_t], quote(a) + "->size()")
    case _ => super.emitNode(sym, rhs)
  }
}

trait CxxShptrGenArrayLoopsFat extends CxxShptrGenArrayLoops with CLikeGenLoopsFat {
  val IR: ArrayLoopsFatExp with JNILmsOpsExp
  import IR._

  override def emitFatNode(sym: List[Sym[Any]], rhs1: FatDef) = rhs1 match {
    case SimpleFatLoop(s,x,rhs) =>
      stream.println(s"/*start: ${rhs1.toString}*/")
      for ((l,r) <- sym zip rhs) {
        r match {
          case JArrayElem(x,y) =>
          case JNIArrayElem(x,y) =>
          case ArrayElem(y) =>
            emitConstruct(l, quote(s))
          case ReduceElem(y) =>
            emitConstruct(l)
            //stream.println(s"${remap(l.tp)} ${quote(l)} = ${remap(getBlockResult(y).tp)}();")
          case ArrayIfElem(c,y) =>
            emitVarDecl(l)
          case ReduceIfElem(c,y) =>
            emitConstruct(l)
            //stream.println(s"${remap(l.tp)} ${quote(l)} = ${remap(getBlockResult(y).tp)}();")
//          case FlattenElem(y) =>
//            stream.println("var " + quote(l) + " = new ArrayBuilder[" + remap(getBlockResult(y).tp) + "]")
        }
      }

      val ii = x // was: x(i)
      stream.println(s"for( ${remap(ii.tp)} ${quote(ii)} = 0; ${quote(ii)} < ${quote(s)}; ++${quote(ii)} ) {")

      emitFatBlock(syms(rhs).map(Block(_))) // TODO: check this
      for ((l,r) <- sym zip rhs) {
        r match {
          case JArrayElem(x,y) =>
            val q = getBlockResult(y)
            stream.println( s"env->SetObjectArrayElement( ${quote(x)}, ${quote(ii)}, ${quote(q)}); /*JArrayElem*/" )
          case JNIArrayElem(x,y) =>
            val q = getBlockResult(y)
            stream.println(src"(*$x)[$ii] = $q; /*JNIArrayElem*/")
          case ArrayElem(y) =>
            stream.println(src"(*$l)[$ii] = ${quote(getBlockResult(y))};")
          case ReduceElem(y) =>
            stream.println(quote(l) + " += " + quote(getBlockResult(y)) + ";")
          case ArrayIfElem(c,y) =>
            stream.println("if ("+quote(/*getBlockResult*/(c))+") " + quote(l) + "->push_back( " + quote(getBlockResult(y)) + " );")
          case ReduceIfElem(c,y) =>
            stream.println("if ("+quote(/*getBlockResult*/(c))+") " + quote(l) + " += " + quote(getBlockResult(y)) + ";")
//          case FlattenElem(y) =>
//            stream.println(quote(l) + " ++= " + quote(getBlockResult(y)))
        }
      }
      stream.println("}")
      stream.println(s"/*end: ${rhs1.toString}*/")
    case _ => super.emitFatNode(sym, rhs1)
  }
}
