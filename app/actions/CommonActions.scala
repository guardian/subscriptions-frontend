package actions

 import controllers.{Cached, NoCache}
 import play.api.mvc._
 import play.filters.csrf.CSRFCheck

 import scala.concurrent.{ExecutionContext, Future}
final class CommonActions(executionContext: ExecutionContext, cSRFCheck: CSRFCheck, parser: BodyParser[AnyContent]) {

  private implicit val executionContextImplicit = executionContext

  val StoreAcquisitionDataAction = new ActionBuilder[Request, AnyContent] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(result => {
      request.getQueryString("acquisitionData").fold(result)(a => {
        val sessionWithAcquisitionData = result.session(request).data.toSeq ++ Seq("acquisitionData" -> a)
        result.withSession(sessionWithAcquisitionData: _*)
      })
    })
    override def parser = CommonActions.this.parser

    override protected def executionContext: ExecutionContext = CommonActions.this.executionContext
  }

  val NoCacheAction = StoreAcquisitionDataAction andThen resultModifier(noCache)

  val CachedAction = resultModifier(Cached(_))

  def noCache(result: Result): Result = NoCache(result)

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request, AnyContent] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)

    override def parser = CommonActions.this.parser

    override protected def executionContext: ExecutionContext = CommonActions.this.executionContext
  }
}
