package it.pagopa.interop.catalogmanagement.model

sealed trait PersistentAgreementApprovalPolicy
case object Automatic extends PersistentAgreementApprovalPolicy
case object Manual    extends PersistentAgreementApprovalPolicy
object PersistentAgreementApprovalPolicy {
  val default: PersistentAgreementApprovalPolicy = Automatic
}
