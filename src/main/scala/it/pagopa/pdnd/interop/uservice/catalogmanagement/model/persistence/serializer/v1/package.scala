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
              id = UUID.fromString(eServicesV1.value.id),
              producerId = UUID.fromString(eServicesV1.value.producerId),
              name = eServicesV1.value.name,
              description = eServicesV1.value.description,
              scopes = Some(eServicesV1.value.scopes),
              versions = eServicesV1.value.versions.map(ver1 =>
                EServiceVersion(
                  id = UUID.fromString(ver1.id),
                  version = ver1.version,
                  docs = ver1.docs,
                  proposal = Some(ver1.proposal)
                )
              ),
              status = eServicesV1.value.status
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
            id = eService.id.toString,
            producerId = eService.producerId.toString,
            name = eService.name,
            description = eService.description,
            scopes = eService.scopes.getOrElse(Seq.empty),
            versions = eService.versions.map(ver =>
              EServiceVersionV1(
                id = ver.id.toString,
                version = ver.version,
                docs = ver.docs,
                proposal = ver.proposal.get
              )
            ),
            status = eService.status
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
            id = UUID.fromString(event.eService.id),
            producerId = UUID.fromString(event.eService.producerId),
            name = event.eService.name,
            description = event.eService.description,
            scopes = Some(event.eService.scopes),
            versions = event.eService.versions.map(ver1 =>
              EServiceVersion(
                id = UUID.fromString(ver1.id),
                version = ver1.version,
                docs = ver1.docs,
                proposal = Some(ver1.proposal)
              )
            ),
            status = event.eService.status
          )
        )
      )

  implicit def eServiceAddedV1PersistEventSerializer: PersistEventSerializer[EServiceAdded, EServiceAddedV1] = event =>
    Right[Throwable, EServiceAddedV1] {
      EServiceAddedV1
        .of(
          EServiceV1(
            id = event.eService.id.toString,
            producerId = event.eService.producerId.toString,
            name = event.eService.name,
            description = event.eService.description,
            scopes = event.eService.scopes.getOrElse(Seq.empty[String]),
            versions = event.eService.versions.flatMap(ver =>
              for {
                proposal <- ver.proposal
              } yield EServiceVersionV1(
                id = ver.id.toString,
                version = ver.version,
                docs = ver.docs,
                proposal = proposal
              )
            ),
            status = event.eService.status
          )
        )
    }

}
