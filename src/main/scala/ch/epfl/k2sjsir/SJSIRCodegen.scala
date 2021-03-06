package ch.epfl.k2sjsir

import java.io.{File, FileOutputStream}

import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.{
  InvalidIRException,
  Serializers
}

object SJSIRCodegen {

  def genIRFile(outDir: String, cd: ClassDescriptor, tree: ClassDef): Unit = {
    val name = cd.toJsClassName.drop(1).replace("_", "/")
    genIRFile(outDir, name, tree)
  }

  def genIRFile(outDir: String, name: String, tree: ClassDef): Unit = {
    val file = new File(outDir, name + ".sjsir")

    file.getParentFile.mkdirs()

    val output = new FileOutputStream(file)
    try {
      Serializers.serialize(output, tree)
    } catch {
      case e: InvalidIRException =>
        e.tree match {
          case _: UndefinedParam =>
            println(
              "Found a dangling UndefinedParam at " +
                s"${e.tree.pos}. This is likely due to a bad interaction " +
                "between a macro or a compiler plugin and the Scala.js " +
                "compiler plugin. If you hit this, please let us know.")
          case _ =>
            println("The Scala.js compiler generated " +
              s"invalid IR for this class. Please report this as a bug. IR: ${e.tree}")
        }
    } finally {
      output.close()
    }
  }

  def genIRDefaultImpls(outDir: String, cd: ClassDescriptor, tree: ClassDef): Unit = {
    val name = cd.toJsDefaultImplName.drop(1).replace("_", "/")
    genIRFile(outDir, name, tree)
  }

  def genIREnumCompanion(outDir: String, cd: ClassDescriptor, tree: ClassDef): Unit = {
    val name: String = cd.toJsEnumCompanionName.drop(1).replace("_", "/")
    genIRFile(outDir, name, tree)
  }
}
