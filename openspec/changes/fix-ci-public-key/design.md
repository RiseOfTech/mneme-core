## Context

`app.jwt.public-key` binds to a resource location. `application.yaml` defaults it to
`classpath:keys/mneme-auth-public.pem`, and `JwtPropertiesTest` asserts that this location
resolves. Every other test points the property at a temporary key from `JwtTestTokens`.

## Decisions

**Change the test; do not commit the key.** A public key is not secret, but the
`application.yaml` comment and the JWT design both say key material comes from the
environment, and `.gitignore` enforces that. Committing it would also bake a key into
every image. The test's purpose, a location binding to a resource with PEM content, is
fully kept with the temporary key.

## Risks / Trade-offs

The classpath default in `application.yaml` still resolves to nothing on a clean clone, so
running the app there requires `JWT_PUBLIC_KEY`. That is existing behaviour, and it fails
at startup with a clear message. Documenting it belongs to the README update, not here.
