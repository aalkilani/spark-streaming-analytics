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
package Domain

object Meetup {

  case class Group(
                    groupId: Option[String],
                    groupCity: Option[String],
                    groupCountry: Option[String],
                    groupLat: Option[String],
                    groupLon: Option[String],
                    groupName: Option[String],
                    groupState: Option[String],
                    groupUrlname: Option[String]
                  )

  case class Event(
                    eventId: Option[String],
                    eventName: Option[String],
                    eventUrl: Option[String],
                    Time: Option[Long]
                  )

  case class Member(
                     memberId: Option[String],
                     memberName: Option[String]
                   )

  case class RSVP(event: Event, group: Group, guests: Int, member: Member, mtime: Long, response: String, rsvpId: String)

}
