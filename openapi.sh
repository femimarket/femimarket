#!/bin/sh
set -e
cd "$(dirname "$0")"
buildDir="app/shared/build"
DATABASE_URL="sqlite::memory:" cargo run -q --manifest-path rust/Cargo.toml -p care-service -- openapi > rust/target/care-service-openapi.json
openapi-generator generate \
    -i rust/target/care-service-openapi.json \
    -g kotlin \
    --library multiplatform \
    --skip-validate-spec \
    -o "$buildDir/generated/source/care-service" \
    --package-name market.femi.care \
    --global-property models,apis,supportingFiles \
    --additional-properties dateLibrary=kotlinx-datetime,useCoroutines=true,omitGradleWrapper=true,modelPropertyNaming=original \
    --type-mappings binary=kotlin.String,UUID=String,File=ByteArray,DateTime=LocalDateTime \
    --import-mappings LocalDateTime=kotlinx.datetime.LocalDateTime
DATABASE_URL="sqlite::memory:" cargo run -q --manifest-path rust/Cargo.toml -p match-service -- openapi > rust/target/match-service-openapi.json
openapi-generator generate \
    -i rust/target/match-service-openapi.json \
    -g kotlin \
    --library multiplatform \
    --skip-validate-spec \
    -o "$buildDir/generated/source/match-service" \
    --package-name market.femi.match \
    --global-property models,apis,supportingFiles \
    --additional-properties dateLibrary=kotlinx-datetime,useCoroutines=true,omitGradleWrapper=true,modelPropertyNaming=original \
    --type-mappings binary=kotlin.String,UUID=String,File=ByteArray
DATABASE_URL="sqlite::memory:" cargo run -q --manifest-path rust/Cargo.toml -p ui-service -- openapi > rust/target/ui-service-openapi.json
openapi-generator generate \
    -i rust/target/ui-service-openapi.json \
    -g kotlin \
    --library multiplatform \
    --skip-validate-spec \
    -o "$buildDir/generated/source/ui-service" \
    --package-name market.femi.ui \
    --global-property models,apis,supportingFiles \
    --additional-properties dateLibrary=kotlinx-datetime,useCoroutines=true,omitGradleWrapper=true,modelPropertyNaming=original \
    --type-mappings binary=kotlin.String,UUID=String,File=ByteArray
DATABASE_URL="sqlite::memory:" MATRIX_URL="x" FS_URL="x" cargo run -q --manifest-path rust/Cargo.toml -p music-service -- openapi > rust/target/music-service-openapi.json
openapi-generator generate \
    -i rust/target/music-service-openapi.json \
    -g kotlin \
    --library multiplatform \
    --skip-validate-spec \
    -o "$buildDir/generated/source/music-service" \
    --package-name market.femi.music \
    --global-property models,apis,supportingFiles \
    --additional-properties dateLibrary=kotlinx-datetime,useCoroutines=true,omitGradleWrapper=true,modelPropertyNaming=original \
    --type-mappings binary=kotlin.String,UUID=String,File=ByteArray
DATABASE_URL="sqlite::memory:" MATRIX_URL="x" cargo run -q --manifest-path rust/Cargo.toml -p matrix-service -- openapi > rust/target/matrix-service-openapi.json
openapi-generator generate \
    -i rust/target/matrix-service-openapi.json \
    -g kotlin \
    --library multiplatform \
    --skip-validate-spec \
    -o "$buildDir/generated/source/matrix-service" \
    --package-name market.femi.matrix \
    --global-property models,apis,supportingFiles \
    --additional-properties dateLibrary=kotlinx-datetime,useCoroutines=true,omitGradleWrapper=true,modelPropertyNaming=original \
    --type-mappings binary=kotlin.String,UUID=String,File=ByteArray
openapi-generator generate \
    -i rust/target/matrix-service-openapi.json \
    -g rust \
    --skip-validate-spec \
    -o rust/matrix-service/client \
    --additional-properties packageName=matrix-client
