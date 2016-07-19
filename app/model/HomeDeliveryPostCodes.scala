package model

import java.lang.Math.min

object HomeDeliveryPostCodes {
  private val London = Set(
    "E1", "EC1", "N1", "NW1", "SE1", "SW1", "W1", "WC1",
    "E10", "EC2", "N10", "NW10", "SE10", "SW10", "W10", "WC2",
    "E11", "EC3", "N11", "NW11", "SE11", "SW11", "W11",
    "E12", "EC4", "N12", "NW2", "SE12", "SW12", "W12",
    "E13", "N13", "NW3", "SE13", "SW13", "W13",
    "E14", "N14", "NW4", "SE14", "SW14", "W14",
    "E15", "N15", "NW5", "SE15", "SW15", "W2",
    "E16", "N16", "NW6", "SE16", "SW15", "W3",
    "E17", "N17", "NW7", "SE17", "SW16", "W4",
    "E18", "N18", "NW8", "SE18", "SW17", "W5",
    "E1W", "N19", "NW9", "SE19", "SW18", "W6",
    "E2", "N2", "SE2", "SW19", "W7",
    "E3", "N20", "SE20", "SW2", "W8",
    "E4", "N21", "SE21", "SW20", "W9",
    "E5", "N22", "SE22", "SW3",
    "E6", "N3", "SE23", "SW4",
    "E7", "N4", "SE24", "SW5",
    "E8", "N5", "SE25", "SW6",
    "E9", "N6", "SE26", "SW7",
    "E98", "N7", "SE27", "SW8",
    "N8", "SE28", "SW9",
    "N9", "SE3",
    "SE4",
    "SE5",
    "SE6",
    "SE7",
    "SE8",
    "SE9"
  )
  private val M25 = Set(
    "BR1", "CR0", "DA1", "EN1", "HA0", "IG1", "KT1", "RM1", "SM1", "TW1", "UB1", "WD1",
    "BR2", "CR2", "DA14", "EN2", "HA1", "IG10", "KT10", "RM10", "SM2", "TW10", "UB10", "WD17",
    "BR3", "CR3", "DA15", "EN3", "HA2", "IG11", "KT12", "RM11", "SM3", "TW11", "UB11", "WD18",
    "BR4", "CR4", "DA16", "EN4", "HA3", "IG2", "KT13", "RM12", "SM4", "TW12", "UB2", "WD19",
    "BR5", "CR5", "DA17", "EN5", "HA4", "IG3", "KT17", "RM13", "SM5", "TW13", "UB3", "WD23",
    "BR6", "CR6", "DA18", "HA5", "IG4", "KT19", "RM2", "SM6", "TW14", "UB4", "WD24",
    "BR7", "CR7", "DA5", "HA6", "IG5", "KT2", "RM3", "SM7", "TW15", "UB5", "WD25",
    "CR8", "DA6", "HA7", "IG6", "KT21", "RM4", "TW16", "UB6", "WD6",
    "CR9", "DA7", "HA8", "IG7", "KT3", "RM5", "TW17", "UB7", "WD7",
    "DA8", "HA9", "IG8", "KT4", "RM6", "TW18", "UB8",
    "IG9", "KT5", "RM7", "TW2", "UB9",
    "KT6", "RM8", "TW3",
    "KT7", "RM9", "TW4",
    "KT8", "TW5",
    "KT9", "TW6",
    "TW7",
    "TW8",
    "TW9"
  )
  private val Outer = Set(
    "AL1", "BR8", "EN6", "GU1", "HP1", "KT11", "OX1", "RG1", "RM14", "SL0", "TN1", "TW19", "WD3",
    "AL2", "GU14", "KT14", "OX2", "RG10", "SL1", "TN11", "TW20", "WD4",
    "AL3", "GU15", "HP11", "KT15", "OX3", "RG12", "SL2", "TN10", "WD5",
    "AL4", "GU16", "HP12", "KT16", "OX4", "SL3", "TN13",
    "GU18", "HP2", "KT18", "SL4", "TN14",
    "GU19", "HP3", "KT20", "RG40", "SL5", "TN16",
    "GU2", "HP5", "KT22", "RG41", "SL6", "TN4",
    "GU20", "HP6", "KT23", "RG42", "SL7", "TN9",
    "GU21", "HP7", "KT24", "SL8",
    "GU22", "HP8", "SL9",
    "GU23", "HP9", "RG9",
    "GU24",
    "GU25",
    "GU3",
    "GU4"
  )

  private val availableDistricts = London ++ M25 ++ Outer

  def findDistrict(postCode: String): Option[String] = {
    val sanitised = postCode.toUpperCase.trim
    if (sanitised.isEmpty) None else availableDistricts.find(d => d.startsWith(sanitised) || sanitised.startsWith(d))
  }

}
