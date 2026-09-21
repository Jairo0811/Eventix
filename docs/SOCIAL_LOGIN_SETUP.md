# Google and Apple sign-in

Eventix supports local credentials and optional OIDC sign-in with Google and Apple. External identities are linked to a local `users` record through `external_identities`; authorization therefore continues to use Eventix roles.

## Environment variables

```dotenv
EVENTIX_SOCIAL_LOGIN_ENABLED=true
EVENTIX_GOOGLE_CLIENT_ID=
EVENTIX_GOOGLE_CLIENT_SECRET=
EVENTIX_APPLE_CLIENT_ID=
EVENTIX_APPLE_CLIENT_SECRET=
```

Only configured providers are displayed. Keep social login disabled until at least one complete provider configuration is available.

## Redirect URIs

- Google: `{PUBLIC_BASE_URL}/login/oauth2/code/google`
- Apple: `{PUBLIC_BASE_URL}/login/oauth2/code/apple`

Register the exact HTTPS production URI with each provider. Local Google development can use `http://localhost:8080/login/oauth2/code/google`; Apple requires a verified HTTPS domain and Services ID.

For Apple, `EVENTIX_APPLE_CLIENT_ID` is the Services ID. `EVENTIX_APPLE_CLIENT_SECRET` is an ES256 client-secret JWT signed with the Apple private key. Apple limits its expiration, so rotate it before it expires and keep the `.p8` key outside the repository.

## Account and role behavior

- A verified provider email matching an existing user links that provider to the account.
- A new verified email creates an active account with the `USER` role.
- Provider subject identifiers are unique and cannot be moved between accounts.
- Google or Apple never chooses or elevates an Eventix role.
- The local email/password flow remains available.
