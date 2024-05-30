# hmpps-assess-risks-and-needs-handover-service
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-assess-risks-and-needs-handover-service)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-assess-risks-and-needs-handover-service "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-assess-risks-and-needs-handover-service/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-assess-risks-and-needs-handover-service)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-assess-risks-and-needs-handover-service/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-assess-risks-and-needs-handover-service)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](http://arns-handover-service-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

HMPPS Assess Risks and Needs (ARNS) Handover Service is a (mostly) backend service built for handling authentication 
and shared context data across OASys and ARNS-space applications. It is managed by both the ARNS and 
Sentence Planning (SP) team as both os their projects rely on it for authentication.

## What is the handover service? How does it work?

The ARNS Handover Service operates as follows:

1. **Handover Context Payload**: 
   - A client (OASys) authorized through HMPPS Auth sends a payload of contextual data to the `/handover` endpoint in the ARNS. 
   Handover Service. This payload includes information about the principal, the subject, and additional context related to 
   HMPPS Strength-Based Needs Assessment (SBNA/SAN) or HMPPS Sentence Plan (SP).

2. **Handover Link Generation**: 
   - A handover link is generated and returned to the client. This link should be presented to the user.

3. **User Authentication**: 
   - When the user clicks on the handover link, they are authenticated within the ARNS Handover Service. A cookie
   is stored on the user's browser to maintain this authentication session.

4. **Redirection to Intended Service**:
   - The ARNS Handover Service then redirects the user to the intended service (SBNA/SAN or SP).

5. **OAuth2 Authorization**: 
   - The intended service initiates an OAuth2 authorization code flow grant with the user and the ARNS Handover Service.
   Note that intended service must have a registered client within the ARNS Handover Service, and therefore know the
   client ID and client secret to perform this flow.

6. **Access Token Retrieval**: 
   - After the authorization code flow is completed, the intended service receives an access token for the user.

7. **Contextual Data Exchange**: 
   - The intended service can then use the access token to exchange for the contextual information provided in step 1.

## Running the service

To run this service in your local environment, follow these steps:

1. **Set the Active Spring Profile to `local`**
    - The `local` profile configures various application properties, enabling the ARNS Handover Service to work 
      with a client service hosted at `localhost:3000`.

2. **Run a Redis Instance**
    - Ensure a passwordless Redis instance is running on `localhost:6379`.

## Generating a handover session
Currently, the ARNS Handover Service does not provide a user interface for generating handover sessions and 
their subsequent handover links. Instead, a cURL request can be made to the `/handover` endpoint to create a session.

```cURL
curl -X "POST" "http://localhost:8080/handover" \
     -H 'Authorization: Bearer HMPPS_AUTH_CLIENT_CREDENTIALS_JWT \
     -H 'Content-Type: application/json; charset=utf-8' \
     -d $'{
  "subject": {
    "gender": 0,
    "location": 0,
    "crn": "X483319",
    "sexuallyMotivatedOffenceHistory": true,
    "dateOfBirth": "1990-01-01",
    "familyName": "John",
    "givenName": "Smith",
    "pnc": "01/1000000"
  },
  "assessmentContext": {
    "oasysAssessmentPk": "913ea2fc-049f-41c6-a19e-be2e8725712b",
    "assessmentUUID": "d8cb7e78-1d55-4153-a2ed-a73fefc361a8"
  },
  "principal": {
    "displayName": "Jane Doe",
    "accessMode": "READ_WRITE",
    "identifier": "123ABC"
  }
}'
```

This request will return an object formatted like

```json
{
   "handoverSessionId": "{HANDOVER_SESSION_UUID}",
   "handoverLink": "http://localhost:8080/handover/{HANDOVER_CODE}"
}
```
where the value of property `handoverLink` will be a URL that can be used within the browser to begin the handover
process.
