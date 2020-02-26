/*
 * Copyright (C) 2018-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.internal

import java.lang.{ Iterable => jIterable }

import scala.collection.JavaConverters._

import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.grpc.javadsl.{ scalaAnonymousPartialFunction }
import akka.http.javadsl.model.{ HttpHeader => jHttpHeader }
import akka.http.scaladsl.model.headers.{ RawHeader => sRawHeader }
import akka.http.scaladsl.model.{ HttpHeader => sHttpHeader }
import akka.japi.{ Function => jFunction }

@InternalApi
object GrpcExceptionHelper {
  def asScala(h: jHttpHeader): sHttpHeader = h match {
    case s: sHttpHeader => s
    case _              => sRawHeader(h.value, h.name)
  }

  def asScala(i: jIterable[jHttpHeader]): List[sHttpHeader] =
    i.asScala.map(asScala).toList

  def asScala(m: jFunction[ActorSystem, jFunction[Throwable, jIterable[jHttpHeader]]])
      : ActorSystem => PartialFunction[Throwable, List[sHttpHeader]] =
    scalaAnonymousPartialFunction(m).andThen(f => f.andThen(asScala _))

  def asJava(s: List[sHttpHeader]): jIterable[jHttpHeader] =
    s.map(_.asInstanceOf[jHttpHeader]).asJava
}
