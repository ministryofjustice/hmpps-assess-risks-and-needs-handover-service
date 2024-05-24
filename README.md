# hmpps-assess-risks-and-needs-handover-service
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-assess-risks-and-needs-handover-service)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-assess-risks-and-needs-handover-service "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-assess-risks-and-needs-handover-service/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-assess-risks-and-needs-handover-service)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-assess-risks-and-needs-handover-service/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-assess-risks-and-needs-handover-service)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-assess-risks-and-needs-handover-service-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

HMPPS Assess Risks and Needs (ARNS) Handover Service is a (mostly) backend service built for handling authentication and shared context data across OASys and ARNS-space applications. It is managed by both the ARNS and Sentence Planning (SP) team as both os their projects rely on it for authentication.

## Running the Service

To run this service in your local environment, follow these steps:

1. **Set the Active Spring Profile to `local`**:
    - The `local` profile configures various application properties, enabling the ARNS Handover Service to work with a client service hosted at `localhost:3000`.

2. **Run a Redis Instance**:
    - Ensure a passwordless Redis instance is running on `localhost:6379`.
