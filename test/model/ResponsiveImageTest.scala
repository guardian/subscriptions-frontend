package model

import play.api.test.PlaySpecification

class ResponsiveImageTest extends PlaySpecification {

  val largeImage = "https://media.guim.co.uk/bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683/1000.jpg"
  val mediumImage = "https://media.guim.co.uk/bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683/500.jpg"
  val smallImage = "https://media.guim.co.uk/bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683/140.jpg"

  "ResponsiveImageGroup" should {

    "generate a src and srcset information for a sequence of images" in {

      val imageGroup = ResponsiveImageGroup(
        altText=None,
        availableImages = Seq(
          ResponsiveImage(
            path=largeImage,
            width=1000
          ),
          ResponsiveImage(
            path=mediumImage,
            width=500
          ),
          ResponsiveImage(
            path=smallImage,
            width=140
          )
        )
      )

      imageGroup.smallestImage mustEqual smallImage
      imageGroup.defaultImage mustEqual mediumImage
      imageGroup.srcset mustEqual List(
        smallImage + " 140w",
        mediumImage + " 500w",
        largeImage + " 1000w"
      ).mkString(", ")

    }

  }

  "ResponsiveImageGenerator" should {

    "generate a src and srcset information for a generated image group" in {

      val imageGroup = ResponsiveImageGroup(
        name=None,
        altText=None,
        availableImages=ResponsiveImageGenerator(
          id="bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683",
          sizes=List(1000,500,140)
        )
      )

      imageGroup.smallestImage mustEqual smallImage
      imageGroup.defaultImage mustEqual mediumImage
      imageGroup.srcset mustEqual List(
        smallImage + " 140w",
        mediumImage + " 500w",
        largeImage + " 1000w"
      ).mkString(", ")

    }

  }

}
