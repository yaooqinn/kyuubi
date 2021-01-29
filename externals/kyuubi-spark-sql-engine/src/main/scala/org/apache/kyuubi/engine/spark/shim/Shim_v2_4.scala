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

package org.apache.kyuubi.engine.spark.shim

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.DataType

class Shim_v2_4 extends SparkShim {

  override def getCatalogs(spark: SparkSession): Seq[Row] = {
    Seq(Row(""))
  }

  override def catalogExists(spark: SparkSession, catalog: String): Boolean = false

  override def getSchemas(
      spark: SparkSession,
      catalogName: String,
      schemaPattern: String): Seq[Row] = {
    (spark.sessionState.catalog.listDatabases(schemaPattern) ++
      getGlobalTempViewManager(spark, schemaPattern)).map(Row(_, ""))
  }

  // TODO
  override def toHiveString(value: Any, typ: DataType): String = ""
}
