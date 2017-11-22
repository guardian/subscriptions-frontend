package model.promoCodes

sealed trait PromoCodeKey

case object Digital extends PromoCodeKey

case object Paper extends PromoCodeKey

case object PaperAndDigital extends PromoCodeKey

case object GuardianWeekly extends PromoCodeKey
