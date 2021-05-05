package com.gu.acquisition

package object instances {

  object all extends AbTestInstances
    with AbTestInfoInstances
    with AcquisitionInstances
    with AcquisitionSourceInstances
    with ComponentTypeInstances
    with PaymentFrequencyInstances
    with PaymentProviderInstances
    with QueryParameterInstances

  object abTest extends AbTestInstances
  object abTestInfo extends AbTestInfoInstances
  object acquisition extends AcquisitionInstances
  object acquisitionSource extends AcquisitionSourceInstances
  object componentType extends ComponentTypeInstances
  object paymentFrequency extends PaymentFrequencyInstances
  object paymentProvider extends PaymentProviderInstances
  object queryParamter extends QueryParameterInstances
}
