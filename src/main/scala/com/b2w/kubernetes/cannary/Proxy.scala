package com.b2w.kubernetes.cannary

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import kube_integration.ServicesClient
import rules.DecisionRule

import scala.concurrent.{ExecutionContextExecutor, Future}


object Proxy extends App {
  implicit val system: ActorSystem = ActorSystem("Proxy")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val source_port:Integer = Integer.valueOf(System.getenv("SOURCE_PORT"))
  val source_host:String = "0.0.0.0"

  val serviceClient = new ServicesClient("poc-andre")
  val rule = new DecisionRule()

  val proxy = Route { context:RequestContext =>
    val request = context.request
    val headers:Map[String, String]
          = request.headers.map( h => h.name() -> h.value()).toMap
    val query = request.uri.query().toMap

    rule.decide(headers, query) match {
      case "stable" =>
          doRoute(context, serviceClient.destination_host_stable, serviceClient.destination_port_stable)
      case "cannary" =>
          doRoute(context, serviceClient.destination_host_cannary, serviceClient.destination_port_cannary)
      case _ =>
        doRoute(context, serviceClient.destination_host_stable, serviceClient.destination_port_stable)

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
