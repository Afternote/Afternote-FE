# Secretless CI app configuration

This local composite action generates the deterministic, inert Android configuration used by
validation jobs. It writes `app/google-services.json` and exports placeholder social-login values
through `GITHUB_ENV`; it never reads repository or environment secrets.

Use it after `actions/checkout` in lint, unit-test, screenshot, CodeQL, or androidTest jobs:

```yaml
- name: Set up CI-only app configuration
  uses: ./.github/actions/setup-ci-config
```

This action is intentionally forbidden from `release-distribution.yml`. Release builds keep their
separate approved environment, signing material, Firebase service account, and real app
configuration. `AFTERNOTE_CI_CONFIG_MODE=stub` identifies artifacts produced with this fixture as
validation-only; do not publish or distribute them.
