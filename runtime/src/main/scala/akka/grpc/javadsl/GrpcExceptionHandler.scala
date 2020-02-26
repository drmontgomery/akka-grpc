/*
 * Copyright (C) 2018-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.grpc.javadsl

import java.lang.Iterable
import java.util.concurrent.CompletionException

import io.grpc.Status
import scala.concurrent.ExecutionException

import akka.actor.ActorSystem
import akka.grpc.GrpcServiceException
import akka.grpc.scaladsl.{ HeaderUtils => sHeaderUtils }
import akka.grpc.internal.{ GrpcExceptionHelper, MissingParameterException }
import akka.http.javadsl.model.{ HttpHeader, HttpResponse }
import akka.japi.{ Function => jFunction }

object GrpcExceptionHandler {
  private val INVALID_ARGUMENT = HeaderUtils.statusHeaders(Status.INVALID_ARGUMENT)
  private val INTERNAL = HeaderUtils.statusHeaders(Status.INTERNAL)
  private val UNIMPLEMENTED = HeaderUtils.statusHeaders(Status.UNIMPLEMENTED)

  def defaultMapper: jFunction[ActorSystem, jFunction[Throwable, Iterable[HttpHeader]]] =
    new jFunction[ActorSystem, jFunction[Throwable, Iterable[HttpHeader]]] {
      override def apply(system: ActorSystem): jFunction[Throwable, Iterable[HttpHeader]] =
        default(system)
    }

  def default(system: ActorSystem): jFunction[Throwable, Iterable[HttpHeader]] =
    new jFunction[Throwable, Iterable[HttpHeader]] {
      override def apply(param: Throwable): Iterable[HttpHeader] = param match {
        case e: ExecutionException =>
          if (e.getCause == null) INTERNAL
          else default(system)(e.getCause)
        case e: CompletionException =>
          if (e.getCause == null) INTERNAL
          else default(system)(e.getCause)
        case grpcException: GrpcServiceException =>
          GrpcExceptionHelper.asJava(sHeaderUtils.statusHeaders(grpcException.status) ++ grpcException.headers)
        case _: MissingParameterException     => INVALID_ARGUMENT
        case _: NotImplementedError           => UNIMPLEMENTED
        case _: UnsupportedOperationException => UNIMPLEMENTED
        case other =>
          system.log.error(other, "Unhandled error: [" + other.getMessage + "]")
          INTERNAL
      }
    }

  def standard(t: Throwable, system: ActorSystem): HttpResponse = standard(t, defaultMapper, system)

  def standard(
      t: Throwable,
      mapper: jFunction[ActorSystem, jFunction[Throwable, Iterable[HttpHeader]]],
      system: ActorSystem): HttpResponse =
    GrpcMarshalling.status(mapper(system)(t))
}
