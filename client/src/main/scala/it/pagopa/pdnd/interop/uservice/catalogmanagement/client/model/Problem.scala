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

import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.invoker.ApiModel

case class Problem (
  /* A human readable explanation specific to this occurrence of the problem. */
  detail: Option[String] = None,
  /* The HTTP status code generated by the origin server for this occurrence of the problem. */
  status: Int,
  /* A short, summary of the problem type. Written in english and readable */
  title: String
) extends ApiModel

