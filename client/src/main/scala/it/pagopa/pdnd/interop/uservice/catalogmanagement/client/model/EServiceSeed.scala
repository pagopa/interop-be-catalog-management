/**
 * Catalog Management Micro Service
 * Service implementing the persistence of e-services
 *
 * The version of the OpenAPI document: {{version}}
 * Contact: support@example.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model

import java.util.UUID
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.invoker.ApiModel

case class EServiceSeed (
  producerId: UUID,
  name: String,
  description: String,
  technology: EServiceSeedEnums.Technology,
  attributes: Attributes
) extends ApiModel

object EServiceSeedEnums {

  type Technology = Technology.Value
  object Technology extends Enumeration {
    val REST = Value("REST")
    val SOAP = Value("SOAP")
  }

}