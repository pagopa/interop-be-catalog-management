package it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.{EService, EServiceVersion}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.e_service.{
  EServiceV1,
  EServiceVersionV1
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.events.EServiceAddedV1
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.serializer.v1.state.{EServicesV1, StateV1}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.model.persistence.{EServiceAdded, State}

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
              producer = Some(UUID.fromString(eServicesV1.value.producer)),
              name = eServicesV1.value.name,
              description = eServicesV1.value.description,
              scopes = Some(eServicesV1.value.scopes),
              versions = Some(
                eServicesV1.value.versions.map(ver1 =>
                  EServiceVersion(
                    id = Some(UUID.fromString(ver1.id)),
                    version = ver1.version,
                    docs = ver1.docs,
                    proposal = Some(ver1.proposal)
                  )
                )
              ),
              status = Some(eServicesV1.value.status)
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
            producer = eService.producer.get.toString,
            name = eService.name,
            description = eService.description,
            scopes = eService.scopes.get,
            versions = eService.versions.get.map(ver =>
              EServiceVersionV1(
                id = ver.id.get.toString,
                version = ver.version,
                docs = ver.docs,
                proposal = ver.proposal.get
              )
            ),
            status = eService.status.get
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
            producer = Some(UUID.fromString(event.eService.producer)),
            name = event.eService.name,
            description = event.eService.description,
            scopes = Some(event.eService.scopes),
            versions = Some(
              event.eService.versions.map(ver1 =>
                EServiceVersion(
                  id = Some(UUID.fromString(ver1.id)),
                  version = ver1.version,
                  docs = ver1.docs,
                  proposal = Some(ver1.proposal)
                )
              )
            ),
            status = Some(event.eService.status)
          )
        )
      )

  implicit def eServiceAddedV1PersistEventSerializer: PersistEventSerializer[EServiceAdded, EServiceAddedV1] = event =>
    {
      for {
        id       <- event.eService.id
        producer <- event.eService.producer
        versions <- event.eService.versions
        status   <- event.eService.status
      } yield EServiceAddedV1
        .of(
          EServiceV1(
            id = id.toString,
            producer = producer.toString,
            name = event.eService.name,
            description = event.eService.description,
            scopes = event.eService.scopes.get,
            versions = versions.map(ver =>
              EServiceVersionV1(
                id = ver.id.get.toString,
                version = ver.version,
                docs = ver.docs,
                proposal = ver.proposal.get
              )
            ),
            status = status
          )
        )
    }.toRight(new RuntimeException("Deserialization from protobuf failed"))

}
