#!/bin/bash
#
# Copyright (C) 2026 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Builds the Docker image of the Kaltblut command line client (kaltblut-cli/Dockerfile).
#
# Usage: build.sh snapshot [--mvn] [--push]
#        build.sh release [--mvn]
#
#   snapshot   The current -SNAPSHOT version. Without --push the image is built for this machine only and loaded
#              into the local Docker, tagged <version> and "snapshot", and smoke tested. With --push it is built for
#              linux/amd64 and linux/arm64 and pushed with the same tags.
#   release    A released version (no -SNAPSHOT): built for linux/amd64 and linux/arm64 and pushed, tagged <version>
#              and "latest". Run it on the checkout of the release tag.
#   --mvn      Build the fat JAR first ("mvn clean install" of kaltblut-cli and the modules it needs, without
#              tests), so that it is exactly the one of the current version.
#
# The version is taken from the root pom.xml. The image name is phelger/kaltblut; set IMAGE to use another one.
# Pushing needs a "docker login" first, e.g. echo "$DOCKER_TOKEN" | docker login -u "$DOCKER_USER" --password-stdin

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
CLI="$ROOT/kaltblut-cli"
JAR="$CLI/target/kaltblut-cli-full.jar"
IMAGE="${IMAGE:-phelger/kaltblut}"
BUILDER=kaltblut

usage ()
{
  sed -n '/^# Usage:/,/^# The version/p' "$0" | sed 's/^# \{0,1\}//' >&2
  exit 2
}

[ $# -ge 1 ] || usage
MODE="$1"
shift
MVN=false
PUSH=false
for ARG in "$@"; do
  case "$ARG" in
    --mvn) MVN=true ;;
    --push) PUSH=true ;;
    *) usage ;;
  esac
done
case "$MODE" in
  snapshot) ;;
  release) [ "$PUSH" = false ] || usage; PUSH=true ;;
  *) usage ;;
esac

# The version of the project - the first <version> outside of <parent>
VERSION="$(sed -n '/<parent>/,/<\/parent>/!{s/^  <version>\(.*\)<\/version>.*/\1/p;}' "$ROOT/pom.xml" | head -1)"
if [ -z "$VERSION" ]; then
  echo "The version cannot be read from '$ROOT/pom.xml'" >&2
  exit 3
fi
case "$MODE:$VERSION" in
  snapshot:*-SNAPSHOT) TAGS=(-t "$IMAGE:$VERSION" -t "$IMAGE:snapshot") ;;
  release:*-SNAPSHOT) echo "The version $VERSION is a snapshot - a release needs a released version" >&2; exit 2 ;;
  release:*) TAGS=(-t "$IMAGE:$VERSION" -t "$IMAGE:latest") ;;
  *) echo "The version $VERSION is no snapshot - use 'release'" >&2; exit 2 ;;
esac

if [ "$MVN" = true ]; then
  echo "Building the fat JAR of $VERSION"
  (cd "$ROOT" && mvn --batch-mode -q -pl kaltblut-cli -am clean install -DskipTests)
fi

# The Dockerfile copies the fat JAR from target/ - its file name has no version, so check its manifest
if [ ! -f "$JAR" ]; then
  echo "Expected kaltblut-cli/target/kaltblut-cli-full.jar, found none - use --mvn" >&2
  exit 3
fi
JAR_VERSION="$(unzip -p "$JAR" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: *//p' | tr -d '\r')"
if [ "$JAR_VERSION" != "$VERSION" ]; then
  echo "Expected kaltblut-cli/target/kaltblut-cli-full.jar of version $VERSION, found ${JAR_VERSION:-unknown} - use --mvn" >&2
  exit 3
fi

if [ "$PUSH" = false ]; then
  echo "Building $IMAGE:$VERSION for this machine"
  docker build --pull "${TAGS[@]}" -f "$CLI/Dockerfile" "$CLI"
  echo "Smoke test"
  docker run --rm "$IMAGE:$VERSION" --version
  echo "Built $IMAGE:$VERSION and $IMAGE:snapshot - e.g. docker run --rm -v \"\$PWD:/data\" $IMAGE:snapshot validate invoice.pdf"
else
  echo "Building and pushing $IMAGE:$VERSION for linux/amd64 and linux/arm64"
  docker buildx rm "$BUILDER" > /dev/null 2>&1 || true
  docker buildx create --name "$BUILDER" --driver docker-container > /dev/null
  docker buildx build --builder "$BUILDER" --platform=linux/amd64,linux/arm64 --push --pull "${TAGS[@]}" -f "$CLI/Dockerfile" "$CLI"
  docker buildx rm "$BUILDER" > /dev/null 2>&1 || true
  echo "Pushed ${TAGS[*]}"
fi
