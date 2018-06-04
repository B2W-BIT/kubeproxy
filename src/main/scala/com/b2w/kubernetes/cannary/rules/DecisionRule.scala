package com.b2w.kubernetes.cannary.rules

class DecisionRule() {
  var decision: (Map[String, String], Map[String,String]) => String = (headers:Map[String, String], query:Map[String, String]) => "stable"

  def loadDecision():Unit = {
    // Simula o carregamento de uma função, num script ou arquivo por exemplo
    this.decision = (_:Map[String, String], query:Map[String, String]) => query("version")
  }

  def decide(headers: Map[String, String], query: Map[String, String]):String = {
    loadDecision()
    try{
      if(headers.isEmpty || query.isEmpty )
        "empty"
      else
        decision(headers, query)
    }
    catch{
      case _: Throwable => "fail"
    }
  }
}
