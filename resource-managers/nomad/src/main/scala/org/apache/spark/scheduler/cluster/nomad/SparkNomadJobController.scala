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

import java.util.concurrent.{Callable, ExecutorService, TimeUnit}

import scala.concurrent.Future

import com.hashicorp.nomad.apimodel._
import com.hashicorp.nomad.scalasdk.NomadScalaApi

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.scheduler.cluster.nomad.SparkNomadJob.CommonConf
import org.apache.spark.util.ThreadUtils

/**
 * Manipulates a Nomad job running a Spark application.
 *
 * A single Nomad job is used for a Spark application running with a Nomad master.
 * This includes all of the executors (and possibly shuffle servers),
 * and in cluster mode also includes the Spark driver process.
 */
private[spark] class SparkNomadJobController(jobManipulator: NomadJobManipulator) extends Logging {

  def jobId: String = jobManipulator.jobId

  private val executorService: ExecutorService =
    ThreadUtils.newDaemonSingleThreadExecutor("allocation-stop-wait")

  def driverGroupAndTaskNames: (String, String) = {
    val job = jobManipulator.jobSnapshot
    val group = SparkNomadJob.find(job, DriverTaskGroup).get
    val task = DriverTaskGroup.find(group, DriverTask).get
    (group.getName, task.getName)
  }

  def startDriver(): Option[Evaluation] = {
    logInfo(s"Starting driver in Nomad job ${jobId}")
    jobManipulator.create()
  }

  def fetchDriverLogUrls(conf: SparkConf): Option[Map[String, String]] = {
    for {
      allocId <- Option(conf.getenv("NOMAD_ALLOC_ID"))
      taskName <- Option(conf.getenv("NOMAD_TASK_NAME"))
    } yield jobManipulator.fetchLogUrlsForTask(allocId, taskName)
  }

  def initialiseExecutors(
      jobConf: CommonConf,
      conf: SparkConf,
      driverUrl: String,
      count: Int
  ): Unit = {
    jobManipulator.updateJob(startIfNotYetRunning = count > 0) { job =>
      val group = SparkNomadJob.find(job, ExecutorTaskGroup).get
      ExecutorTaskGroup.initialize(jobConf, conf, group, driverUrl, count)
    }
  }

  def setExecutorCount(count: Int): Unit = {
    jobManipulator.updateJob(startIfNotYetRunning = count > 0) { job =>
      val executorGroup = SparkNomadJob.find(job, ExecutorTaskGroup).get
      if (count == 0 || count > executorGroup.getCount) {
        executorGroup.setCount(count)
      }
    }
  }

  def waitForAllocRemoval(allocId: String): Unit = {
    do {
      Thread.sleep(500)
    } while (!jobManipulator.isAllocStopped(allocId))
  }

  def removeExecutors(executorIds: Seq[String]): Future[Boolean] = {
    try {
      executorIds
        // executor ID == "${allocId}-${epoch}"
        .map(executorId => executorId.replaceFirst("-\\d+$", ""))
        // parallelize this collection so we can wait for allocs to stop concurrently
        .par
        .map(allocId => {
          logInfo(s"stopping alloc $allocId")
          jobManipulator.stopAlloc(allocId)
          // stop all allocs and wait for them concurrently
          executorService.submit(new Callable[String] {
            override def call(): String = {
              waitForAllocRemoval(allocId)
              allocId
            }
          })
        }).foreach(f => {
        val allocId = f.get(10, TimeUnit.SECONDS)
        logInfo(s"alloc stopped $allocId")
      })
    } catch {
      case e: Exception => logError("caught an exception removing executors", e)
        return Future.successful(false)
    }

    val removeCount = executorIds.size
    jobManipulator.updateJob(startIfNotYetRunning = false) { job =>
      val executors = SparkNomadJob.find(job, ExecutorTaskGroup).get
      val currentSize = executors.getCount
      logInfo(s"changing executor count in job spec from " +
        s"$currentSize to ${currentSize - removeCount}")
      executors.setCount(currentSize - removeCount)
    }

    Future.successful(true)
  }

  def resolveExecutorLogUrls(reportedLogUrls: Map[String, String]): Map[String, String] =
    reportedLogUrls.get(ExecutorTask.LOG_KEY_FOR_ALLOC_ID) match {
      case None =>
        logWarning(s"Didn't find expected ${ExecutorTask.LOG_KEY_FOR_ALLOC_ID} key in " +
          s"executor log URLs: $reportedLogUrls")
        reportedLogUrls
      case Some(allocId) =>
          jobManipulator.fetchLogUrlsForTask(allocId, "executor")
    }

  def close(): Unit = {
    jobManipulator.close()
  }

}

private[spark] object SparkNomadJobController extends Logging {

  def initialize(
      clusterModeConf: NomadClusterManagerConf,
      nomad: NomadScalaApi
  ): SparkNomadJobController = {
    try {
      val manipulator = NomadJobManipulator.fetchOrCreateJob(nomad, clusterModeConf.jobDescriptor)
      new SparkNomadJobController(manipulator)
    } catch {
      case e: Throwable =>
        nomad.close()
        throw e
    }
  }

}
