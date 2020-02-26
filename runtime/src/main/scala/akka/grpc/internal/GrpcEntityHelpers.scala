/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.internal

import io.grpc.Status
import akka.NotUsed
import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.grpc.{ Codec, Grpc, GrpcServiceException, ProtobufSerializer }
import akka.grpc.scaladsl.HeaderUtils
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpEntity.LastChunk
import akka.http.scaladsl.model.HttpHeader
import akka.stream.Materializer
import akka.stream.scaladsl.Source

/** INTERNAL API */
@InternalApi
object GrpcEntityHelpers {
  def apply[T](
      e: Source[T, NotUsed],
      trail: Source[HttpEntity.LastChunk, NotUsed],
      eHandler: ActorSystem => PartialFunction[Throwable, List[HttpHeader]])(
      implicit m: ProtobufSerializer[T],
      mat: Materializer,
      codec: Codec,
      system: ActorSystem): HttpEntity.Chunked = {
    HttpEntity.Chunked(Grpc.contentType, chunks(e, trail).recover {
      case t =>
        val headers = eHandler(system).orElse[Throwable, List[HttpHeader]] {
          case e: GrpcServiceException => statusHeaders(e.status) ++ e.headers
          case e: Exception            => statusHeaders(Status.UNKNOWN.withCause(e).withDescription("Stream failed"))
        }(t)
        trailer(headers)
    })
  }

  def apply[T](e: T)(
      implicit m: ProtobufSerializer[T],
      mat: Materializer,
      codec: Codec,
      system: ActorSystem): HttpEntity.Chunked =
    HttpEntity.Chunked(Grpc.contentType, chunks(Source.single(e), Source.empty))

  private def chunks[T](e: Source[T, NotUsed], trail: Source[HttpEntity.LastChunk, NotUsed])(
      implicit m: ProtobufSerializer[T],
      mat: Materializer,
      codec: Codec,
      system: ActorSystem) =
    e.map(m.serialize).via(Grpc.grpcFramingEncoder(codec)).map(bytes => HttpEntity.Chunk(bytes)).concat(trail)

  def trailer(status: Status): LastChunk =
    trailer(statusHeaders(status))

  def trailer(headers: List[HttpHeader]): LastChunk =
    LastChunk(trailer = headers)

  def statusHeaders(status: Status): List[HttpHeader] =
    HeaderUtils.statusHeaders(status)
}
