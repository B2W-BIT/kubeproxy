package com.b2w.kubernetes.cannary.kube_integration

import io.kubernetes.client.ApiClient
import io.kubernetes.client.Configuration
import io.kubernetes.client.apis.CoreV1Api
import io.kubernetes.client.util.Config

class ServicesClient(namespace_name: String, kubernetesLikeDomain:Boolean = false) {

  val client:ApiClient = Config.defaultClient()
  Configuration.setDefaultApiClient(client)

  val api:CoreV1Api = new CoreV1Api()
  api.setApiClient(api.getApiClient.setVerifyingSsl(false))

  val destination_port_stable:Int = System.getenv("DESTINATION_PORT_STABLE").toInt
  val destination_port_cannary:Int = System.getenv("DESTINATION_PORT_CANNARY").toInt

  val destination_host_stable:String = System.getenv("DESTINATION_HOST_STABLE")
  val destination_host_cannary:String = System.getenv("DESTINATION_HOST_CANNARY")

  def destinationPortStable():Int = destination_port_stable

  def destinationPortCannary():Int = destination_port_cannary

  def destinationHostStable(stableSelector:String):String =
    if(kubernetesLikeDomain) s"$destination_host_stable.$namespace_name" else destination_host_stable

  def destinationHostCannary():String =
    if(kubernetesLikeDomain) s"$destination_host_cannary.$namespace_name" else destination_host_cannary

}
