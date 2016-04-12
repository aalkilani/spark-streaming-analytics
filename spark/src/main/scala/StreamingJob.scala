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

import Domain.Meetup.RSVP
import Utils.SparkUtils._
import kafka.serializer.{DefaultDecoder, StringDecoder}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext, Minutes}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats
import org.apache.spark.streaming._

import scala.util.Try

object StreamingJob {

  def main(args: Array[String]): Unit = {

    // replace hadoop.home.dir with your own path. Hard coded here for simplicity. Not a good practice!
    System.setProperty("hadoop.home.dir", "F:\\Libraries\\hadoop-common-2.2.0-bin-master") // required for winutils see: http://stackoverflow.com/questions/19620642/failed-to-locate-the-winutils-binary-in-the-hadoop-binary-path
    implicit val sc = getSparkContext(Some("file:///e:/checkpoint"))
    implicit val batchDuration = Seconds(4)

    def streamingFunc(sc: SparkContext, batchDuration: Duration) : StreamingContext = {
      val ssc = new StreamingContext(sc, batchDuration)
      val sqlContext = getSQLContext()(sc)
      import sqlContext.implicits._
/*
      // Example 1 - Start (Receiver based Kafka stream)
      val kafkaParams = Map(
        "zookeeper.connect" -> "localhost:2181",
        "group.id" -> "lambda"
      )

      val rsvpStream =  KafkaUtils.createStream[Array[Byte], String, DefaultDecoder, StringDecoder](
        ssc, kafkaParams, Map("meetup_rsvps" -> 1), StorageLevel.MEMORY_ONLY).map(_._2).mapPartitions { it =>
        implicit val formats = DefaultFormats
        it.flatMap { event =>
          Try(
            parse(event).camelizeKeys.extract[RSVP]
          ).toOption
        }
      }
      // Example 1 - End
      */

      val topics = Set("meetup_rsvps")

      val kafkaDirectParams = Map(
        "metadata.broker.list" -> "localhost:9092",
        "group.id" -> "streaming-analytics",
        "auto.offset.reset" -> "smallest"
      )

      val rsvpStream = KafkaUtils.createDirectStream[Array[Byte], String, DefaultDecoder, StringDecoder](ssc, kafkaDirectParams, topics)
        .map(_._2).mapPartitions { it =>
        implicit val formats = DefaultFormats
        it.flatMap { event =>
          Try(
            parse(event).camelizeKeys.extract[RSVP]
          ).toOption
        }
      }


      // Example 2 - Start (Sample Query, runs every batch, non-stateful operation)
      // Unique users per city (regardless of response)
      rsvpStream.transform { rdd =>
        val df = rdd.toDF()
        df.registerTempTable("rsvpSource")
        val multiEventCities = sqlContext.sql(
          """ SELECT group["groupCountry"], group["groupState"], group["groupCity"], COUNT(DISTINCT member["memberId"]) as unique_member_count
            | FROM
            | rsvpSource
            | GROUP BY group["groupCountry"], group["groupState"], group["groupCity"]
          """.stripMargin)

        val result = multiEventCities
          .map { r => (r.getString(0), r.getString(1), r.getString(2), r.getLong(3)) }

        result
      }.print(100)
      // Example 2 - End


      /* Uncomment this section for all following examples
      val projections = rsvpStream.flatMap { rsvp =>
        val (country, state, city, memberId) = (rsvp.group.groupCountry.getOrElse(""), rsvp.group.groupState.getOrElse(""), rsvp.group.groupCity.getOrElse(""), rsvp.member.memberId.getOrElse(""))
        if ((country.isEmpty && state.isEmpty && city.isEmpty) || memberId.isEmpty) None else Some ((country, state, city), memberId)
      }
      */

      /*
      // Example 3 - Start (Stateful operation with updateStateByKey)
      val stateStream = projections.updateStateByKey { (newItems, currentState: Option[Set[String]]) =>
        val currentMemberSet = currentState.getOrElse(Set())
        val newMemberSet = currentMemberSet ++ newItems
        if (newMemberSet.nonEmpty) Some(newMemberSet) else None
      }

      stateStream.print()
      // Example 3 - End
      */

      /*
      // Example 4 - Start (Using mapWithState)
      def trackStateFunc = (k: (String, String, String), v: Option[String], state: State[Set[String]]) => {
        val currentMemberSet = state.getOption().getOrElse(Set())
        val newMemberSet = v match {
          case Some(memberId) => currentMemberSet + memberId
          case None => currentMemberSet
        }
        state.update(newMemberSet)
        val output = (k, newMemberSet.size)
        Some(output)
      }

      val uniqueMembersStateSpec = StateSpec
        .function(trackStateFunc)
        .timeout(Minutes(60))

      val memberStateStream = projections.mapWithState(uniqueMembersStateSpec)

      // memberStateStream contains same number of records as the input stream at this batch
      memberStateStream.print

      // let's look at a snapshot of the current state as of this batch
      val stateSnapshotStream = memberStateStream.stateSnapshots()
      stateSnapshotStream.print
      // Example 4 - End
      */

      ssc.remember(Minutes(60))

      ssc
    }

    val ssc = getStreamingContext(streamingFunc)
    ssc.start()
    ssc.awaitTermination()
  }
}
