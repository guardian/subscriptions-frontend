package acceptance.util

import org.scalatestplus.selenium.Page


trait LoadablePage extends Page {

  /**
    * Page has loaded if the business critical element is present on the page.
    *
    * Do not relay on the 'load' event to determine if the page has loaded.
    * Instead provide context dependant implementation of determining if the page
    * has loaded by checking for the presence of a business critical element.
    *
    * @return business critical element is present on the page
    */
  def hasLoaded(): Boolean

}
