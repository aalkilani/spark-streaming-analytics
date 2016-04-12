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
import java.util.Properties

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer._

object KafkaProducerActor {
  def props(topic: String) : Props = {
    Props(new KafkaProducerActor(topic))
  }
}
class KafkaProducerActor(topic: String) extends Actor {

  var producer : Producer[Nothing, String] = _

  override def preStart: Unit = {

    val props = new Properties()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducerActor") // perhaps set client ID as the path of the calling actor

    producer = new KafkaProducer[Nothing, String](props)
  }

  override def receive: Receive = {
    case data: String =>
      val datum = new ProducerRecord(topic, data)
      producer.send(datum, new Callback {
        override def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
          if (e != null)
            println(e.getMessage)
          println(s"Partition: ${recordMetadata.partition()} Offset: ${recordMetadata.offset()}")
        }
      })
  }
}
