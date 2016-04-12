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

import akka.actor._

object Main {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("meetup")

    val kafkaProducer = system.actorOf(KafkaProducerActor.props("meetup_rsvps"), "kafkaProducer")
    val receiver = system.actorOf(MeetupReceiverActor.props("http://stream.meetup.com/2/rsvps", kafkaProducer), "meetupReceiver")
    system.actorOf(Terminator.props(receiver), "terminator")

    receiver ! MeetupReceiverActor.Start
  }

  object Terminator {
    def props(ref: ActorRef) = {
      Props(new Terminator(ref))
    }
  }
  class Terminator(ref: ActorRef) extends  Actor with ActorLogging {
    context watch ref
    override def receive: Receive = {
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", ref.path)
        context.system.terminate()
    }
  }

}
