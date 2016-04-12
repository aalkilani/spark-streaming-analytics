/**
  * Copyright 2015 Grid Compass Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package Utils

import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.{SparkContext, SparkConf}

object SparkUtils {
  def getSparkContext(checkpointDir : Option[String]) = {
    val conf = new SparkConf()
      .setMaster("local[2]") // don't hardcode in your production code (not a good example)
      .setAppName("High velocity analytics")

    val sc = SparkContext.getOrCreate(conf)
    checkpointDir.foreach(dir => sc.setCheckpointDir(dir))

    // Verify that the attached Spark cluster is 1.6.0+
    require(sc.version.replace(".", "").substring(0,3).toInt >= 152, "Spark 1.6.0+ is required to run this program. Please attach it to a Spark 1.6.0+ cluster.")
    sc
  }

  def getSQLContext()(implicit sc : SparkContext) = {
    val sqlContext = new SQLContext(sc)
    sqlContext
  }

  def getStreamingContext(createStreamingApplication : (SparkContext, Duration) => StreamingContext)(implicit sc : SparkContext, batchDuration : Duration) = {
    def creatingFunc = () => createStreamingApplication(sc, batchDuration)
    val ssc = sc.getCheckpointDir match {
      case Some(checkpointDir) => StreamingContext.getActiveOrCreate(checkpointDir, creatingFunc, sc.hadoopConfiguration, createOnError = true)
      case None => creatingFunc()
    }
    ssc
  }
}
