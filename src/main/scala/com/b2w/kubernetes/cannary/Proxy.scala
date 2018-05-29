package com.b2w.kubernetes.cannary

import akka.actor.ActorSystem
import akka.http.javadsl.server.RouteResult
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.http.scaladsl.server.RequestContext

import scala.concurrent.{ExecutionContextExecutor, Future}


object Proxy extends App {
  implicit val system: ActorSystem = ActorSystem("Proxy")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val source_port:Integer = Integer.valueOf(System.getenv("SOURCE_PORT"))
  val source_host:String = System.getenv("SOURCE_HOST")

  val destination_port_stable:Int = System.getenv("DESTINATION_PORT_STABLE").toInt
  val destination_port_cannary:Int = System.getenv("DESTINATION_PORT_CANNARY").toInt

  val destination_host:String = System.getenv("DESTINATION_HOST")

  val proxy = Route { context:RequestContext =>
    val request = context.request
    val mapHeaders:Map[String, String]
          = request.headers.map( h => h.name() -> h.value()).toMap

    mapHeaders.getOrElse("version", None) match {
      case "stable" =>
          doRoute(context, destination_host, destination_port_stable)
      case "cannary" =>
          doRoute(context, destination_host, destination_port_cannary)
      case _ =>
          doRoute(context, destination_host, destination_port_stable)
    }
  }

  def doRoute(context:RequestContext, host:String, port:Int) = {
    val flow = Http(system).outgoingConnection(host, port)
    Source.single(context.request)
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))
  }

  val binding: Future[Http.ServerBinding] =
    Http(system).bindAndHandle(handler = proxy, interface = source_host, port = source_port)
}
