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
  *
  * Attribution: Code within MeetupReceiverActor class primarily reused from: https://github.com/actions/meetup-stream/blob/master/src/main/scala/receiver/MeetupReceiver.scala
  */
import akka.actor.{Actor, ActorRef, Props}
import com.ning.http.client._

/**
  * Inspired by meetup-stream Spark package https://github.com/actions/meetup-stream
  */

object MeetupReceiverActor {
  case class Start()

  def props(url: String, dataConsumer: ActorRef): Props = {
    Props(new MeetupReceiverActor(url, dataConsumer))
  }
}

class MeetupReceiverActor(url: String, dataConsumer: ActorRef) extends Actor {
  /*
  Attribution: The majority of this class was reused from work here: https://github.com/actions/meetup-stream
   */
  @transient var client: AsyncHttpClient = _
  @transient var builder: StringBuilder = _

  override def preStart(): Unit = {
    val cf = new AsyncHttpClientConfig.Builder()
    cf.setRequestTimeout(Integer.MAX_VALUE)
    cf.setReadTimeout(Integer.MAX_VALUE)
    cf.setPooledConnectionIdleTimeout(Integer.MAX_VALUE)
    client = new AsyncHttpClient(cf.build())
  }

  override def receive: Receive = {
    case MeetupReceiverActor.Start =>
      client.prepareGet(url).execute(new AsyncHandler[Unit]{

        def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
          val rsvp = new String(bodyPart.getBodyPartBytes)
          dataConsumer forward rsvp
          AsyncHandler.STATE.CONTINUE
        }

        def onStatusReceived(status: HttpResponseStatus) = {
          AsyncHandler.STATE.CONTINUE
        }

        def onHeadersReceived(headers: HttpResponseHeaders) = {
          AsyncHandler.STATE.CONTINUE
        }

        def onCompleted = {
          println("completed")
        }

        def onThrowable(t: Throwable)={
          t.printStackTrace()
        }

      })
  }
}
