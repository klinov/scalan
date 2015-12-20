package scalan.stream

import scalan._
import scala.reflect.runtime.universe._
import scalan.monads.{MonadsDsl, Monads}
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait ProcessesAbs extends scalan.ScalanDsl with Processes {
  self: ProcessesDsl =>

  // single proxy for each type family
  implicit def proxyProcess[F[_], O](p: Rep[Process[F, O]]): Process[F, O] = {
    proxyOps[Process[F, O]](p)(scala.reflect.classTag[Process[F, O]])
  }

  // familyElem
  class ProcessElem[F[_], O, To <: Process[F, O]](implicit _cF: Cont[F], _eO: Elem[O])
    extends EntityElem[To] {
    def cF = _cF
    def eO = _eO
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "O" -> Left(eO))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[Process[F, O]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Process[F, O]] => convertProcess(x) }
      tryConvert(element[Process[F, O]], this, x, conv)
    }

    def convertProcess(x: Rep[Process[F, O]]): Rep[To] = {
      x.selfType1.asInstanceOf[Elem[_]] match {
        case _: ProcessElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have ProcessElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def processElement[F[_], O](implicit cF: Cont[F], eO: Elem[O]): Elem[Process[F, O]] =
    cachedElem[ProcessElem[F, O, Process[F, O]]](cF, eO)

  implicit case object ProcessCompanionElem extends CompanionElem[ProcessCompanionAbs] {
    lazy val tag = weakTypeTag[ProcessCompanionAbs]
    protected def getDefaultRep = Process
  }

  abstract class ProcessCompanionAbs extends CompanionDef[ProcessCompanionAbs] with ProcessCompanion {
    def selfType = ProcessCompanionElem
    override def toString = "Process"
  }
  def Process: Rep[ProcessCompanionAbs]
  implicit def proxyProcessCompanionAbs(p: Rep[ProcessCompanionAbs]): ProcessCompanionAbs =
    proxyOps[ProcessCompanionAbs](p)

  abstract class AbsAwait[F[_], A, O]
      (req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends Await[F, A, O](req, recv) with Def[Await[F, A, O]] {
    lazy val selfType = element[Await[F, A, O]]
  }
  // elem for concrete class
  class AwaitElem[F[_], A, O](val iso: Iso[AwaitData[F, A, O], Await[F, A, O]])(implicit  val eA: Elem[A], override  val eO: Elem[O], override  val cF: Cont[F])
    extends ProcessElem[F, O, Await[F, A, O]]
    with ConcreteElem[AwaitData[F, A, O], Await[F, A, O]] {
    override lazy val parent: Option[Elem[_]] = Some(processElement(container[F], element[O]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "A" -> Left(eA), "O" -> Left(eO))
    }

    override def convertProcess(x: Rep[Process[F, O]]) = // Converter is not generated by meta
!!!("Cannot convert from Process to Await: missing fields List(req, recv)")
    override def getDefaultRep = Await(cF.lift(element[A]).defaultRepValue, constFun[$bar[Throwable, A], Process[F, O]](element[Process[F, O]].defaultRepValue))
    override lazy val tag = {
      implicit val tagA = eA.tag
      implicit val tagO = eO.tag
      weakTypeTag[Await[F, A, O]]
    }
  }

  // state representation type
  type AwaitData[F[_], A, O] = (F[A], $bar[Throwable, A] => Process[F, O])

  // 3) Iso for concrete class
  class AwaitIso[F[_], A, O](implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends EntityIso[AwaitData[F, A, O], Await[F, A, O]] with Def[AwaitIso[F, A, O]] {
    override def from(p: Rep[Await[F, A, O]]) =
      (p.req, p.recv)
    override def to(p: Rep[(F[A], $bar[Throwable, A] => Process[F, O])]) = {
      val Pair(req, recv) = p
      Await(req, recv)
    }
    lazy val eFrom = pairElement(element[F[A]], element[$bar[Throwable, A] => Process[F, O]])
    lazy val eTo = new AwaitElem[F, A, O](self)
    lazy val selfType = new AwaitIsoElem[F, A, O](eA, eO, cF)
    def productArity = 3
    def productElement(n: Int) = (eA, eO, cF).productElement(n)
  }
  case class AwaitIsoElem[F[_], A, O](eA: Elem[A], eO: Elem[O], cF: Cont[F]) extends Elem[AwaitIso[F, A, O]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new AwaitIso[F, A, O]()(eA, eO, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      implicit val tagO = eO.tag
      weakTypeTag[AwaitIso[F, A, O]]
    }
  }
  // 4) constructor and deconstructor
  class AwaitCompanionAbs extends CompanionDef[AwaitCompanionAbs] with AwaitCompanion {
    def selfType = AwaitCompanionElem
    override def toString = "Await"
    def apply[F[_], A, O](p: Rep[AwaitData[F, A, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
      isoAwait(eA, eO, cF).to(p)
    def apply[F[_], A, O](req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
      mkAwait(req, recv)
  }
  object AwaitMatcher {
    def unapply[F[_], A, O](p: Rep[Process[F, O]]) = unmkAwait(p)
  }
  lazy val Await: Rep[AwaitCompanionAbs] = new AwaitCompanionAbs
  implicit def proxyAwaitCompanion(p: Rep[AwaitCompanionAbs]): AwaitCompanionAbs = {
    proxyOps[AwaitCompanionAbs](p)
  }

  implicit case object AwaitCompanionElem extends CompanionElem[AwaitCompanionAbs] {
    lazy val tag = weakTypeTag[AwaitCompanionAbs]
    protected def getDefaultRep = Await
  }

  implicit def proxyAwait[F[_], A, O](p: Rep[Await[F, A, O]]): Await[F, A, O] =
    proxyOps[Await[F, A, O]](p)

  implicit class ExtendedAwait[F[_], A, O](p: Rep[Await[F, A, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]) {
    def toData: Rep[AwaitData[F, A, O]] = isoAwait(eA, eO, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoAwait[F[_], A, O](implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Iso[AwaitData[F, A, O], Await[F, A, O]] =
    reifyObject(new AwaitIso[F, A, O]()(eA, eO, cF))

  // 6) smart constructor and deconstructor
  def mkAwait[F[_], A, O](req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]]
  def unmkAwait[F[_], A, O](p: Rep[Process[F, O]]): Option[(Rep[F[A]], Rep[$bar[Throwable, A] => Process[F, O]])]

  abstract class AbsEmit[F[_], O]
      (head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends Emit[F, O](head, tail) with Def[Emit[F, O]] {
    lazy val selfType = element[Emit[F, O]]
  }
  // elem for concrete class
  class EmitElem[F[_], O](val iso: Iso[EmitData[F, O], Emit[F, O]])(implicit override  val eO: Elem[O], override  val cF: Cont[F])
    extends ProcessElem[F, O, Emit[F, O]]
    with ConcreteElem[EmitData[F, O], Emit[F, O]] {
    override lazy val parent: Option[Elem[_]] = Some(processElement(container[F], element[O]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "O" -> Left(eO))
    }

    override def convertProcess(x: Rep[Process[F, O]]) = // Converter is not generated by meta
!!!("Cannot convert from Process to Emit: missing fields List(head, tail)")
    override def getDefaultRep = Emit(element[O].defaultRepValue, element[Process[F, O]].defaultRepValue)
    override lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[Emit[F, O]]
    }
  }

  // state representation type
  type EmitData[F[_], O] = (O, Process[F, O])

  // 3) Iso for concrete class
  class EmitIso[F[_], O](implicit eO: Elem[O], cF: Cont[F])
    extends EntityIso[EmitData[F, O], Emit[F, O]] with Def[EmitIso[F, O]] {
    override def from(p: Rep[Emit[F, O]]) =
      (p.head, p.tail)
    override def to(p: Rep[(O, Process[F, O])]) = {
      val Pair(head, tail) = p
      Emit(head, tail)
    }
    lazy val eFrom = pairElement(element[O], element[Process[F, O]])
    lazy val eTo = new EmitElem[F, O](self)
    lazy val selfType = new EmitIsoElem[F, O](eO, cF)
    def productArity = 2
    def productElement(n: Int) = (eO, cF).productElement(n)
  }
  case class EmitIsoElem[F[_], O](eO: Elem[O], cF: Cont[F]) extends Elem[EmitIso[F, O]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new EmitIso[F, O]()(eO, cF))
    lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[EmitIso[F, O]]
    }
  }
  // 4) constructor and deconstructor
  class EmitCompanionAbs extends CompanionDef[EmitCompanionAbs] with EmitCompanion {
    def selfType = EmitCompanionElem
    override def toString = "Emit"
    def apply[F[_], O](p: Rep[EmitData[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
      isoEmit(eO, cF).to(p)
    def apply[F[_], O](head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
      mkEmit(head, tail)
  }
  object EmitMatcher {
    def unapply[F[_], O](p: Rep[Process[F, O]]) = unmkEmit(p)
  }
  lazy val Emit: Rep[EmitCompanionAbs] = new EmitCompanionAbs
  implicit def proxyEmitCompanion(p: Rep[EmitCompanionAbs]): EmitCompanionAbs = {
    proxyOps[EmitCompanionAbs](p)
  }

  implicit case object EmitCompanionElem extends CompanionElem[EmitCompanionAbs] {
    lazy val tag = weakTypeTag[EmitCompanionAbs]
    protected def getDefaultRep = Emit
  }

  implicit def proxyEmit[F[_], O](p: Rep[Emit[F, O]]): Emit[F, O] =
    proxyOps[Emit[F, O]](p)

  implicit class ExtendedEmit[F[_], O](p: Rep[Emit[F, O]])(implicit eO: Elem[O], cF: Cont[F]) {
    def toData: Rep[EmitData[F, O]] = isoEmit(eO, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoEmit[F[_], O](implicit eO: Elem[O], cF: Cont[F]): Iso[EmitData[F, O], Emit[F, O]] =
    reifyObject(new EmitIso[F, O]()(eO, cF))

  // 6) smart constructor and deconstructor
  def mkEmit[F[_], O](head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]]
  def unmkEmit[F[_], O](p: Rep[Process[F, O]]): Option[(Rep[O], Rep[Process[F, O]])]

  abstract class AbsHalt[F[_], O]
      (err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F])
    extends Halt[F, O](err) with Def[Halt[F, O]] {
    lazy val selfType = element[Halt[F, O]]
  }
  // elem for concrete class
  class HaltElem[F[_], O](val iso: Iso[HaltData[F, O], Halt[F, O]])(implicit override  val eO: Elem[O], override  val cF: Cont[F])
    extends ProcessElem[F, O, Halt[F, O]]
    with ConcreteElem[HaltData[F, O], Halt[F, O]] {
    override lazy val parent: Option[Elem[_]] = Some(processElement(container[F], element[O]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "O" -> Left(eO))
    }

    override def convertProcess(x: Rep[Process[F, O]]) = // Converter is not generated by meta
!!!("Cannot convert from Process to Halt: missing fields List(err)")
    override def getDefaultRep = Halt(element[Throwable].defaultRepValue)
    override lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[Halt[F, O]]
    }
  }

  // state representation type
  type HaltData[F[_], O] = Throwable

  // 3) Iso for concrete class
  class HaltIso[F[_], O](implicit eO: Elem[O], cF: Cont[F])
    extends EntityIso[HaltData[F, O], Halt[F, O]] with Def[HaltIso[F, O]] {
    override def from(p: Rep[Halt[F, O]]) =
      p.err
    override def to(p: Rep[Throwable]) = {
      val err = p
      Halt(err)
    }
    lazy val eFrom = element[Throwable]
    lazy val eTo = new HaltElem[F, O](self)
    lazy val selfType = new HaltIsoElem[F, O](eO, cF)
    def productArity = 2
    def productElement(n: Int) = (eO, cF).productElement(n)
  }
  case class HaltIsoElem[F[_], O](eO: Elem[O], cF: Cont[F]) extends Elem[HaltIso[F, O]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new HaltIso[F, O]()(eO, cF))
    lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[HaltIso[F, O]]
    }
  }
  // 4) constructor and deconstructor
  class HaltCompanionAbs extends CompanionDef[HaltCompanionAbs] with HaltCompanion {
    def selfType = HaltCompanionElem
    override def toString = "Halt"

    def apply[F[_], O](err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]] =
      mkHalt(err)
  }
  object HaltMatcher {
    def unapply[F[_], O](p: Rep[Process[F, O]]) = unmkHalt(p)
  }
  lazy val Halt: Rep[HaltCompanionAbs] = new HaltCompanionAbs
  implicit def proxyHaltCompanion(p: Rep[HaltCompanionAbs]): HaltCompanionAbs = {
    proxyOps[HaltCompanionAbs](p)
  }

  implicit case object HaltCompanionElem extends CompanionElem[HaltCompanionAbs] {
    lazy val tag = weakTypeTag[HaltCompanionAbs]
    protected def getDefaultRep = Halt
  }

  implicit def proxyHalt[F[_], O](p: Rep[Halt[F, O]]): Halt[F, O] =
    proxyOps[Halt[F, O]](p)

  implicit class ExtendedHalt[F[_], O](p: Rep[Halt[F, O]])(implicit eO: Elem[O], cF: Cont[F]) {
    def toData: Rep[HaltData[F, O]] = isoHalt(eO, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoHalt[F[_], O](implicit eO: Elem[O], cF: Cont[F]): Iso[HaltData[F, O], Halt[F, O]] =
    reifyObject(new HaltIso[F, O]()(eO, cF))

  // 6) smart constructor and deconstructor
  def mkHalt[F[_], O](err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]]
  def unmkHalt[F[_], O](p: Rep[Process[F, O]]): Option[(Rep[Throwable])]

  registerModule(Processes_Module)
}

// Seq -----------------------------------
trait ProcessesSeq extends scalan.ScalanDslSeq with ProcessesDsl {
  self: ProcessesDslSeq =>
  lazy val Process: Rep[ProcessCompanionAbs] = new ProcessCompanionAbs {
  }

  case class SeqAwait[F[_], A, O]
      (override val req: Rep[F[A]], override val recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends AbsAwait[F, A, O](req, recv) {
  }

  def mkAwait[F[_], A, O]
    (req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
    new SeqAwait[F, A, O](req, recv)
  def unmkAwait[F[_], A, O](p: Rep[Process[F, O]]) = p match {
    case p: Await[F, A, O] @unchecked =>
      Some((p.req, p.recv))
    case _ => None
  }

  case class SeqEmit[F[_], O]
      (override val head: Rep[O], override val tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsEmit[F, O](head, tail) {
  }

  def mkEmit[F[_], O]
    (head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
    new SeqEmit[F, O](head, tail)
  def unmkEmit[F[_], O](p: Rep[Process[F, O]]) = p match {
    case p: Emit[F, O] @unchecked =>
      Some((p.head, p.tail))
    case _ => None
  }

  case class SeqHalt[F[_], O]
      (override val err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsHalt[F, O](err) {
  }

  def mkHalt[F[_], O]
    (err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]] =
    new SeqHalt[F, O](err)
  def unmkHalt[F[_], O](p: Rep[Process[F, O]]) = p match {
    case p: Halt[F, O] @unchecked =>
      Some((p.err))
    case _ => None
  }
}

// Exp -----------------------------------
trait ProcessesExp extends scalan.ScalanDslExp with ProcessesDsl {
  self: ProcessesDslExp =>
  lazy val Process: Rep[ProcessCompanionAbs] = new ProcessCompanionAbs {
  }

  case class ExpAwait[F[_], A, O]
      (override val req: Rep[F[A]], override val recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends AbsAwait[F, A, O](req, recv)

  object AwaitMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f
  }

  object AwaitCompanionMethods {
  }

  def mkAwait[F[_], A, O]
    (req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
    new ExpAwait[F, A, O](req, recv)
  def unmkAwait[F[_], A, O](p: Rep[Process[F, O]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: AwaitElem[F, A, O] @unchecked =>
      Some((p.asRep[Await[F, A, O]].req, p.asRep[Await[F, A, O]].recv))
    case _ =>
      None
  }

  case class ExpEmit[F[_], O]
      (override val head: Rep[O], override val tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsEmit[F, O](head, tail)

  object EmitMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f
  }

  object EmitCompanionMethods {
  }

  def mkEmit[F[_], O]
    (head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
    new ExpEmit[F, O](head, tail)
  def unmkEmit[F[_], O](p: Rep[Process[F, O]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: EmitElem[F, O] @unchecked =>
      Some((p.asRep[Emit[F, O]].head, p.asRep[Emit[F, O]].tail))
    case _ =>
      None
  }

  case class ExpHalt[F[_], O]
      (override val err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsHalt[F, O](err)

  object HaltMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f
  }

  object HaltCompanionMethods {
  }

  def mkHalt[F[_], O]
    (err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]] =
    new ExpHalt[F, O](err)
  def unmkHalt[F[_], O](p: Rep[Process[F, O]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: HaltElem[F, O] @unchecked =>
      Some((p.asRep[Halt[F, O]].err))
    case _ =>
      None
  }

  object ProcessMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f

    object ++ {
      def unapply(d: Def[_]): Option[(Rep[Process[F, O]], Th[Process[F, O]]) forSome {type F[_]; type O}] = d match {
        case MethodCall(receiver, method, Seq(p, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: ProcessElem[_, _, _] => true; case _ => false }) && method.getName == "$plus$plus" =>
          Some((receiver, p)).asInstanceOf[Option[(Rep[Process[F, O]], Th[Process[F, O]]) forSome {type F[_]; type O}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Process[F, O]], Th[Process[F, O]]) forSome {type F[_]; type O}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object ProcessCompanionMethods {
    object emit {
      def unapply(d: Def[_]): Option[(Rep[O], Rep[Process[F, O]]) forSome {type F[_]; type O}] = d match {
        case MethodCall(receiver, method, Seq(head, tail, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "emit" =>
          Some((head, tail)).asInstanceOf[Option[(Rep[O], Rep[Process[F, O]]) forSome {type F[_]; type O}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[O], Rep[Process[F, O]]) forSome {type F[_]; type O}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `await`: Method has function arguments recv

    object eval {
      def unapply(d: Def[_]): Option[Rep[F[A]] forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "eval" =>
          Some(a).asInstanceOf[Option[Rep[F[A]] forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[F[A]] forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object evalIO {
      def unapply(d: Def[_]): Option[Rep[IO[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "evalIO" =>
          Some(a).asInstanceOf[Option[Rep[IO[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[IO[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object Try {
      def unapply(d: Def[_]): Option[Th[Process[F, O]] forSome {type F[_]; type O}] = d match {
        case MethodCall(receiver, method, Seq(p, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "Try" =>
          Some(p).asInstanceOf[Option[Th[Process[F, O]] forSome {type F[_]; type O}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Th[Process[F, O]] forSome {type F[_]; type O}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object Processes_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAANVXTWwbRRSe9U8c22kSKL+p2oTIgEBgF4TUQ6RWJrWhyMRRNgdkqkbj9cTZMruzmR2naw499gA3xIUDEpW4IPWCOHFBXJAQB8ShQpU4ceBUilAP9ATizXh3vf5ZJ6GUiD2MdmbfvHnzfd97M3vjDkq7HD3jGphiu2gRgYu6ei+7oqBXbGGK7pus1aHkPNl+gn39yUufLXyZQHMNNLWD3fMubaBs76XiOeG7TnZrKIttg7iCcVegp2pqhZLBKCWGMJldMi2rI3CTklLNdMVKDaWarNXdRVeRVkPzBrMNTgTRVyl2XeL649NERmSG/azqd+tOfw27JHdRiuxik2NTQPiwxnzPfoM4etdmdtcSaNYPre7IsMAmY1oO4yJYIgPudlgr6KZsDAPo4dplvIdLsES7pAtu2m2YmXew8Q5ukzUwkeYpCNgldHuz66h+soZyLtkFgC5YDlUjnoMQAgZeVkEU+/gUQ3yKEp+CTriJqfkulh/XOfO6qPdoSYQ8B1y8sI+LwAOp2K3CexeNt+/peSshJ3sylIza4RQ4WoxRg6ICcPx24wP37mvXzyRQroFypltuuoJjQ0Qp99HKY9tmQsUcAoh5G9hajmNLrVIGmyFJZA1mOdgGTz6UM8ATNQ1TSGM5NuOzEwN9RjgkMNU8Rwv3uxSzX6WbVUzp+u0nX3z618pbCZQYXCILLnUQPg+cCpQBbgAD1/cu2zmBtGofYtmtq65ssl6/zUwIJoTl2du/tb45jS4mQjD9tQ/GH7hIu7d+zN987lwCTTeU2qsUtxuAp1uhxKrzVWaLBppme4T3vmT2MJVvY/nMtMg27lDhoxyFJwnwCLQUm5cOkditqBzQAgDyPRmvMZsUquuFP/TvPrwhVcrRTO9LL1H/Ms/8+dPstlACFijJyW6AbhLSexj+oX55BH/1aSEMRDaLAqU4MfbG+OXoVJxoHFLt2MbNCx8dnzu59bOSzFSLWdhUuj1RQ2kORUNBdMInDXynCk3MB4PMbu5wdkWm76B2orEHg/ejur72cj2AdWaRh5bvmpeuvy+UyjRvsODVm5ehwqyoeacmCC4ovJ9fu/bo759uHVcFY7ppCgs7hdOHKBdBdj/AcoBCbHp4Pd7vKy2AxmbLV2RBiK67GJkQwX5BG9JRgpRDpmWSTZBjnIP6JAf1/R0Y1dCBTPCxSomKQqC02q+aH8r+ZLzsAczHNmqP0Dvnvkqg9BsovQ0lwwW9N1nHbgUswcEuiCdeDca0QZaAFcyxFbKiniXUx3oomS+NtdgahmO8WbnP3niDEVjz2iBu91ftR2QzUn52CG5NKGujtI94ENik8R7+jboh27OqLR8giY5VrH+cQ/99CqRktNEMiNfKPqqUjXEwWR616pKE8zjJyOaVCY4PIYTXMf0fCUFGe5RCiBhP+RgPhp2Eg/bBJHeEvENyPO9HMIbmyFX56GGV7Q+DvsAw68cHVxR0zD/34E+H+MfTWTgOl2OOQ92/awAnV+99vPb891/8om6COXlrgZutHf5e9g8/b6g2z4TLww9jJGQQo7zKqHD/BkqbovXADwAA"
}
}

