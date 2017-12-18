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

package org.apache.spark.deploy.nomad

import java.lang.reflect.InvocationTargetException

import org.apache.spark.SparkUserAppException
import org.apache.spark.internal.Logging
import org.apache.spark.util.{ShutdownHookManager, Utils}

/**
 * Calls [[System.exit]] when the main thread completes.
 *
 * This is used to replicate the behaviour of the YARN application master.
 */
object SystemExitOnMainCompletion extends Logging {

  /**
   * The first argument should be the fully qualified class name of the class to run,
   * and the remaining arguments are passed to that class.
   */
  def main(args: Array[String]): Unit = {
    System.exit(runClass(args(0), args.slice(1, args.length)))
  }

  def runClass(className: String, args: Array[String]): Int = {

    val mainMethod =
      Utils.classForName(className)
        .getMethod("main", classOf[Array[String]])

    @volatile
    var earlyExit = false

    val earlyExitDetectionHook = {
      val mainThread = Thread.currentThread()
      val priority = ShutdownHookManager.SPARK_CONTEXT_SHUTDOWN_PRIORITY - 1
      ShutdownHookManager.addShutdownHook(priority) { () =>
        logError("System.exit was called early. Interrupting the main thread.")
        earlyExit = true
        mainThread.interrupt()
      }
    }

    try {
      mainMethod.invoke(null, args)
      0
    } catch {
      case e: InvocationTargetException =>
        if (earlyExit) 16
        else e.getCause match {
          case SparkUserAppException(exitCode) =>
            logError(s"User application exited with status $exitCode")
            exitCode
          case cause: Throwable =>
            logError("User class threw exception: " + cause, cause)
            15
        }
    } finally {
      ShutdownHookManager.removeShutdownHook(earlyExitDetectionHook)
    }
  }
}
