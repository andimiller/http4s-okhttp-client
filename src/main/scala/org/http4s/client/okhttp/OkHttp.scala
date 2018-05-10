package org.http4s.client.okhttp

import java.io.IOException

import cats._
import cats.implicits._
import cats.data._
import cats.syntax._
import cats.effect._
import cats.effect.implicits._
import okhttp3.{
  Call,
  Callback,
  OkHttpClient,
  RequestBody,
  Headers => OKHeaders,
  MediaType => OKMediaType,
  Request => OKRequest,
  Response => OKResponse
}
import okio.BufferedSink
import org.http4s.{Header, Headers, MediaType, Request, Response, Status}
import org.http4s.client.{Client, DisposableResponse}
import fs2.Stream._
import fs2._
import fs2.io._
import fs2.interop.reactivestreams.{StreamSubscriber, StreamUnicastPublisher}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object OkHttp {

  def apply[F[_]]()(implicit F: Effect[F], ec: ExecutionContext): Client[F] = {
    val client = new OkHttpClient()
    Client(
      Kleisli { req =>
        F.async[DisposableResponse[F]] { cb =>
          client.newCall(toOkHttpRequest(req)).enqueue(handler(cb))
          ()
        }
      },
      F.delay({
        client.dispatcher.executorService().shutdown()
        client.connectionPool().evictAll()
        if (client.cache() != null) {
          client.cache().close()
        }
      })
    )
  }

  def stream[F[_]]()(implicit F: Effect[F],
                     ec: ExecutionContext): Stream[F, Client[F]] =
    Stream.bracket(F.delay(apply()))(c => Stream.emit(c), _.shutdown)

  def handler[F[_]](cb: Either[Throwable, DisposableResponse[F]] => Unit)(
      implicit F: Effect[F],
      ec: ExecutionContext) = {

    new Callback {
      override def onFailure(call: Call, e: IOException): Unit = {
        ec.execute(new Runnable { def run(): Unit = cb(Left(e)) })
      }

      override def onResponse(call: Call, response: OKResponse): Unit = {
        val dr = new DisposableResponse[F](
          Response[F](headers = getHeaders(response))
            .withStatus(Status.apply(response.code()))
            .withType(MediaType.fromKey(response.body.contentType().`type`(),
                                        response.body.contentType().subtype()))
            .withBodyStream(
              fs2.io.readInputStream(F.delay(response.body.byteStream()),
                                     1024,
                                     true)),
          F.delay()
        )
        ec.execute(new Runnable { def run(): Unit = cb(Right(dr)) })
      }
    }
  }

  def getHeaders(response: OKResponse): Headers = {
    Headers(response.headers().names().asScala.toList.flatMap { v =>
      response.headers().values(v).asScala.map(Header(v, _))
    })
  }

  def toOkHttpRequest[F[_]](req: Request[F])(
      implicit F: Effect[F]): OKRequest = {
    val body = req match {
      case _ if req.isChunked || req.contentLength.isDefined =>
        new RequestBody {
          override def contentType(): OKMediaType =
            req.contentType.map(c => OKMediaType.parse(c.toString())).orNull

          override def writeTo(sink: BufferedSink): Unit = {
            req.body.chunks
              .map(_.toArray)
              .to(Sink { b: Array[Byte] =>
                F.delay {
                  sink.write(b); ()
                }
              })
              .compile
              .drain
              .runAsync(_ => IO.unit)
              .unsafeRunSync()
          }
        }
      case _ => null
    }

    new OKRequest.Builder()
      .headers(OKHeaders.of(
        req.headers.toList.map(h => (h.name.value, h.value)).toMap.asJava))
      .method(req.method.toString(), body)
      .url(req.uri.toString())
      .build()
  }

}
