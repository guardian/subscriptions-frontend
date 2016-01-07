import com.gu.subscriptions.DigipackCatalog
import services.TouchpointBackend

package object controllers {
  trait CatalogProvider {
    def catalog(implicit touchpointBackend: TouchpointBackend): DigipackCatalog =
      touchpointBackend.catalogService.digipackCatalog
  }
}
