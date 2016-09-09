import com.gu.memsub.subsv2.Catalog
import services.TouchpointBackend

package object controllers {
  trait CatalogProvider {
    def catalog(implicit touchpointBackend: TouchpointBackend): Catalog =
      touchpointBackend.catalogService.unsafeCatalog
  }
}
