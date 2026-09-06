#!/usr/bin/env bash
# Builds the ffi crate for every platform the app consumes:
#
#   desktop  -> app/shared/src/jvmMain/resources/natives/          (System.load via extracted resource)
#   android  -> app/shared/src/androidMain/jniLibs/<abi>/          (System.loadLibrary("ffi"))
#   ios      -> app/shared/src/nativeInterop/cinterop/             (metadata_ffi.h beside ffi.def; .a in target/)
#   wasm     -> app/webApp/src/webMain/resources/ffi_bg.wasm       (wasm-pack on ffi itself)
set -euo pipefail

export PATH="$HOME/.cargo/bin:$PATH"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FFI="$ROOT/ffi"
SHARED_SRC="$ROOT/app/shared/src"
WEB_RESOURCES="$ROOT/app/webApp/src/webMain/resources"

APPLE_TARGETS=(
  aarch64-apple-ios
  aarch64-apple-ios-sim
)

# NDK linker/env for these triples comes from ffi/.cargo/config.toml.
ANDROID_PAIRS=(
  aarch64-linux-android:arm64-v8a
  armv7-linux-androideabi:armeabi-v7a
)

rustup target add "${APPLE_TARGETS[@]}" \
  aarch64-linux-android armv7-linux-androideabi \
  wasm32-unknown-unknown

command -v cbindgen >/dev/null || cargo install cbindgen

########################################
# Desktop (host)
########################################
echo ">> desktop (host)"
(cd "$FFI" && cargo build --release)
case "$(uname -s)" in
  Darwin) HOST_LIB="libffi.dylib" ;;
  Linux)  HOST_LIB="libffi.so" ;;
  *)      HOST_LIB="ffi.dll" ;;
esac
mkdir -p "$SHARED_SRC/jvmMain/resources/natives"
cp "$FFI/target/release/$HOST_LIB" "$SHARED_SRC/jvmMain/resources/natives/$HOST_LIB"

########################################
# Android (plain cargo per triple -> jniLibs)
########################################
echo ">> android"
JNI_LIBS="$SHARED_SRC/androidMain/jniLibs"
for pair in "${ANDROID_PAIRS[@]}"; do
  triple="${pair%%:*}"
  abi="${pair##*:}"
  (cd "$FFI" && cargo build --release --target "$triple")
  mkdir -p "$JNI_LIBS/$abi"
  cp "$FFI/target/$triple/release/libffi.so" "$JNI_LIBS/$abi/libffi.so"
done

########################################
# iOS (device + simulator staticlibs + cbindgen header)
########################################
echo ">> ios (${APPLE_TARGETS[*]})"
for t in "${APPLE_TARGETS[@]}"; do
  (cd "$FFI" && cargo build --release --target "$t")
done
# The header lands BESIDE the ffi.def that consumes it; the .a files are linked
# by the cinterop straight from ffi/target/<triple>/release.
# TARGET makes cbindgen resolve the cfg gates as iOS, so the header carries the
# md_*/ae_*/db_* C-ABI and skips the cfg'd-out JNI symbols.
CINTEROP_DIR="$SHARED_SRC/nativeInterop/cinterop"
(cd "$FFI" && TARGET=aarch64-apple-ios cbindgen --config cbindgen.toml --output "$CINTEROP_DIR/ffi.h")

########################################
# WebAssembly (ffi itself via wasm-pack)
########################################
echo ">> wasm"
mkdir -p "$WEB_RESOURCES"
(cd "$FFI" && npx wasm-pack build --target web --out-dir pkg --out-name ffi)
cp "$FFI/pkg/ffi_bg.wasm" "$WEB_RESOURCES/ffi_bg.wasm"

echo
echo "✓ desktop  → $SHARED_SRC/jvmMain/resources/natives/$HOST_LIB"
echo "✓ android  → $SHARED_SRC/androidMain/jniLibs/<abi>/libffi.so"
echo "✓ ios      → $CINTEROP_DIR/ffi.h (+ .a in ffi/target/<triple>/release)"
echo "✓ wasm     → $WEB_RESOURCES/ffi_bg.wasm (glue JS in ffi/pkg/)"
