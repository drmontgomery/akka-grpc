/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.internal

import io.grpc.Status
import akka.NotUsed
import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.grpc.{ Codec, Grpc, ProtobufSerializer }
import akka.grpc.javadsl.{ GrpcServiceException => jGrpcServiceException }
import akka.grpc.scaladsl.{
  GrpcErrorResponse,
  GrpcExceptionHandler,
  headers,
  GrpcServiceException => sGrpcServiceException
}
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
      eHandler: ActorSystem => PartialFunction[Throwable, GrpcErrorResponse])(
      implicit m: ProtobufSerializer[T],
      mat: Materializer,
      codec: Codec,
      system: ActorSystem): HttpEntity.Chunked = {
    HttpEntity.Chunked(Grpc.contentType, chunks(e, trail).recover {
      case t =>
        val e = eHandler(system).orElse[Throwable, GrpcErrorResponse] {
          case e: sGrpcServiceException => GrpcErrorResponse(e.status, e.headers)
          case e: jGrpcServiceException => GrpcErrorResponse(e.status, GrpcExceptionHelper.asScala(e.headers))
          case e: Exception             => GrpcErrorResponse(Status.UNKNOWN.withCause(e).withDescription("Stream failed"))
        }(t)
        trailer(e.status, e.headers)
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

  def trailer(status: Status, headers: Seq[HttpHeader] = Nil): LastChunk =
    LastChunk(trailer = statusHeaders(status) ++ headers)

  def statusHeaders(status: Status): List[HttpHeader] =
    List(headers.`Status`(status.getCode.value.toString)) ++ Option(status.getDescription).map(d =>
      headers.`Status-Message`(d))
}