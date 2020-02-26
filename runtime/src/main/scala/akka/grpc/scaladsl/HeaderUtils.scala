/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.scaladsl

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import akka.util.ByteString
import io.grpc.Status

object HeaderUtils {
  val BINARY_SUFFIX = "-bin"

  def binaryHeader(key: String, value: ByteString): HttpHeader = {
    require(key.endsWith(BINARY_SUFFIX))
    RawHeader(key, encodeBinary(value))
  }

  def textHeader(key: String, value: String): HttpHeader = {
    require(!key.endsWith(BINARY_SUFFIX))
    RawHeader(key, value)
  }

  def statusHeaders(status: Status): List[HttpHeader] =
    List(headers.`Status`(status.getCode.value.toString)) ++ Option(status.getDescription).map(d =>
      headers.`Status-Message`(d))

  def encodeBinary(b: ByteString): String =
    new String(Base64.getEncoder.encode(b.toByteBuffer).array, StandardCharsets.ISO_8859_1)

  def decodeBinary(s: String): ByteString =
    ByteString(Base64.getDecoder.decode(s))
}
