package acceptance.util

import java.security.cert.X509Certificate
import javax.net.ssl.{X509TrustManager, SSLContext, SSLSession, HostnameVerifier}
import com.madgag.okhttpscala._
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request.Builder
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try
import scala.language.postfixOps

object Dependencies {
  object SubscriptionFrontend {
    val subsFeRequest = new Builder().url(Config.baseUrl).build()

    def isAvailable: Boolean = {
      Try(Await.result(insecureClient.execute(subsFeRequest), 30 second).isSuccessful).getOrElse(false)
    }
  }

  object IdentityFrontend {
    val subsFeRequest = new Builder().url(Config.identityFrontendUrl).build()

    def isAvailable: Boolean = {
      Try(Await.result(insecureClient.execute(subsFeRequest), 30 second).isSuccessful).getOrElse(false)
    }
  }

  private val client = new OkHttpClient()
  private val insecureClient = InsecureOkHttpClient()

 /*
  * Get OkHttpClient which ignores all SSL errors.
  *
  * Needed when running against local servers which might not have a valid cert.
  * https://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
  */
  private object InsecureOkHttpClient {
   def apply() =
     new OkHttpClient().setSslSocketFactory(SSL.InsecureSocketFactory).setHostnameVerifier(new HostnameVerifier {
       override def verify(hostname: String, sslSession: SSLSession): Boolean = true
     })

    private object SSL {
      val InsecureSocketFactory = {
        val sslcontext = SSLContext.getInstance("TLS")
        sslcontext.init(null, Array(TrustEveryoneTrustManager), null)
        sslcontext.getSocketFactory
      }

      object TrustEveryoneTrustManager extends X509TrustManager {
        def checkClientTrusted(chain: Array[X509Certificate], authType: String) {}

        def checkServerTrusted(chain: Array[X509Certificate], authType: String) {}

        val getAcceptedIssuers = new Array[X509Certificate](0)
      }
    }
  }
}
