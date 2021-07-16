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

package org.apache.kyuubi.engine.spark.monitor.listener

import org.apache.spark.scheduler._

import org.apache.kyuubi.Logging
import org.apache.kyuubi.engine.spark.monitor.KyuubiStatementMonitor
import org.apache.kyuubi.engine.spark.monitor.entity.KyuubiJobInfo
import org.apache.kyuubi.operation.AbstractOperation._

/**
 * This listener is used for getting metrics about job, stage, executor and stageExecutor.
 * It's singleton pattern and we will add it into sparkContext when initialize sparkEngine.
 */
class KyuubiStatementListener extends StatsReportListener with Logging{

  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    val statementId = jobStart.properties.getProperty(KYUUBI_STATEMENT_ID_KEY)
    val kyuubiJobInfo = KyuubiJobInfo(
      jobStart.jobId, statementId, jobStart.stageIds, jobStart.time)
    KyuubiStatementMonitor.putJobInfoIntoQueue(kyuubiJobInfo)
    info(s"Add jobStartInfo. Query [$statementId]: Job ${jobStart.jobId} started with " +
      s"${jobStart.stageIds.length} stages")
  }

  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    KyuubiStatementMonitor.addJobEndInfo(jobEnd)
    info(s"Add jobEndInfo. Job ${jobEnd.jobId} state is ${jobEnd.jobResult.toString}")
  }
}
