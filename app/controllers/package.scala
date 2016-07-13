import com.gu.subscriptions.{DigipackCatalog, PaperCatalog}
import services.TouchpointBackend

package object controllers {
  trait CatalogProvider {
    def catalog(implicit touchpointBackend: TouchpointBackend): DigipackCatalog =
      touchpointBackend.catalogService.digipackCatalog
    def catalogue(implicit touchpointBackend: TouchpointBackend): PaperCatalog =
      touchpointBackend.catalogService.paperCatalog
  }
}
