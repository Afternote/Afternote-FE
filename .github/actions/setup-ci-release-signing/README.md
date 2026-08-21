# Ephemeral CI release signing

Run this local composite action after `./.github/actions/setup-ci-config`. It creates a one-run JKS
keystore under the GitHub runner's temporary storage and a validation-only `local.properties` file
in the ephemeral checkout. No production key or repository secret is read.

The action refuses to overwrite an existing `local.properties` file and requires
`AFTERNOTE_CI_CONFIG_MODE=stub`. Its outputs are only for release packaging validation; never use
them in Firebase App Distribution or Google Play publishing jobs.
