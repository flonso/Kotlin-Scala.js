/*                     __                                               *\
**     ________ ___   / /  ___      __ ____  Scala.js CLI               **
**    / __/ __// _ | / /  / _ | __ / // __/  (c) 2013-2014, LAMP/EPFL   **
**  __\ \/ /__/ __ |/ /__/ __ |/_// /_\ \    http://scala-js.org/       **
** /____/\___/_/ |_/____/_/ | |__/ /____/                               **
**                          |/____/                                     **
\*                                                                      */

// Modified by Guillaume Tournigand and Lionel Fleury
import java.io.File
import java.net.URI

import org.scalajs.core.ir.ScalaJSVersions
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.linker._
import org.scalajs.core.tools.linker.standard._
import org.scalajs.core.tools.logging._

import scala.collection.immutable.Seq

object Scalajsld {

  private case class Options(
                              cp: Seq[File] = Seq.empty,
                              moduleInitializers: Seq[ModuleInitializer] = Seq.empty,
                              output: File = null,
                              jsoutput: Boolean = false,
                              semantics: Semantics = Semantics.Defaults,
                              outputMode: OutputMode = OutputMode.ECMAScript51Isolated,
                              moduleKind: ModuleKind = ModuleKind.NoModule,
                              noOpt: Boolean = false,
                              fullOpt: Boolean = false,
                              prettyPrint: Boolean = false,
                              sourceMap: Boolean = false,
                              relativizeSourceMap: Option[URI] = None,
                              bypassLinkingErrors: Boolean = false,
                              checkIR: Boolean = false,
                              stdLib: Option[File] = None,
                              logLevel: Level = Level.Info)

  private implicit object MainMethodRead extends scopt.Read[ModuleInitializer] {
    val arity = 1
    val reads = { (s: String) =>
      val lastDot = s.lastIndexOf('.')
      if (lastDot < 0)
        throw new IllegalArgumentException(s"$s is not a valid main method")
      ModuleInitializer.mainMethod(s.substring(0, lastDot),
        s.substring(lastDot + 1))
    }
  }

  private implicit object OutputModeRead extends scopt.Read[OutputMode] {
    val arity = 1
    val reads = { (s: String) =>
      OutputMode.All.find(_.toString() == s).getOrElse(
        throw new IllegalArgumentException(s"$s is not a valid output mode"))
    }
  }

  private implicit object ModuleKindRead extends scopt.Read[ModuleKind] {
    val arity = 1
    val reads = { (s: String) =>
      ModuleKind.All.find(_.toString() == s).getOrElse(
        throw new IllegalArgumentException(s"$s is not a valid module kind"))
    }
  }

  def run(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Options]("scalajsld") {
      head("scalajsld", ScalaJSVersions.current)
      arg[File]("<value> ...")
        .unbounded()
        .action { (x, c) => c.copy(cp = c.cp :+ x) }
        .text("Entries of Scala.js classpath to link")
      opt[File]('o', "output")
        .valueName("<file>")
        .required()
        .action { (x, c) => c.copy(output = x) }
        .text("Output file of linker (required)")
      opt[File]("stdlib")
        .valueName("<scala.js stdlib jar>")
        .hidden()
        .action { (x, c) => c.copy(stdLib = Some(x)) }
        .text("Location of Scala.js standard library. This is set by the " +
          "runner script and automatically prepended to the classpath. " +
          "Use -n to not include it.")
      opt[Unit]('c', "checkIR")
        .action { (_, c) => c.copy(checkIR = true) }
        .text("Check IR before optimizing")

      override def showUsageOnError = true
    }

    for (options <- parser.parse(args, Options())) {
      val classpath = options.stdLib.toList ++ options.cp
      val irContainers = FileScalaJSIRContainer.fromClasspath(classpath)
      val moduleInitializers = options.moduleInitializers

      val semantics =
        if (options.fullOpt) options.semantics.optimized
        else options.semantics

      val config = StandardLinker.Config()
        .withSemantics(semantics)
        .withSourceMap(options.sourceMap)
        .withOptimizer(!options.noOpt)
        .withParallel(true)
        .withOutputMode(options.outputMode)
        .withModuleKind(options.moduleKind)
        // TODO: Remove it
        .withCheckIR(false)

      val linker = StandardLinker(config)

      val logger = new ScalaConsoleLogger(options.logLevel)
      val outFile = WritableFileVirtualJSFile(options.output)
      val cache = (new IRFileCache).newCache

      linker.link(cache.cached(irContainers), moduleInitializers, outFile,
        logger)
    }
  }
}