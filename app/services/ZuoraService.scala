package services

import com.gu.membership.salesforce.Authentication
import touchpoint.ZuoraApiConfig

import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class ZuoraService(val apiConfig: ZuoraApiConfig) {

  val authTask =  ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes)(Future(Authentication("", "")))


  def start() {
    authTask.start()
  }

  implicit def authentication: Authentication = authTask.get()

}
