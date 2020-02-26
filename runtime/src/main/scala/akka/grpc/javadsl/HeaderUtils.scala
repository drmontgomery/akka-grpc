/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.javadsl

import java.lang.Iterable

import scala.collection.JavaConverters._

import io.grpc.Status

import akka.http.javadsl.model.HttpHeader
import akka.util.ByteString
import akka.grpc.scaladsl.{ HeaderUtils => sHeaderUtils }

object HeaderUtils {
  def binaryHeader(key: String, value: ByteString): HttpHeader =
    sHeaderUtils.binaryHeader(key, value)

  def textHeader(key: String, value: String): HttpHeader =
    sHeaderUtils.textHeader(key, value)

  def statusHeaders(status: Status): Iterable[HttpHeader] =
    sHeaderUtils.statusHeaders(status).map(_.asInstanceOf[HttpHeader]).asJava

  def encodeBinary(b: ByteString): String =
    sHeaderUtils.encodeBinary(b)

  def decodeBinary(s: String): ByteString =
    sHeaderUtils.decodeBinary(s)
}
