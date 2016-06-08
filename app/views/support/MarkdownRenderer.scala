package views.support
import org.pegdown.Extensions._
import org.pegdown.{LinkRenderer, PegDownProcessor}
import org.pegdown.LinkRenderer.Rendering
import org.pegdown.ast.{AnchorLinkNode, ExpLinkNode, MailLinkNode}

trait MarkdownRenderer {
  def render(in: String): String
}

object PegdownMarkdownRenderer extends MarkdownRenderer {

  val linkRenderer = new LinkRenderer {
    def addAttrs(in: Rendering): Rendering = in.withAttribute("class", "u-link").withAttribute("target", "_blank")
    override def render(node: ExpLinkNode, text: String): LinkRenderer.Rendering = addAttrs(super.render(node, text))
    override def render(node: AnchorLinkNode): LinkRenderer.Rendering = addAttrs(super.render(node))
    override def render(node: MailLinkNode): LinkRenderer.Rendering = addAttrs(super.render(node))
  }

  val pegdown = new PegDownProcessor(SUPPRESS_ALL_HTML)
  def render(in: String): String = pegdown.markdownToHtml(in, linkRenderer)
}
