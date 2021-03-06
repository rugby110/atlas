/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.atlas.lwcapi

import akka.actor.ActorSystem
import com.netflix.spectator.api.Spectator
import org.scalatest.FunSuite
import spray.testkit.ScalatestRouteTest

class ExpressionApiSuite(implicit actorSystem: ActorSystem) extends FunSuite with ScalatestRouteTest {
  import scala.concurrent.duration._

  implicit val routeTestTimeout = RouteTestTimeout(5.second)

  val splitter = new ExpressionSplitterImpl()

  val alertmap = ExpressionDatabaseImpl()
  val endpoint = ExpressionApi(alertmap, Spectator.globalRegistry(), actorSystem)

  test("get of a path returns empty data") {
    Get("/lwc/api/v1/expressions/123") ~> endpoint.routes ~> check {
      assert(responseAs[String] === """[]""")
    }
  }

  test("has data") {
    val split = splitter.split("nf.cluster,skan,:eq,:avg", 60000)
    split.queries.zip(split.expressions).foreach { case (query, expr) =>
        alertmap.addExpr(expr, query)
    }
    alertmap.regenerateQueryIndex()
    Get("/lwc/api/v1/expressions/skan") ~> endpoint.routes ~> check {
      assert(responseAs[String] === """[{"id":"lZSwuIrFJU98PxX5uBUehDutSgA","frequency":60000,"dataExpressions":["nf.cluster,skan,:eq,:count","nf.cluster,skan,:eq,:sum"]}]""")
    }
  }
}
