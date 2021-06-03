package it.pagopa.pdnd.uservice.resttemplate.model.persistence.serializer

import it.pagopa.pdnd.interopuservice.agreementmanagement.model.EService
import it.pagopa.pdnd.uservice.resttemplate.model.persistence.serializer.v1.e_service.EServiceV1
import it.pagopa.pdnd.uservice.resttemplate.model.persistence.serializer.v1.events.EServiceAddedV1
import it.pagopa.pdnd.uservice.resttemplate.model.persistence.serializer.v1.state.{EServicesV1, StateV1}
import it.pagopa.pdnd.uservice.resttemplate.model.persistence.{EServiceAdded, State}

import java.util.UUID

package object v1 {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      val eServices = state.eServices
        .map(eServicesV1 =>
          (
            eServicesV1.key,
            EService(
              id = Some(UUID.fromString(eServicesV1.value.id)),
              name = eServicesV1.value.name,
              version = eServicesV1.value.version,
              docs = Some(eServicesV1.value.docs),
              description = eServicesV1.value.description,
              scopes = Some(eServicesV1.value.scopes),
              proposal = Some(eServicesV1.value.proposal)
            )
          )
        )
        .toMap
      Right(State(eServices))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.OptionPartial"))
  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      val eServices = state.eServices
      val eServicesV1 = eServices.map { case (key, eService) =>
        EServicesV1(
          key,
          EServiceV1(
            id = eService.id.get.toString,
            name = eService.name,
            version = eService.version,
            docs = eService.docs.get,
            description = eService.description,
            scopes = eService.scopes.get,
            proposal = eService.proposal.get
          )
        )
      }.toSeq
      Right(StateV1(eServicesV1))
    }

  implicit def eServiceAddedV1PersistEventDeserializer: PersistEventDeserializer[EServiceAddedV1, EServiceAdded] =
    event =>
      Right[Throwable, EServiceAdded](
        EServiceAdded(eService =
          EService(
            id = Some(UUID.fromString(event.eService.id)),
            name = event.eService.name,
            version = event.eService.version,
            docs = Some(event.eService.docs),
            description = event.eService.description,
            scopes = Some(event.eService.scopes),
            proposal = Some(event.eService.proposal)
          )
        )
      )

  implicit def eServiceAddedV1PersistEventSerializer: PersistEventSerializer[EServiceAdded, EServiceAddedV1] = event =>
    {
      for {
        id       <- event.eService.id
        docs     <- event.eService.docs
        scopes   <- event.eService.scopes
        proposal <- event.eService.proposal
      } yield EServiceAddedV1
        .of(
          EServiceV1(
            id = id.toString,
            name = event.eService.name,
            version = event.eService.version,
            docs = docs,
            description = event.eService.description,
            scopes = scopes,
            proposal = proposal
          )
        )
    }.toRight(new RuntimeException("Deserialization from protobuf failed"))

}
