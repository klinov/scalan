package scalan.compilation.lms.cxx.sharedptr

import java.lang.reflect.Method

import scalan.compilation.language.ScalaInterpreter
import scalan.compilation.lms.{CoreLmsBackend, CoreBridge}

trait CoreBridgeCxx extends CoreBridge {
  override val lms: CoreLmsBackend with CxxMethodCallOpsExp
  import scalan._

  override def transformMethodCall[T](m: LmsMirror, receiver: Exp[_], method: Method, args: List[AnyRef], returnType: Elem[T]): lms.Exp[_] = {
    mappedFunc(method) match {
      case Some(func: CppMappingDSL#CppFunc) => func.lib match {
        case e: CppMappingDSL#CppLib =>
//          val param = func.wrapper match {
//            case true => Seq(m.symMirrorUntyped(receiver))
//            case false => Seq.empty[lms.Exp[_]]
//          }
          elemToManifest(returnType) match {
            case (mA: Manifest[a]) =>
              val lmsArgs = /*param ++ */args.collect { case v: Exp[_] => m.symMirrorUntyped(v) }
              lms.cxxMethodCall[a](null, lms.Pure, func.name, List.empty, lmsArgs: _*)(mA.asInstanceOf[Manifest[a]])
          }
//        case e: CppMappingDSL#EmbeddedObject if e.name == "lms" =>
//          val obj = m.symMirrorUntyped(receiver)
//          val name = func.name
//          val lmsMethod = lmsMemberByName(name).asMethod
//          lmsMirror.reflectMethod(lmsMethod).apply(obj, elemToManifest(receiver.elem)).asInstanceOf[lms.Exp[_]]
      }
      case Some(nonCxxFunc) =>
        !!!(s"$nonCxxFunc is not a CppMappingDSL#CppFunc")
      case None =>
        val obj = m.symMirrorUntyped(receiver)
        elemToManifest(returnType) match {
          case mA: Manifest[a] => lms.cxxMethodCall[a](obj, lms.Pure, method.getName,
            args.collect {
              case elem: Elem[_] => lms.TypeArg(elemToManifest(elem))
            },
            /* filter out implicit ClassTag params */
            args.collect { case v: Exp[_] => m.symMirrorUntyped(v) }: _*)(mA.asInstanceOf[Manifest[a]])
        }
    }
  }
}
