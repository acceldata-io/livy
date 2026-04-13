/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.livy.repl

import java.io.{BufferedReader, File, PrintWriter, StringReader}
import java.lang.reflect.InvocationTargetException
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Paths}

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.Results.Result
import scala.tools.nsc.interpreter.shell.{ILoop, ShellConfig}

import org.apache.spark.SparkConf
import org.apache.spark.repl.SparkILoop

/**
 * Spark 4 + Scala 2.13 use `scala.tools.nsc.interpreter.shell.ILoop` and a different
 * `org.apache.spark.repl.SparkILoop` than Spark 3.2 / Scala 2.12.
 *
 * Livy must not run Spark REPL's default `initializationCommands` (they use
 * `org.apache.spark.repl.Main`); `SparkEntries` + [[AbstractSparkInterpreter.postStart]]
 * own context/session creation.
 */
class SparkInterpreter(protected override val conf: SparkConf) extends AbstractSparkInterpreter {

  private var sparkILoop: SparkILoop = _

  private def intp: IMain = sparkILoop.intp.asInstanceOf[IMain]

  private val interpretPreambleMethod: java.lang.reflect.Method = {
    val m = classOf[ILoop].getDeclaredMethod("interpretPreamble")
    m.setAccessible(true)
    m
  }

  override def start(): Unit = {
    require(sparkILoop == null)

    val rootDir = conf.get("spark.repl.classdir", System.getProperty("java.io.tmpdir"))
    val outputDir = Files.createTempDirectory(Paths.get(rootDir), "spark").toFile
    outputDir.deleteOnExit()
    conf.set("spark.repl.class.outputDir", outputDir.getAbsolutePath)

    val settings = new Settings()
    settings.processArguments(List("-Yrepl-class-based",
      "-Yrepl-outdir", s"${outputDir.getAbsolutePath}"), true)
    settings.usejavacp.value = true
    settings.embeddedDefaults(Thread.currentThread().getContextClassLoader())

    val printWriter = new PrintWriter(outputStream, true)
    val inReader = new BufferedReader(new StringReader(""))

    sparkILoop = new SparkILoop(ShellConfig(settings), inReader, printWriter) {
      override protected def internalReplAutorunCode(): scala.collection.immutable.Seq[String] =
        scala.collection.immutable.Seq.empty
    }

    sparkILoop.createInterpreter(settings)
    intp.reporter.withoutPrintingResults {
      intp.withSuppressedSettings {
        intp.initializeCompiler()
        if (intp.reporter.hasErrors) {
          throw new RuntimeException(
            "Scala interpreter encountered errors during initialization")
        }
        sparkILoop.echoOff {
          try {
            interpretPreambleMethod.invoke(sparkILoop)
          } catch {
            case e: InvocationTargetException =>
              throw Option(e.getCause).getOrElse(e)
          }
        }
      }
    }

    restoreContextClassLoader {
      var classLoader = Thread.currentThread().getContextClassLoader
      while (classLoader != null) {
        if (classLoader.getClass.getCanonicalName ==
          "org.apache.spark.util.MutableURLClassLoader") {
          val extraJarPath = classLoader.asInstanceOf[URLClassLoader].getURLs()
            .filter { u => u.getProtocol == "file" && new File(u.getPath).isFile }
            .filterNot { u => Paths.get(u.toURI).getFileName.toString.startsWith("livy-") }
            .filterNot { u =>
              Paths.get(u.toURI).getFileName.toString.contains("org.scala-lang_scala-reflect")
            }

          extraJarPath.foreach { p => debug(s"Adding $p to Scala interpreter's class path...") }
          intp.addUrlsToClassPath(extraJarPath: _*)
          classLoader = null
        } else {
          classLoader = classLoader.getParent
        }
      }

      postStart()
    }
  }

  override def close(): Unit = synchronized {
    super.close()

    if (sparkILoop != null) {
      sparkILoop.closeInterpreter()
      sparkILoop = null
    }
  }

  override def addJar(jar: String): Unit = {
    intp.addUrlsToClassPath(new URL(jar))
  }

  override protected def isStarted(): Boolean = {
    sparkILoop != null
  }

  override protected def interpret(code: String): Result = {
    intp.interpret(code)
  }

  override protected def completeCandidates(code: String, cursor: Int): Array[String] = {
    Array.empty
  }

  override protected def valueOfTerm(name: String): Option[Any] = {
    try {
      Option(intp.lastRequest.lineRep.call("$result"))
    } catch {
      case _: Exception => intp.valueOfTerm(name)
    }
  }

  override protected def bind(name: String,
      tpe: String,
      value: Object,
      modifier: List[String]): Unit = {
    intp.beQuietDuring { () =>
      intp.bind(name, tpe, value, modifier)
      ()
    }
  }
}
