#!/usr/bin/env bash
# Runs the full build and writes a short build-report.md, so a review reads the outcome
# and the failed tests instead of the whole Gradle log. Used by the task-review skill.
set -uo pipefail

if [[ "${1:-}" != "full" ]]; then
	echo "usage: scripts/build-report.sh full" >&2
	exit 2
fi

cd "$(dirname "$0")/.."

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

./gradlew clean build --continue --console=plain >"$log" 2>&1
status=$?

{
	echo "# Build report - $(date '+%Y-%m-%d %H:%M')"
	echo
	echo "Commit: $(git rev-parse --short HEAD)"
	if [[ $status -eq 0 ]]; then
		echo "Result: SUCCESS"
	else
		echo "Result: FAILURE (exit code $status)"
	fi
	echo
	echo "## Failures"
	echo
	grep -E ' FAILED$|^\* What went wrong|tests completed' "$log" | sed 's/^/- /' || true
} >build-report.md

exit $status
