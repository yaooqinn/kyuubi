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

package org.apache.kyuubi.engine.spark.monitor.entity

import org.apache.spark.scheduler.JobResult

/**
 * This object is used for storing the basic data for job.
 * You can use statementId to get all jobs that belong to this statemnent.
 * And also you can use statementId and jobId to get all stages that belong to this job.
 * @param jobId
 * @param statementId
 * @param stageIds: is array
 * @param startTime
 * @param endTime
 * @param jobResult
 */
case class KyuubiJobInfo(
    jobId: Int,
    statementId: String,
    stageIds: Seq[Int],
    startTime: Long) {

  var endTime: Option[Long] = None
  var jobResult: JobResult = null
}
