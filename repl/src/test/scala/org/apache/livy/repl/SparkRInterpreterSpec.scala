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

import org.apache.spark.SparkConf
import org.json4s.DefaultFormats
import org.json4s.JsonDSL._
import org.scalatest._

import org.apache.livy.rsc.driver.SparkEntries

private class SparkRInterpreterSpec extends BaseInterpreterSpec {

  implicit val formats = DefaultFormats

  override protected def withFixture(test: NoArgTest): Outcome = {
    assume(!sys.props.getOrElse("skipRTests", "false").toBoolean, "Skipping R tests.")
    super.withFixture(test)
  }


  override def createInterpreter(): Interpreter = {
    val sparkConf = new SparkConf()
    SparkRInterpreter(sparkConf, new SparkEntries(sparkConf))
  }
  /** Commenting out these tests as we will not have R/SParkR in all the platform by default
  it should "execute `1 + 2` == 3" in withInterpreter { interpreter =>
    val response = interpreter.execute("1 + 2")
    response should equal (Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> "[1] 3"
    ))
  }

  it should "execute multiple statements" in withInterpreter { interpreter =>
    var response = interpreter.execute("x = 1")
    response should equal (Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> ""
    ))

    response = interpreter.execute("y = 2")
    response should equal (Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> ""
    ))

    response = interpreter.execute("x + y")
    response should equal (Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> "[1] 3"
    ))
  }

  it should "execute multiple statements in one block" in withInterpreter { interpreter =>
    val response = interpreter.execute(
      """
        |x = 1
        |
        |y = 2
        |
        |x + y
      """.stripMargin)
    response should equal(Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> "[1] 3"
    ))
  }

  it should "get multiple outputs in one block" in withInterpreter { interpreter =>
    val response = interpreter.execute(
      """
        |print("1")
        |print("2")
      """.stripMargin)
    response should equal(Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> "[1] \"1\"\n[1] \"2\""
    ))
  }

  it should "capture stdout" in withInterpreter { interpreter =>
    val response = interpreter.execute("cat(3)")
    response should equal(Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> "3"
    ))
  }

  it should "report an error if accessing an unknown variable" in withInterpreter { interpreter =>
    val response = interpreter.execute("x")
    assert(response.isInstanceOf[Interpreter.ExecuteError])
    val errorResponse = response.asInstanceOf[Interpreter.ExecuteError]
    errorResponse.ename should be ("Error")
    assert(errorResponse.evalue.contains("object 'x' not found"))
  }


  it should "not hang when executing incomplete statements" in withInterpreter { interpreter =>
    val response = interpreter.execute("x[")
    response should equal(Interpreter.ExecuteError(
      "Error",
        """[1] "Error in parse(text = \"x[\"): <text>:2:0: unexpected end of input\n1: x[\n   ^""""
    ))
  }

  it should "escape the statement" in withInterpreter { interpreter =>
    val response = interpreter.execute("print(\"a\")")
    response should equal(Interpreter.ExecuteSuccess(
      TEXT_PLAIN -> "[1] \"a\""
    ))
  }
  */
}
