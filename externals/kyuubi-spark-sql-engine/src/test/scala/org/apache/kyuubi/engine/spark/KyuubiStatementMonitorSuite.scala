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

package org.apache.kyuubi.engine.spark

import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

import org.apache.hive.service.rpc.thrift._
import org.apache.hive.service.rpc.thrift.TCLIService.Iface
import org.apache.hive.service.rpc.thrift.TOperationState._
import org.apache.spark.scheduler.JobSucceeded
import org.scalatest.PrivateMethodTester

import org.apache.kyuubi.engine.spark.monitor.KyuubiStatementMonitor
import org.apache.kyuubi.engine.spark.monitor.entity.{KyuubiJobInfo, KyuubiStatementInfo}
import org.apache.kyuubi.operation.{HiveJDBCTests, OperationHandle}

class KyuubiStatementMonitorSuite extends WithSparkSQLEngine with HiveJDBCTests
    with PrivateMethodTester {

  override protected def jdbcUrl: String = getJdbcUrl
  override def withKyuubiConf: Map[String, String] = Map.empty

  test("add kyuubiStatementInfo into queue and remove them by size type threshold") {
    val sql = "select timestamp'2021-06-01'"
    val total: Int = 7
    // Clear kyuubiStatementQueue first
    val getQueue = PrivateMethod[
      ArrayBlockingQueue[KyuubiStatementInfo]](Symbol("kyuubiStatementQueue"))()
    val kyuubiStatementQueue = KyuubiStatementMonitor.invokePrivate(getQueue)
    kyuubiStatementQueue.clear()
    withSessionHandle { (client, handle) =>
      for ( a <- 1 to total ) {
        val req = new TExecuteStatementReq()
        req.setSessionHandle(handle)
        req.setStatement(sql)
        val tExecuteStatementResp = client.ExecuteStatement(req)
        val operationHandle = tExecuteStatementResp.getOperationHandle
        waitForOperationToComplete(client, operationHandle)
      }

      var iterator = kyuubiStatementQueue.iterator()
      while (iterator.hasNext) {
        val kyuubiStatementInfo = iterator.next()
        assert(kyuubiStatementInfo.statement !== null)
        assert(kyuubiStatementInfo.statementId !== null)
        assert(kyuubiStatementInfo.sessionId !== null)
        assert(kyuubiStatementInfo.queryExecution !== null)
        assert(kyuubiStatementInfo.stateToTime.size === 4)
      }
      iterator = null

      // Test for clear kyuubiStatementQueue
      // This function is used for avoiding mem leak
      val req = new TExecuteStatementReq()
      req.setSessionHandle(handle)
      req.setStatement(sql)
      val tExecuteStatementResp = client.ExecuteStatement(req)
      val operationHandle = tExecuteStatementResp.getOperationHandle
      waitForOperationToComplete(client, operationHandle)

      assert(kyuubiStatementQueue.size() === 1)
    }
  }

  test("add kyuubiJobInfo into queue and remove them when threshold reached") {
    val sql = "select timestamp'2021-06-01'"
    val getJobQueue = PrivateMethod[
      ArrayBlockingQueue[KyuubiJobInfo]](Symbol("kyuubiJobQueue"))()
    val getJobMap = PrivateMethod[
      ConcurrentHashMap[Int, KyuubiJobInfo]](Symbol("jobIdToJobInfoMap"))()

    val kyuubiJobQueue = KyuubiStatementMonitor.invokePrivate(getJobQueue)
    val jobIdToJobInfoMap = KyuubiStatementMonitor.invokePrivate(getJobMap)
    kyuubiJobQueue.clear()
    jobIdToJobInfoMap.clear()
    withSessionHandle { (client, handle) =>
      val req = new TExecuteStatementReq()
      req.setSessionHandle(handle)
      req.setStatement(sql)
      val tExecuteStatementResp = client.ExecuteStatement(req)
      val opHandle = tExecuteStatementResp.getOperationHandle
      waitForOperationToComplete(client, opHandle)

      val kyuubiJobInfo = kyuubiJobQueue.peek()
      assert(kyuubiJobInfo.statementId === OperationHandle(opHandle).identifier.toString)
      assert(kyuubiJobQueue.size() === 1)
      assert(kyuubiJobInfo.jobId === 0)
      assert(kyuubiJobInfo.stageIds.length === 1)
      assert(kyuubiJobInfo.stageIds.apply(0) === 0)
      assert(kyuubiJobInfo.jobResult === JobSucceeded)
      assert(kyuubiJobInfo.endTime !== None)
      assert(jobIdToJobInfoMap.size() === 0)
    }
  }

  private def waitForOperationToComplete(client: Iface, op: TOperationHandle): Unit = {
    val req = new TGetOperationStatusReq(op)
    var state = client.GetOperationStatus(req).getOperationState
    while (state == INITIALIZED_STATE || state == PENDING_STATE || state == RUNNING_STATE) {
      state = client.GetOperationStatus(req).getOperationState
    }
  }
}
