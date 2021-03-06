package scalan.collections

import scalan._
import scalan.common.OverloadHack.Overloaded1
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait BitSetsAbs extends scalan.ScalanDsl with BitSets {
  self: CollectionsDsl with BitSetsDsl =>

  // single proxy for each type family
  implicit def proxyBitSet(p: Rep[BitSet]): BitSet = {
    proxyOps[BitSet](p)(scala.reflect.classTag[BitSet])
  }

  // familyElem
  class BitSetElem[To <: BitSet]
    extends EntityElem[To] {
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }
    override def isEntityType = true
    override lazy val tag = {
      weakTypeTag[BitSet].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[BitSet] => convertBitSet(x) }
      tryConvert(element[BitSet], this, x, conv)
    }

    def convertBitSet(x: Rep[BitSet]): Rep[To] = {
      x.selfType1 match {
        case _: BitSetElem[_] => x.asRep[To]
        case e => !!!(s"Expected $x to have BitSetElem[_], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def bitSetElement: Elem[BitSet] =
    cachedElem[BitSetElem[BitSet]]()

  implicit case object BitSetCompanionElem extends CompanionElem[BitSetCompanionAbs] {
    lazy val tag = weakTypeTag[BitSetCompanionAbs]
    protected def getDefaultRep = BitSet
  }

  abstract class BitSetCompanionAbs extends CompanionDef[BitSetCompanionAbs] with BitSetCompanion {
    def selfType = BitSetCompanionElem
    override def toString = "BitSet"
  }
  def BitSet: Rep[BitSetCompanionAbs]
  implicit def proxyBitSetCompanionAbs(p: Rep[BitSetCompanionAbs]): BitSetCompanionAbs =
    proxyOps[BitSetCompanionAbs](p)

  abstract class AbsBoolCollBitSet
      (bits: Rep[Collection[Boolean]])
    extends BoolCollBitSet(bits) with Def[BoolCollBitSet] {
    lazy val selfType = element[BoolCollBitSet]
  }
  // elem for concrete class
  class BoolCollBitSetElem(val iso: Iso[BoolCollBitSetData, BoolCollBitSet])
    extends BitSetElem[BoolCollBitSet]
    with ConcreteElem[BoolCollBitSetData, BoolCollBitSet] {
    override lazy val parent: Option[Elem[_]] = Some(bitSetElement)
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }

    override def convertBitSet(x: Rep[BitSet]) = BoolCollBitSet(x.bits)
    override def getDefaultRep = BoolCollBitSet(element[Collection[Boolean]].defaultRepValue)
    override lazy val tag = {
      weakTypeTag[BoolCollBitSet]
    }
  }

  // state representation type
  type BoolCollBitSetData = Collection[Boolean]

  // 3) Iso for concrete class
  class BoolCollBitSetIso
    extends EntityIso[BoolCollBitSetData, BoolCollBitSet] with Def[BoolCollBitSetIso] {
    override def from(p: Rep[BoolCollBitSet]) =
      p.bits
    override def to(p: Rep[Collection[Boolean]]) = {
      val bits = p
      BoolCollBitSet(bits)
    }
    lazy val eFrom = element[Collection[Boolean]]
    lazy val eTo = new BoolCollBitSetElem(self)
    lazy val selfType = new BoolCollBitSetIsoElem
    def productArity = 0
    def productElement(n: Int) = ???
  }
  case class BoolCollBitSetIsoElem() extends Elem[BoolCollBitSetIso] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new BoolCollBitSetIso())
    lazy val tag = {
      weakTypeTag[BoolCollBitSetIso]
    }
  }
  // 4) constructor and deconstructor
  class BoolCollBitSetCompanionAbs extends CompanionDef[BoolCollBitSetCompanionAbs] with BoolCollBitSetCompanion {
    def selfType = BoolCollBitSetCompanionElem
    override def toString = "BoolCollBitSet"

    @scalan.OverloadId("fromFields")
    def apply(bits: Rep[Collection[Boolean]]): Rep[BoolCollBitSet] =
      mkBoolCollBitSet(bits)

    def unapply(p: Rep[BitSet]) = unmkBoolCollBitSet(p)
  }
  lazy val BoolCollBitSetRep: Rep[BoolCollBitSetCompanionAbs] = new BoolCollBitSetCompanionAbs
  lazy val BoolCollBitSet: BoolCollBitSetCompanionAbs = proxyBoolCollBitSetCompanion(BoolCollBitSetRep)
  implicit def proxyBoolCollBitSetCompanion(p: Rep[BoolCollBitSetCompanionAbs]): BoolCollBitSetCompanionAbs = {
    proxyOps[BoolCollBitSetCompanionAbs](p)
  }

  implicit case object BoolCollBitSetCompanionElem extends CompanionElem[BoolCollBitSetCompanionAbs] {
    lazy val tag = weakTypeTag[BoolCollBitSetCompanionAbs]
    protected def getDefaultRep = BoolCollBitSet
  }

  implicit def proxyBoolCollBitSet(p: Rep[BoolCollBitSet]): BoolCollBitSet =
    proxyOps[BoolCollBitSet](p)

  implicit class ExtendedBoolCollBitSet(p: Rep[BoolCollBitSet]) {
    def toData: Rep[BoolCollBitSetData] = isoBoolCollBitSet.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoBoolCollBitSet: Iso[BoolCollBitSetData, BoolCollBitSet] =
    reifyObject(new BoolCollBitSetIso())

  // 6) smart constructor and deconstructor
  def mkBoolCollBitSet(bits: Rep[Collection[Boolean]]): Rep[BoolCollBitSet]
  def unmkBoolCollBitSet(p: Rep[BitSet]): Option[(Rep[Collection[Boolean]])]

  registerModule(BitSets_Module)
}

// Std -----------------------------------
trait BitSetsStd extends scalan.ScalanDslStd with BitSetsDsl {
  self: CollectionsDsl with BitSetsDslStd =>
  lazy val BitSet: Rep[BitSetCompanionAbs] = new BitSetCompanionAbs {
  }

  case class StdBoolCollBitSet
      (override val bits: Rep[Collection[Boolean]])
    extends AbsBoolCollBitSet(bits) {
  }

  def mkBoolCollBitSet
    (bits: Rep[Collection[Boolean]]): Rep[BoolCollBitSet] =
    new StdBoolCollBitSet(bits)
  def unmkBoolCollBitSet(p: Rep[BitSet]) = p match {
    case p: BoolCollBitSet @unchecked =>
      Some((p.bits))
    case _ => None
  }
}

// Exp -----------------------------------
trait BitSetsExp extends scalan.ScalanDslExp with BitSetsDsl {
  self: CollectionsDsl with BitSetsDslExp =>
  lazy val BitSet: Rep[BitSetCompanionAbs] = new BitSetCompanionAbs {
  }

  case class ExpBoolCollBitSet
      (override val bits: Rep[Collection[Boolean]])
    extends AbsBoolCollBitSet(bits)

  object BoolCollBitSetMethods {
  }

  object BoolCollBitSetCompanionMethods {
  }

  def mkBoolCollBitSet
    (bits: Rep[Collection[Boolean]]): Rep[BoolCollBitSet] =
    new ExpBoolCollBitSet(bits)
  def unmkBoolCollBitSet(p: Rep[BitSet]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: BoolCollBitSetElem @unchecked =>
      Some((p.asRep[BoolCollBitSet].bits))
    case _ =>
      None
  }

  object BitSetMethods {
    object bits {
      def unapply(d: Def[_]): Option[Rep[BitSet]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[BitSetElem[_]] && method.getName == "bits" =>
          Some(receiver).asInstanceOf[Option[Rep[BitSet]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[BitSet]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object union {
      def unapply(d: Def[_]): Option[(Rep[BitSet], Rep[BitSet])] = d match {
        case MethodCall(receiver, method, Seq(that, _*), _) if receiver.elem.isInstanceOf[BitSetElem[_]] && method.getName == "union" =>
          Some((receiver, that)).asInstanceOf[Option[(Rep[BitSet], Rep[BitSet])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[BitSet], Rep[BitSet])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object length {
      def unapply(d: Def[_]): Option[Rep[BitSet]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[BitSetElem[_]] && method.getName == "length" =>
          Some(receiver).asInstanceOf[Option[Rep[BitSet]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[BitSet]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object contains {
      def unapply(d: Def[_]): Option[(Rep[BitSet], Rep[Int])] = d match {
        case MethodCall(receiver, method, Seq(n, _*), _) if receiver.elem.isInstanceOf[BitSetElem[_]] && method.getName == "contains" =>
          Some((receiver, n)).asInstanceOf[Option[(Rep[BitSet], Rep[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[BitSet], Rep[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object add {
      def unapply(d: Def[_]): Option[(Rep[BitSet], Rep[Int])] = d match {
        case MethodCall(receiver, method, Seq(n, _*), _) if receiver.elem.isInstanceOf[BitSetElem[_]] && method.getName == "add" && method.getAnnotation(classOf[scalan.OverloadId]) == null =>
          Some((receiver, n)).asInstanceOf[Option[(Rep[BitSet], Rep[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[BitSet], Rep[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object add_many {
      def unapply(d: Def[_]): Option[(Rep[BitSet], Coll[Int])] = d match {
        case MethodCall(receiver, method, Seq(ns, _*), _) if receiver.elem.isInstanceOf[BitSetElem[_]] && method.getName == "add" && { val ann = method.getAnnotation(classOf[scalan.OverloadId]); ann != null && ann.value == "many" } =>
          Some((receiver, ns)).asInstanceOf[Option[(Rep[BitSet], Coll[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[BitSet], Coll[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object BitSetCompanionMethods {
    object apply {
      def unapply(d: Def[_]): Option[Coll[Boolean]] = d match {
        case MethodCall(receiver, method, Seq(flags, _*), _) if receiver.elem == BitSetCompanionElem && method.getName == "apply" && method.getAnnotation(classOf[scalan.OverloadId]) == null =>
          Some(flags).asInstanceOf[Option[Coll[Boolean]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Coll[Boolean]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object apply_many {
      def unapply(d: Def[_]): Option[Rep[Array[Boolean]]] = d match {
        case MethodCall(receiver, method, Seq(flags, _*), _) if receiver.elem == BitSetCompanionElem && method.getName == "apply" && { val ann = method.getAnnotation(classOf[scalan.OverloadId]); ann != null && ann.value == "many" } =>
          Some(flags).asInstanceOf[Option[Rep[Array[Boolean]]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Array[Boolean]]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object empty {
      def unapply(d: Def[_]): Option[Rep[Int]] = d match {
        case MethodCall(receiver, method, Seq(range, _*), _) if receiver.elem == BitSetCompanionElem && method.getName == "empty" =>
          Some(range).asInstanceOf[Option[Rep[Int]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Int]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object BitSets_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVVPYwTRxR+tu/OZ/t0B4dQxDUhF4cIBPYJCRHpiugwJork3J1YQMgg0Nx6bAZmZzY7cyc7BSVF6FBaCiSkNDQRRYpEaaJIUYpUURQpFQUVASEKqIh4Mzu7tk+3/BS4WO3MvnnvfT9vfO8xTKoIDiifcCJqAdWk5tn3FaWrXlNopgdfyc4mpydp98H3n91fLPz4cx7m2jB1haiTirehFL80+2H67ulOC0pE+FRpGSkNH7VshbovOae+ZlLUWRBsarLBab3FlF5uwcSG7Ay+huuQa8EuXwo/opp6DU6UosrtT1PTEUvXJbserIXDGqJuUNRHUJyJCNPYPtbYFcefpqE3EFIMAg2zrrW10LSFMUUWhDLSSYkiprsiO8lyQhDcgPnWVbJF6liiV/d0xEQPT1ZC4l8jPbqKISZ8AhtWlHfPDEK7LrSgrHQHCfoyCLnd6YcAgAoctU3UhvzUUn5qhp+qRyNGOPuGmI/rkewPIP7lCgD9EFMcfkOKJANtik7124v+hRdeJcibw33TStEinMJEH2a4wUqBPP5++pZ69sWd43kot6HM1MqG0hHx9ajkjq0KEUJq23NKIIl6qNZillq2ygrGbLNEyZdBSARmclTOoE6c+UybYLM349TJoL6oQ5qE5vphLsW7PwOv9U2DcL7+aN+RT/5rns9DfrxECVN6aPwoSaph6gRDprVl1DxKjtzsMingTx896fy2BBfzKU0u69spgykm1T9/V/46+HkeptvWx6c46bWRKdXkNFiLGlLoNkzLLRrFX4pbhJu3HZUqdmiXbHLt+BsFXkDgGvZnTlxIDSvL1t25hIBKbNBVKWj11Hr1uffHd/eM/yKYib/EI/g/O/7y39muttbU6AGmlW1pTkMBJ9ex4XbKjdTvKU0fZwka0vWIBXiBbNFjv/509ukvq5NW03mH9BzhmzQeZwd0CNr0MtklXCHw4gkpOSViKPDo02Atx4g8GdDdi8/YpTs3tZU11x+/O9Y2rmLzy/bcvtconNxhP9y4sffp3ct77OxNIzUBCatL7zB5yaC8x8mC1PzxnTI/XJvHAhL5gSHQKBdPS2O0gYXtJ/GOHg8fRsWkjzjiIIzbo4Ccje/sPKCl/o792vUBl/nNsOYy4YzfDAvbqi+NbxaNw2wwXpTzzhHDe125riJYzHCL5/RB8Ndf3F499Of9h9bnZaM0jp9I/91G/T3O0+zIYOE/1hC3GTrXXbIfI8FZNcawWF4BlsPzSFkIAAA="
}
}

trait BitSetsDsl extends impl.BitSetsAbs {self: CollectionsDsl with BitSetsDsl =>}
trait BitSetsDslStd extends impl.BitSetsStd {self: CollectionsDsl with BitSetsDslStd =>}
trait BitSetsDslExp extends impl.BitSetsExp {self: CollectionsDsl with BitSetsDslExp =>}
