package utils

import acquisitions.AcquisitionsHelper
import acquisitions.AcquisitionsHelper._
import org.scalatest.{FlatSpec, Matchers}

class AcquisitionsHelperTest extends FlatSpec with Matchers {

  val param = ".0ctCmYiGSGqiUFP0eMFobw.1"

  "An Optimize QueryParam" should "parse correctly" in {

    val test = abTestFromQueryParam(param)
    test.name shouldBe "optimize$$0ctCmYiGSGqiUFP0eMFobw"
    test.variant shouldBe "1"
  }

  "mergeAcquisitionData" should "merge existing acquisitionData with an Optimize param" in {
    val json =
      """
        |{"abTests":[{"name":"ssr","variant":"notintest"}]}
      """.stripMargin

    val mergedData = AcquisitionsHelper.mergeAcquisitionData(Some(json), Some(param) )

    mergedData.get.abTests.get.size shouldBe 2
  }

  it should "create the acquisitionData if none exists and an Optimize parameter is present" in {
    AcquisitionsHelper.mergeAcquisitionData(None, Some(param)).get.abTests.get.size shouldBe 1
  }

  it should "return None when acquisitionData and Optimize parameter are null" in  {
    AcquisitionsHelper.mergeAcquisitionData(None, None) shouldBe None
  }

  it should "return existing acquisitionData when Optimize parameter is null" in {
    val json =
      """
        |{
        |    "campaignCode": "gdnwb_copts_merchhgh_subscribe_DPHighSlotROW_component",
        |    "abTests": [
        |        {
        |            "name": "ssrTwo",
        |            "variant": "notintest"
        |        },
        |        {
        |            "name": "annualContributionsRoundFour",
        |            "variant": "lower"
        |        },
        |        {
        |            "name": "requiredFields",
        |            "variant": "variant"
        |        },
        |        {
        |            "name": "optimize$$I-M60BjwTLClJegHgJmklw",
        |            "variant": "0"
        |        }
        |    ]
        |}
      """.stripMargin
    val merged = AcquisitionsHelper.mergeAcquisitionData(Some(json), None)
    merged.get.abTests.get.size shouldBe 4
  }
}
