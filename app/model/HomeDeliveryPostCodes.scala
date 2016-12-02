package model

object HomeDeliveryPostCodes {

  val availableDistricts = Set(
    "BR1", "BR2", "BR3", "BR4", "BR5", "BR6", "BR7", "BR8",
    "CM14",
    "CR0", "CR2", "CR3", "CR4", "CR5", "CR6", "CR7", "CR8", "CR9",
    "DA1", "DA5", "DA6", "DA7", "DA8",
    "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9",
    "EC1", "EC2", "EC3", "EC4",
    "EN1", "EN2", "EN3", "EN4", "EN5",
    "HA0", "HA1", "HA2", "HA3", "HA4", "HA5", "HA6", "HA7", "HA8", "HA9",
    "IG1", "IG2", "IG3", "IG5", "IG6", "IG7", "IG8", "IG9",
    "KT1", "KT2", "KT3", "KT4", "KT5", "KT6", "KT7", "KT8", "KT9",
    "N1", "N2", "N3", "N4", "N5", "N6", "N7", "N8", "N9",
    "NW1", "NW2", "NW3", "NW4", "NW5", "NW6", "NW7", "NW8", "NW9",
    "RM1", "RM2", "RM3", "RM4", "RM5", "RM6", "RM7", "RM8", "RM9",
    "SE1", "SE2", "SE3", "SE4", "SE5", "SE6", "SE7", "SE8", "SE9",
    "SM1", "SM2", "SM3", "SM4", "SM5", "SM6", "SM7",
    "SW1", "SW2", "SW3", "SW4", "SW5", "SW6", "SW7", "SW8", "SW9",
    "TN16",
    "TW1", "TW2", "TW3", "TW4", "TW5", "TW6", "TW7", "TW8", "TW9",
    "UB10", "UB3", "UB4", "UB5", "UB6",
    "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8", "W9",
    "WC1A", "WC2",
    "WD1", "WD2", "WD3", "WD4", "WD5", "WD6", "WD7"
  )

  val districtLengths = availableDistricts.map(_.length)
  val minDistrictLength = districtLengths.min
  val maxDistrictLength = districtLengths.max

  def getAvailableDistricts(postCode: String): Set[String] = {
    val sanitised = postCode.filter(_.isLetterOrDigit).take(maxDistrictLength).toUpperCase
    if (sanitised.length < minDistrictLength) Set.empty else availableDistricts.filter(d => d.startsWith(sanitised) || sanitised.startsWith(d))
  }

}
