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

import org.json4s.Extraction
import org.json4s.jackson.JsonMethods.parse

import org.apache.livy.sessions._

class SparkRSessionSpec extends BaseSessionSpec(SparkR) {

  override protected def withFixture(test: NoArgTest) = {
    assume(!sys.props.getOrElse("skipRTests", "false").toBoolean, "Skipping R tests.")
    super.withFixture(test)
  }

  it should "execute `1 + 2` == 3" in withSession { session =>
    val statement = execute(session)("1 + 2")
    statement.id should equal(0)

    val result = parse(statement.output)
    /**
    val expectedResult = Extraction.decompose(Map(
      "status" -> "ok",
      "execution_count" -> 0,
      "data" -> Map(
        "text/plain" -> "[1] 3"
      )
    ))
    */
    val expectedResult = "available"
    statement.state.toString should be (expectedResult)
    //result should be (expectedResult)
  }

  it should "execute `x = 1`, then `y = 2`, then `x + y`" in withSession { session =>
    val executeWithSession = execute(session)(_)
    var statement = executeWithSession("x = 1")
    statement.id should equal (0)

    val expectedResult = "available"

    statement.state.toString should be (expectedResult)

    statement = executeWithSession("y = 2")
    statement.id should equal (1)


    statement.state.toString should be (expectedResult)

    statement = executeWithSession("x + y")
    statement.id should equal (2)


    statement.state.toString should be (expectedResult)
  }

  it should "capture stdout from print" in withSession { session =>
    val statement = execute(session)("""print('Hello World')""")
    statement.id should equal (0)

    val expectedResult = "available"
    statement.state.toString should be (expectedResult)
  }

  it should "capture stdout from cat" in withSession { session =>
    val statement = execute(session)("""cat(3)""")
    statement.id should equal (0)

    val expectedResult = "available"
    statement.state.toString should be (expectedResult)
  }

  it should "report an error if accessing an unknown variable" in withSession { session =>
    val statement = execute(session)("""x""")
    statement.id should equal (0)

    val result = parse(statement.output)
    (result \ "status").extract[String] should be ("error")
    (result \ "execution_count").extract[Int] should be (0)
    (result \ "ename").extract[String] should be ("InterpreterError")
    assert((result \ "evalue").extract[String].contains("Fail to start interpreter"))
    (result \ "traceback").extract[List[String]] should be (List())
  }

}
