## Why

CI has been failing since the JWT work (runs on `0b9a147` and `f8549e2`), so no image has
been published since. `JwtPropertiesTest` needs `keys/mneme-auth-public.pem` on the
classpath, but `*.pem` is gitignored and the file exists only on one developer's disk.
On a clean checkout 1 of 145 tests fails.

## What Changes

- `JwtPropertiesTest` resolves the key from the location the other tests already use, so
  the suite no longer depends on an untracked file.
- No production code or configuration changes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a test-only fix, so the change sets `skip_specs: true`.

## Impact

`JwtPropertiesTest` only. CI and image publishing start working again on merge.
