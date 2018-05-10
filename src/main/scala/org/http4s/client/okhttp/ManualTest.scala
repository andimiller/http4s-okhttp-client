package org.http4s.client.okhttp

import cats.effect.IO
import fs2.StreamApp
import fs2.StreamApp.ExitCode
import org.http4s.{Method, Request, Response, Uri}
import fs2._
import org.http4s._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object ManualTest extends StreamApp[IO] {
  override def stream(
      args: List[String],
      requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    for {
      c <- OkHttp.stream[IO]()
      result <- Stream.eval(
        c.fetch(
          Request[IO](Method.GET,
                      Uri.unsafeFromString("http://httpbin.org/get")))(IO.pure))
      _ = println(result.status)
      body <- Stream.eval(result.as[String])
      _ = println(body)
      _ <- Stream.eval(c.shutdown)
    } yield (ExitCode.Success)
  }
}
