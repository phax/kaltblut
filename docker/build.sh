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
#        build.sh release <version>
#
#   snapshot   The current -SNAPSHOT version of the root pom.xml, from kaltblut-cli/target/kaltblut-cli-full.jar.
#              Without --push the image is built for this machine only and loaded into the local Docker, tagged
#              <version> and "snapshot", and smoke tested. With --push it is built for linux/amd64 and linux/arm64
#              and pushed with the same tags.
#   release    A released version (no -SNAPSHOT): the shaded JAR is downloaded from the GitHub release
#              (kaltblut-cli-<version>-shaded.jar of the tag kaltblut-parent-pom-<version>), smoke tested, built for
#              linux/amd64 and linux/arm64 and pushed, tagged <version> and "latest".
#   --mvn      Build the fat JAR first ("mvn clean install" of kaltblut-cli and the modules it needs, without
#              tests), so that it is exactly the one of the current version.
#
# The image name is phelger/kaltblut; set IMAGE to use another one.
# Pushing needs a "docker login" first, e.g. echo "$DOCKER_TOKEN" | docker login -u "$DOCKER_USER" --password-stdin

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
CLI="$ROOT/kaltblut-cli"
JAR="$CLI/target/kaltblut-cli-full.jar"
IMAGE="${IMAGE:-phelger/kaltblut}"
BUILDER=kaltblut
RELEASE_URL=https://github.com/phax/kaltblut/releases/download

usage ()
{
  sed -n '/^# Usage:/,/^# The image name/p' "$0" | sed 's/^# \{0,1\}//' >&2
  exit 2
}

[ $# -ge 1 ] || usage
MODE="$1"
shift
MVN=false
PUSH=false
case "$MODE" in
  snapshot)
    for ARG in "$@"; do
      case "$ARG" in
        --mvn) MVN=true ;;
        --push) PUSH=true ;;
        *) usage ;;
      esac
    done
    # The version of the project - the first <version> outside of <parent>
    VERSION="$(sed -n '/<parent>/,/<\/parent>/!{s/^  <version>\(.*\)<\/version>.*/\1/p;}' "$ROOT/pom.xml" | head -1)"
    if [ -z "$VERSION" ]; then
      echo "The version cannot be read from '$ROOT/pom.xml'" >&2
      exit 3
    fi
    case "$VERSION" in
      *-SNAPSHOT) ;;
      *) echo "The version $VERSION is no snapshot - use 'release $VERSION'" >&2; exit 2 ;;
    esac
    TAGS=(-t "$IMAGE:$VERSION" -t "$IMAGE:snapshot")
    CONTEXT="$CLI"
    ;;
  release)
    [ $# -eq 1 ] || usage
    VERSION="$1"
    case "$VERSION" in
      *-SNAPSHOT) echo "The version $VERSION is a snapshot - use 'snapshot'" >&2; exit 2 ;;
      [0-9]*) ;;
      *) usage ;;
    esac
    PUSH=true
    TAGS=(-t "$IMAGE:$VERSION" -t "$IMAGE:latest")
    # The Dockerfile copies target/kaltblut-cli-full.jar - use a separate build context with the downloaded JAR
    CONTEXT="$(mktemp -d)"
    trap 'rm -rf "$CONTEXT"' EXIT
    JAR="$CONTEXT/target/kaltblut-cli-full.jar"
    mkdir "$CONTEXT/target"
    ASSET="$RELEASE_URL/kaltblut-parent-pom-$VERSION/kaltblut-cli-$VERSION-shaded.jar"
    echo "Downloading $ASSET"
    if ! curl -fsSL -o "$JAR" "$ASSET"; then
      echo "The GitHub release of $VERSION has no asset kaltblut-cli-$VERSION-shaded.jar" >&2
      exit 3
    fi
    ;;
  *) usage ;;
esac

if [ "$MVN" = true ]; then
  echo "Building the fat JAR of $VERSION"
  (cd "$ROOT" && mvn --batch-mode -q -pl kaltblut-cli -am clean install -DskipTests)
fi

# The fat JAR file name has no version, so check its manifest
if [ ! -f "$JAR" ]; then
  echo "Expected kaltblut-cli/target/kaltblut-cli-full.jar, found none - use --mvn" >&2
  exit 3
fi
JAR_VERSION="$(unzip -p "$JAR" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: *//p' | tr -d '\r')"
if [ "$JAR_VERSION" != "$VERSION" ]; then
  echo "Expected the fat JAR of version $VERSION, found ${JAR_VERSION:-unknown}" >&2
  exit 3
fi

if [ "$PUSH" = false ]; then
  echo "Building $IMAGE:$VERSION for this machine"
  docker build --pull "${TAGS[@]}" -f "$CLI/Dockerfile" "$CONTEXT"
  echo "Smoke test"
  docker run --rm "$IMAGE:$VERSION" --version
  echo "Built $IMAGE:$VERSION and $IMAGE:snapshot - e.g. docker run --rm -v \"\$PWD:/data\" $IMAGE:snapshot validate invoice.pdf"
else
  if [ "$MODE" = release ]; then
    echo "Smoke test of $VERSION on this machine"
    docker build --pull -q -t "$IMAGE:smoketest" -f "$CLI/Dockerfile" "$CONTEXT" > /dev/null
    docker run --rm "$IMAGE:smoketest" --version
    docker rmi "$IMAGE:smoketest" > /dev/null
  fi
  echo "Building and pushing $IMAGE:$VERSION for linux/amd64 and linux/arm64"
  docker buildx rm "$BUILDER" > /dev/null 2>&1 || true
  docker buildx create --name "$BUILDER" --driver docker-container > /dev/null
  docker buildx build --builder "$BUILDER" --platform=linux/amd64,linux/arm64 --push --pull "${TAGS[@]}" -f "$CLI/Dockerfile" "$CONTEXT"
  docker buildx rm "$BUILDER" > /dev/null 2>&1 || true
  echo "Pushed ${TAGS[*]}"
fi
