package scalan.compilation.lms.cxx

import java.io._

import scalan.JNIExtractorOpsExp
import scalan.community.ScalanCommunityExp
import scalan.compilation.GraphVizExport
import scalan.compilation.lms.LmsCompiler
import scalan.compilation.lms.common.JNILmsOps
import scalan.util.{FileUtil, ProcessUtil}

trait LmsCompilerCXX extends LmsCompiler with JNIExtractorOpsExp { self: ScalanCommunityExp with GraphVizExport =>

  override def createManifest[T]: PartialFunction[Elem[T], Manifest[_]] = {
    case el: JNITypeElem[_] =>
      Manifest.classType(classOf[JNILmsOps#JNIType[_]], createManifest(el.tElem))
    case el: JNIArrayElem[arr_t] =>
      el.eItem match {
        case ei: Elem[a_t] =>
          val mItem = createManifest(ei)
          Manifest.classType(classOf[JNILmsOps#JNIArray[a_t]], mItem)
      }
    case el =>
      super.createManifest(el)
  }

  def generate[A, B](sourcesDir: File, executableDir: File, functionName: String, func: Exp[A => B], emitGraphs: Boolean)
                           (implicit config: Config): Unit = {
    sourcesDir.mkdirs()
    executableDir.mkdirs()
    val eFunc = func.elem
    val graph = buildGraph(sourcesDir, functionName, func, emitGraphs)(config)
    doGenerate(sourcesDir, executableDir, functionName, graph, emitGraphs)(config, eFunc.eDom, eFunc.eRange)
  }

  protected def doGenerate[A,B](sourcesDir: File, executableDir: File, functionName: String, graph: PGraph, emitGraphs: Boolean)
                                      (config: Config, eInput: Elem[A], eOutput: Elem[B]) = {

    val outputSource = new File(sourcesDir, functionName + ".cxx")

    (createManifest(eInput), createManifest(eOutput)) match {
      case (mA: Manifest[a], mB: Manifest[b]) =>
        val bridge = makeBridge[a, b]
        val facade = bridge.getFacade(graph.asInstanceOf[bridge.scalan.PGraph])
        val codegen = bridge.lms.codegen

        FileUtil.withFile(outputSource) { writer =>
          codegen.emitSource[a, b](facade.apply, functionName, writer)(mA, mB)
          //          val s = bridge.lms.fresh[a](mA)
          //          val body = codegen.reifyBlock(facade.apply(s))(mB)
          //          codegen.emitSource(List(s), body, functionName, writer)(mB)
          //          val bridge.lms.TP(sym,_) = bridge.lms.globalDefs.last
          //          codegen.emitDepGraph( sym, new File( sourcesDir, functionName + "-LMS.dot" ).getAbsolutePath )
          codegen.emitDataStructures(writer)
        }
    }
  }

  override protected def doBuildExecutable[A,B](sourcesDir: File, executableDir: File, functionName: String, graph: PGraph, emitGraphs: Boolean)
                                      (config: Config, eInput: Elem[A], eOutput: Elem[B]) = {
    /* LMS stuff */

    val outputSource = new File(sourcesDir, functionName + ".cxx")

    (createManifest(eInput), createManifest(eOutput)) match {
      case (mA: Manifest[a], mB: Manifest[b]) =>
        val bridge = makeBridge[a, b]
        val facade = bridge.getFacade(graph.asInstanceOf[bridge.scalan.PGraph])
        val codegen = bridge.lms.codegen

        FileUtil.withFile(outputSource) { writer =>
          codegen.emitSource[a, b](facade.apply, functionName, writer)(mA, mB)
//          val s = bridge.lms.fresh[a](mA)
//          val body = codegen.reifyBlock(facade.apply(s))(mB)
//          codegen.emitSource(List(s), body, functionName, writer)(mB)
//          val bridge.lms.TP(sym,_) = bridge.lms.globalDefs.last
//          codegen.emitDepGraph( sym, new File( sourcesDir, functionName + "-LMS.dot" ).getAbsolutePath )
          codegen.emitDataStructures(writer)
        }
    }

//    val command = Seq("scalac", "-d", jarPath(functionName, executableDir)) ++ config.extraCompilerOptions :+
//      outputSource.getAbsolutePath
//
    val command = Seq("make")
    ProcessUtil.launch(new File(sourcesDir,"release"), command: _*)
  }

  override protected def doExecute[A, B](executableDir: File, functionName: String, input: A)
                               (config: Config, eInput: Elem[A], eOutput: Elem[B]): B = {
//    val url = new File(jarPath(functionName, executableDir)).toURI.toURL
//    // ensure Scala library is available
//    val classLoader = new URLClassLoader(scala.Array(url), classOf[_ => _].getClassLoader)
//    val cls = classLoader.loadClass(functionName)
//    val argumentClass = eInput.classTag.runtimeClass
//    val method = cls.getMethod("apply", argumentClass)
//    val result = method.invoke(cls.newInstance(), input.asInstanceOf[AnyRef])
//    result.asInstanceOf[B]
    null.asInstanceOf[B]
  }

  private def jarPath(functionName: String, executableDir: File) =
    s"${executableDir.getAbsolutePath}/$functionName.jar"
}
