package services.zuora

import com.gu.membership.zuora.Zuora.{Authentication, QueryResult, ZuoraResult}
import touchpoint.ZuoraApiConfig

import scala.xml.Elem

//TODO move to mem-common?
trait ZuoraAction[T <: ZuoraResult] {

  protected val body: Elem

  val authRequired = true
  val singleTransaction = false


  // The .toString is necessary because Zuora doesn't like Content-Type application/xml
  // which Play automatically adds if you pass it Elems
  def xml(implicit authentication: Authentication) = {
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.zuora.com/"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://api.zuora.com/"
                      xmlns:ns2="http://object.api.zuora.com/">
      <soapenv:Header>
        {if (authRequired) {
        <ns1:SessionHeader>
          <ns1:session>
            {authentication.token}
          </ns1:session>
        </ns1:SessionHeader>
      }}
        {
        if (singleTransaction) {
          <ns1:CallOptions>
            <ns1:useSingleTransaction>true</ns1:useSingleTransaction>
          </ns1:CallOptions>
        }
        }
      </soapenv:Header>
      <soapenv:Body>{body}</soapenv:Body>
    </soapenv:Envelope>.toString()
  }

  def sanitized = body.toString()
}

case class Query(query: String) extends ZuoraAction[QueryResult] {
  val body =
    <ns1:query>
      <ns1:queryString>{query}</ns1:queryString>
    </ns1:query>
}
case class Login(apiConfig: ZuoraApiConfig) extends ZuoraAction[Authentication] {
  override val authRequired = false
  val body =
    <api:login>
      <api:username>{apiConfig.username}</api:username>
      <api:password>{apiConfig.password}</api:password>
    </api:login>
  override def sanitized = "<api:login>...</api:login>"
}