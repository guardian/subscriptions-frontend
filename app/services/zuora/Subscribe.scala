package services.zuora

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.Countries
import com.gu.membership.zuora.soap.Zuora.SubscribeResult
import com.gu.membership.zuora.soap.ZuoraAction
import com.gu.membership.zuora.soap.ZuoraServiceHelpers._
import configuration.Config
import model.SubscriptionData
import org.joda.time.DateTime

import scala.xml.Elem

case class Subscribe(memberId: MemberId, data: SubscriptionData, productRatePlanId: String) extends ZuoraAction[SubscribeResult] {
  override protected val body: Elem = {
    lazy val paymentDelay = Some(Config.Zuora.paymentDelay)
    val now = DateTime.now
    val effectiveDate = formatDateTime(now)
    val contractAcceptanceDate = paymentDelay.map(delay => formatDateTime(now.plus(delay))).getOrElse(effectiveDate)

    val payment =
      <ns1:PaymentMethod xsi:type="ns2:PaymentMethod">
        <ns2:Type>BankTransfer</ns2:Type>
        <ns2:BankTransferType>DirectDebitUK</ns2:BankTransferType>
        <ns2:Country>{Countries.UK.alpha2}</ns2:Country>
        <ns2:BankTransferAccountName>{data.paymentData.holder}</ns2:BankTransferAccountName>
        <ns2:BankTransferAccountNumber>{data.paymentData.account}</ns2:BankTransferAccountNumber>
        <ns2:BankCode>{data.paymentData.sortCode}</ns2:BankCode>
        <ns2:FirstName>{data.personalData.firstName}</ns2:FirstName>
        <ns2:LastName>{data.personalData.lastName}</ns2:LastName>
      </ns1:PaymentMethod>


    // NOTE: This appears to be white-space senstive in some way. Zuora rejected
    // the XML after Intellij auto-reformatted the code.
    <ns1:subscribe>
      <ns1:subscribes>
        <ns1:Account xsi:type="ns2:Account">
          <ns2:AutoPay>true</ns2:AutoPay>
          <ns2:BcdSettingOption>AutoSet</ns2:BcdSettingOption>
          <ns2:BillCycleDay>0</ns2:BillCycleDay>
          <ns2:Currency>GBP</ns2:Currency>
          <ns2:Name>{memberId.salesforceAccountId}</ns2:Name>
          <ns2:PaymentTerm>Due Upon Receipt</ns2:PaymentTerm>
          <ns2:Batch>Batch1</ns2:Batch>
          <ns2:CrmId>{memberId.salesforceAccountId}</ns2:CrmId>
          <ns2:sfContactId__c>{memberId.salesforceContactId}</ns2:sfContactId__c>
          <ns2:PaymentGateway>GoCardless - Zuora Instance</ns2:PaymentGateway>
        </ns1:Account>
        {payment}
        <ns1:BillToContact xsi:type="ns2:Contact">
          <ns2:FirstName>{data.personalData.firstName}</ns2:FirstName>
          <ns2:LastName>{data.personalData.lastName}</ns2:LastName>
          <ns2:Address1>{data.personalData.address.address1}</ns2:Address1>
          <ns2:Address2>{data.personalData.address.address2}</ns2:Address2>
          <ns2:City>{data.personalData.address.town}</ns2:City>
          <ns2:PostalCode>{data.personalData.address.postcode}</ns2:PostalCode>
          <ns2:Country>{Countries.UK.alpha2}</ns2:Country>
        </ns1:BillToContact>
        <ns1:PreviewOptions>
          <ns1:EnablePreviewMode>false</ns1:EnablePreviewMode>
          <ns1:NumberOfPeriods>1</ns1:NumberOfPeriods>
        </ns1:PreviewOptions>
        <ns1:SubscriptionData>
          <ns1:Subscription xsi:type="ns2:Subscription">
            <ns2:AutoRenew>true</ns2:AutoRenew>
            <ns2:ContractEffectiveDate>{effectiveDate}</ns2:ContractEffectiveDate>
            <ns2:ContractAcceptanceDate>{contractAcceptanceDate}</ns2:ContractAcceptanceDate>
            <ns2:InitialTerm>12</ns2:InitialTerm>
            <ns2:RenewalTerm>12</ns2:RenewalTerm>
            <ns2:TermStartDate>{effectiveDate}</ns2:TermStartDate>
            <ns2:TermType>TERMED</ns2:TermType>
          </ns1:Subscription>
          <ns1:RatePlanData>
            <ns1:RatePlan xsi:type="ns2:RatePlan">
              <ns2:ProductRatePlanId>{productRatePlanId}</ns2:ProductRatePlanId>
            </ns1:RatePlan>
          </ns1:RatePlanData>
        </ns1:SubscriptionData>
      </ns1:subscribes>
    </ns1:subscribe>
  }
}
