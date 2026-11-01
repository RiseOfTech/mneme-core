## 1. Traceability: scenario to existing test

- [x] 1.1 `card-management`: create, read, 404 and byte-identity (`CardEndpointIntegrationTest.createThenGetRoundTrip`, `missingCardAndForeignCardReturnByteIdentical404Bodies`; `CardControllerTest.whenBlankWord_returns400`)
- [x] 1.2 `card-management`: list (`listReturnsOnlyCallersCardsNewestFirst`, `listReturnsEmptyArrayForUserWithNoCards`)
- [x] 1.3 `card-management`: replace (`updateReplacesAllFieldsAndLeavesReviewUnchanged`, `updateWithNoImageUrlClearsImageAndNoVisibilityDefaultsToPrivate`, `updateOnMissingAndForeignCardReturnByteIdentical404Bodies`; `CardControllerTest.whenUpdateBodyHasBlankWord_returns400`)
- [x] 1.4 `card-management`: delete (`deleteRemovesCardAndReviewAndShrinksList`, `deleteOnMissingAndForeignCardReturnByteIdentical404BodiesAndDeletesNothing`)
- [x] 1.5 `leitner-review`: schedule (`LeitnerSchedulerTest`, all tests; `ReviewEndpointIntegrationTest.correctGradeReturnsBoxAndDueAtFromSecondInterval`, `correctThenIncorrectMovesReviewThroughBoxes`)
- [x] 1.6 `leitner-review`: errors (`ReviewEndpointIntegrationTest.malformedGradeReturns400`, `missingReviewAndForeignReviewReturnByteIdentical404Bodies`; `ReviewControllerTest.whenMissingGrade_returns400`, `whenUnrecognisedGrade_returns400`)
- [x] 1.7 `review-session` (`SessionEndpointIntegrationTest`, all tests; `SessionControllerTest.whenCardHasNoImage_imageUrlSerialisesAsNull`, `whenNothingDue_returnsEmptyJsonArray`; `JpaReviewRepositoryIntegrationTest.findDueOrdersByDueAtAscending`; `DefaultGetReviewSessionUseCaseTest.whenReviewsCardIsMissing_throwsReviewSessionIntegrityException`)
- [x] 1.8 `authentication` (`MeEndpointIntegrationTest`, all tests; `ActuatorHealthIntegrationTest`, all tests; `ReviewEndpointIntegrationTest.getOnPostOnlyReviewRouteReturnsMethodNotAllowedNotUnauthorized`)
- [x] 1.9 `api-error-contract` (`CardControllerTest.whenBlankWord_returns400`, `whenUseCaseThrowsUnexpectedException_returns500`; `ReviewEndpointIntegrationTest.malformedGradeReturns400`, `missingReviewAndForeignReviewReturnByteIdentical404Bodies`, `getOnPostOnlyReviewRouteReturnsMethodNotAllowedNotUnauthorized`)
- [x] 1.10 `cross-origin-access` (`CorsConfigTest`, all tests; `CorsConfigEmptyOriginsTest`)
- [x] 1.11 `api-documentation`: contract served (`ActuatorHealthIntegrationTest.openApiDocsReturns200WithoutAuthorizationHeader`, `swaggerUiReturns200WithoutAuthorizationHeader`)

## 2. Scenarios with no direct test

True by reading the code and configuration only. No tests are added in this change.

- [x] 2.1 `card-management`, Listing ignores visibility
- [x] 2.2 `card-management`, Same word twice
- [x] 2.3 `leitner-review`, grades are not stored (no column exists)
- [x] 2.4 `authentication`, a `kid` header does not cause rejection, and the 60-second clock skew
- [x] 2.5 `cross-origin-access`, the `prod` profile value itself (the empty-origins behaviour is tested, the profile is not)
- [x] 2.6 `api-documentation`, No documentation in production
- [x] 2.7 `api-error-contract`, the failure is logged

## 3. Verification

- [x] 3.1 Run `openspec validate --all --strict`
- [ ] 3.2 Human review of the seven specs
- [ ] 3.3 Run `openspec archive bootstrap-baseline-specs`
