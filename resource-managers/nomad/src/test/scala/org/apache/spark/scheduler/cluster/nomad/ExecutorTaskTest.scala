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

package org.apache.spark.scheduler.cluster.nomad

import java.net.URI
import java.util
import java.util.Arrays.asList

import com.hashicorp.nomad.apimodel.Task

import org.apache.spark.{SparkConf, SparkFunSuite}


class ExecutorTaskTest extends SparkFunSuite {

  test("should set arguments idempotently, allowing the driver url to vary") {
    val commonConf = SparkNomadJob.CommonConf(
      appId = "app-123",
      appName = "App",
      dockerImage = None,
      dockerAuth = None,
      sparkDistribution = Some(new URI("local:///here/is/spark")),
      preventOverwrite = true
    )
    val task = new Task()

    val task2 = ExecutorTask.configure(commonConf, new SparkConf(), task, None)
    assert(task2 eq task)

    ExecutorTask.addDriverArguments(commonConf, new SparkConf(), task, "driver/url/1")

    assertResult(asList(
      "org.apache.spark.executor.NomadExecutorBackend",
      "--hostname", "${NOMAD_IP_executor}",
      "--app-id", "app-123",
      "--cores", "1",
      "--driver-url", "driver/url/1"
    ), task) {
      task.getConfig.get("args").asInstanceOf[java.util.List[String]]
    }

    ExecutorTask.addDriverArguments(commonConf, new SparkConf(), task, "driver/url/1")

    assertResult(asList(
      "org.apache.spark.executor.NomadExecutorBackend",
      "--hostname", "${NOMAD_IP_executor}",
      "--app-id", "app-123",
      "--cores", "1",
      "--driver-url", "driver/url/1"
    ), task) {
      task.getConfig.get("args").asInstanceOf[java.util.List[String]]
    }

    ExecutorTask.addDriverArguments(commonConf, new SparkConf(), task, "driver/url/22222")

    assertResult(asList(
      "org.apache.spark.executor.NomadExecutorBackend",
      "--hostname", "${NOMAD_IP_executor}",
      "--app-id", "app-123",
      "--cores", "1",
      "--driver-url", "driver/url/22222"
    ), task) {
      task.getConfig.get("args").asInstanceOf[java.util.List[String]]
    }
  }

  test("should append arguments idempotently, allowing the driver url to vary") {
    val commonConf = SparkNomadJob.CommonConf(
      appId = "app-123",
      appName = "App",
      dockerImage = None,
      dockerAuth = None,
      sparkDistribution = Some(new URI("local:///here/is/spark")),
      preventOverwrite = true
    )
    val task = new Task()
      .addConfig("args", new util.ArrayList(asList("template-arg-1", "template arg 2")))

    val task2 = ExecutorTask.configure(commonConf, new SparkConf(), task, None)
    assert(task2 eq task)

    ExecutorTask.addDriverArguments(commonConf, new SparkConf(), task, "driver/url/1")

    assertResult(asList(
      "template-arg-1", "template arg 2",
      "org.apache.spark.executor.NomadExecutorBackend",
      "--hostname", "${NOMAD_IP_executor}",
      "--app-id", "app-123",
      "--cores", "1",
      "--driver-url", "driver/url/1"
    ), task) {
      task.getConfig.get("args").asInstanceOf[java.util.List[String]]
    }

    ExecutorTask.addDriverArguments(commonConf, new SparkConf(), task, "driver/url/1")

    assertResult(asList(
      "template-arg-1", "template arg 2",
      "org.apache.spark.executor.NomadExecutorBackend",
      "--hostname", "${NOMAD_IP_executor}",
      "--app-id", "app-123",
      "--cores", "1",
      "--driver-url", "driver/url/1"
    ), task) {
      task.getConfig.get("args").asInstanceOf[java.util.List[String]]
    }

    ExecutorTask.addDriverArguments(commonConf, new SparkConf(), task, "driver/url/22222")

    assertResult(asList(
      "template-arg-1", "template arg 2",
      "org.apache.spark.executor.NomadExecutorBackend",
      "--hostname", "${NOMAD_IP_executor}",
      "--app-id", "app-123",
      "--cores", "1",
      "--driver-url", "driver/url/22222"
    ), task) {
      task.getConfig.get("args").asInstanceOf[java.util.List[String]]
    }
  }

}
