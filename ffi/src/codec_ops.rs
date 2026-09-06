//! Portable video frame extraction — extracts raw RGBA frames from a video file.
//!
//! - iOS: AVFoundation (AVAssetReader + AVAssetReaderTrackOutput + CVPixelBuffer)
//! - Android: NDK (MediaExtractor + MediaCodec + direct disk write)
//!
//! This module is binding-agnostic: the portable body lives here, and the C-ABI
//! shim (capi.rs) translates `*const c_char` paths into Rust `&str` and calls
//! `extract_video_to_rgba`.

/// Extract raw RGBA frames from `input_path` and write them to `output_path`.
///
/// The output file is a headerless, contiguous RGBA byte stream (width*height*4
/// bytes per frame, frames concatenated). If the pixel buffer has a bytes-per-row
/// that exceeds `width * 4` (hardware padding), each row is stripped before
/// writing.
///
/// Panics on any error — there is no recovery path for video extraction.
pub fn extract_video_to_rgba(input_path: &str, output_path: &str) {
    #[cfg(target_os = "ios")]
    {
        ios_extract_video_to_rgba(input_path, output_path);
    }

    #[cfg(target_os = "android")]
    {
        android_extract_video_to_rgba(input_path, output_path);
    }

    #[cfg(not(any(target_os = "ios", target_os = "android")))]
    {
        panic!("extract_video_to_rgba is not supported on this target");
    }
}

#[cfg(target_os = "android")]
fn android_extract_video_to_rgba(input_path: &str, output_path: &str) {
    use std::ffi::CString;
    use std::fs::File;
    use std::io::Write;
    use std::os::fd::AsRawFd;
    use std::time::Duration;

    // SAFETY: We call the NDK media APIs (MediaExtractor via ndk-sys, MediaCodec via ndk crate).
    // The video_ops module is compiled into the cdylib that Android loads via System.loadLibrary("ffi").

    unsafe {
        use ndk::media::media_codec::{MediaCodec, MediaCodecDirection};
        use ndk::media::media_format::MediaFormat;
        use ndk::media_error::MediaError;

        // ── Prepare input path for AMediaExtractor_setDataSource (C string) ──
        // CString::new just converts the Rust &str to a null-terminated C string;
        // it does NOT read the file. The actual file open happens inside
        // AMediaExtractor_setDataSource.
        let input_cstr = CString::new(input_path).expect("input_path contains NUL bytes");

        // ── Create MediaExtractor (via ndk-sys) ──────────────────────────
        use ndk_sys::{
            AMediaExtractor_advance, AMediaExtractor_delete,
            AMediaExtractor_getSampleTime, AMediaExtractor_getTrackCount,
            AMediaExtractor_getTrackFormat, AMediaExtractor_new, AMediaExtractor_readSampleData,
            AMediaExtractor_selectTrack, AMediaExtractor_setDataSource,
            AMediaExtractor_unselectTrack, AMediaFormat_getInt32, AMediaFormat_getString,
        };

        let extractor = AMediaExtractor_new();
        if extractor.is_null() {
            panic!("AMediaExtractor_new returned null");
        }

        let status = AMediaExtractor_setDataSource(extractor, input_cstr.as_ptr());
        if (status.0) != 0 {
            panic!("AMediaExtractor_setDataSource failed with status {}", status.0);
        }

        // ── Find video track ─────────────────────────────────────────────
        let track_count = AMediaExtractor_getTrackCount(extractor);
        let mut video_track_index = None;
        let mut video_format_ptr: *mut ndk_sys::AMediaFormat = std::ptr::null_mut();

        for i in 0..track_count {
            let track_format_ptr = AMediaExtractor_getTrackFormat(extractor, i);
            if !track_format_ptr.is_null() {
                let mime_ptr: *const u8 = std::ptr::null();
                let mut mime_ptr = mime_ptr;
                let mime_len = AMediaFormat_getString(
                    track_format_ptr,
                    b"mime\0".as_ptr(),
                    &mut mime_ptr as *mut *const u8,
                );
                if mime_len {
                    let mime = std::ffi::CStr::from_ptr(mime_ptr as *const u8).to_string_lossy();
                    if mime.starts_with("video/") {
                        video_track_index = Some(i);
                        video_format_ptr = track_format_ptr;
                        break;
                    }
                }
            }
        }

        let video_track_index = video_track_index.expect("no video track found");
        if video_format_ptr.is_null() {
            panic!("no video format found");
        }

        AMediaExtractor_selectTrack(extractor, video_track_index);

        // ── Get dimensions ───────────────────────────────────────────────
        let mut width: i32 = 0;
        let mut height: i32 = 0;
        if !AMediaFormat_getInt32(video_format_ptr, b"width\0".as_ptr(), &mut width) {
            panic!("failed to get width");
        }
        if !AMediaFormat_getInt32(video_format_ptr, b"height\0".as_ptr(), &mut height) {
            panic!("failed to get height");
        }
        let width = width as usize;
        let height = height as usize;
        let expected_frame_size = width * height * 4; // RGBA

        // ── Get MIME type and create decoder ─────────────────────────────
        let mut mime_ptr: *const u8 = std::ptr::null();
        let mime_len = AMediaFormat_getString(
            video_format_ptr,
            b"mime\0".as_ptr(),
            &mut mime_ptr as *mut *const u8,
        );
        if !mime_len {
            panic!("failed to get MIME type");
        }
        let mime = std::ffi::CStr::from_ptr(mime_ptr as *const u8).to_string_lossy().to_string();

        // ── Create MediaCodec (via ndk crate) ────────────────────────────
        let codec = MediaCodec::from_decoder_type(&mime).expect("failed to create decoder");

        // Create MediaFormat for configuration
        let mut format = MediaFormat::new();
        format.set_i32("width", width as i32);
        format.set_i32("height", height as i32);
        format.set_str("mime", &mime);

        codec
            .configure(&format, None, MediaCodecDirection::Decoder)
            .expect("failed to configure codec");

        codec.start().expect("failed to start codec");

        // ── Open output file ─────────────────────────────────────────────
        let mut out_file = File::create(output_path).expect("failed to create output file");

        // ── Processing loop ──────────────────────────────────────────────
        let mut input_eos = false;
        let mut output_eos = false;
        let timeout = Duration::from_micros(10_000); // 10ms

        while !output_eos {
            // Feed input samples
            if !input_eos {
                match codec.dequeue_input_buffer(timeout) {
                    Ok(ndk::media::media_codec::DequeuedInputBufferResult::Buffer(mut input_buffer)) => {
                        let input_slice = input_buffer.buffer_mut();
                        let sample_size = AMediaExtractor_readSampleData(
                            extractor,
                            input_slice.as_mut_ptr() as *mut u8,
                            input_slice.len(),
                        );
                        if sample_size < 0 {
                            // End of stream
                            codec.queue_input_buffer(
                                input_buffer,
                                0,
                                0,
                                0,
                                0x00000004, // AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM
                            ).expect("failed to queue EOS");
                            input_eos = true;
                        } else {
                            codec.queue_input_buffer(
                                input_buffer,
                                0,
                                sample_size as usize,
                                AMediaExtractor_getSampleTime(extractor) as u64,
                                0,
                            ).expect("failed to queue input buffer");
                            AMediaExtractor_advance(extractor);
                        }
                    }
                    Ok(ndk::media::media_codec::DequeuedInputBufferResult::TryAgainLater) => {
                        // Timeout — continue
                    }
                    Err(e) => {
                        eprintln!("dequeue_input_buffer error: {:?}", e);
                        input_eos = true;
                    }
                }
            }

            // Retrieve decoded output
            match codec.dequeue_output_buffer(timeout) {
                Ok(ndk::media::media_codec::DequeuedOutputBufferInfoResult::Buffer(output_buffer)) => {
                    let buffer_info = output_buffer.info();
                    if buffer_info.flags() & 0x00000004 != 0 {
                        // AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM
                        output_eos = true;
                    }

                    if buffer_info.size() > 0 {
                        let output_slice = output_buffer.buffer();
                        let offset = buffer_info.offset() as usize;
                        let size = buffer_info.size() as usize;
                        let data = &output_slice[offset..offset + size.min(expected_frame_size)];

                        out_file.write_all(data).expect("failed to write frame data");
                    }

                    codec.release_output_buffer(output_buffer, false)
                        .expect("failed to release output buffer");
                }
                Ok(ndk::media::media_codec::DequeuedOutputBufferInfoResult::TryAgainLater) => {
                    // Timeout — continue
                }
                Ok(ndk::media::media_codec::DequeuedOutputBufferInfoResult::OutputFormatChanged) => {
                    // Format changed — continue
                }
                Ok(ndk::media::media_codec::DequeuedOutputBufferInfoResult::OutputBuffersChanged) => {
                    // Buffers changed — continue
                }
                Err(MediaError::ErrorEndOfStream) => {
                    output_eos = true;
                }
                Err(e) => {
                    eprintln!("dequeue_output_buffer error: {:?}", e);
                }
            }
        }

        // ── Cleanup ──────────────────────────────────────────────────────
        codec.stop().expect("failed to stop codec");
        AMediaExtractor_unselectTrack(extractor, video_track_index);
        AMediaExtractor_delete(extractor);
        out_file.flush().expect("failed to flush output file");
    }
}

#[cfg(target_os = "ios")]
fn ios_extract_video_to_rgba(input_path: &str, output_path: &str) {
    // Use the global tokio runtime to run async file I/O from this sync FFI boundary.
    // This follows the same pattern as db_ops.rs: a OnceLock<Runtime> + block_on.
    use std::sync::OnceLock;
    static RUNTIME: OnceLock<tokio::runtime::Runtime> = OnceLock::new();
    let rt = RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("tokio runtime for video extraction")
    });

    rt.block_on(async {
        extract_video_to_rgba_inner(input_path, output_path).await;
    });
}

#[cfg(target_os = "ios")]
async fn extract_video_to_rgba_inner(input_path: &str, output_path: &str) {
    use std::ffi::c_void;
    use tokio::io::AsyncWriteExt;

    // SAFETY: We call the objc2 API which wraps the full AVFoundation / CoreVideo
    // frameworks. The video_ops module is compiled into the staticlib that iOS
    // links against, so these frameworks are available at link time (the same
    // linker opts that the ffi.def declares: -framework AVFoundation).

    unsafe {
        use objc2::AnyThread;
        use objc2::rc::Retained;
        use std::ptr::NonNull;
        use objc2::ffi::NSInteger;
        use objc2::runtime::AnyObject;
        use objc2_foundation::{NSDictionary, NSString, NSURL, NSNumber};
        use objc2_av_foundation::{
            AVURLAsset, AVAssetReader, AVAssetReaderTrackOutput, AVAssetReaderStatus,
            AVMediaTypeVideo,
        };
        use objc2_core_video::{kCVPixelBufferPixelFormatTypeKey, kCVPixelFormatType_32RGBA};

        // Bring in the raw CoreVideo / CoreFoundation symbols.
        // In objc2 0.6 there is no `objc2::sys` module; we use raw `*mut c_void`
        // pointers which is the correct C FFI type for `CFTypeRef` / `CVPixelBufferRef`.
        #[link(name = "CoreVideo", kind = "framework")]
        unsafe extern "C" {
            fn CVPixelBufferLockBaseAddress(
                pixelBuffer: *mut c_void,
                lockFlags: u64,
            ) -> i32;
            fn CVPixelBufferUnlockBaseAddress(
                pixelBuffer: *mut c_void,
                unlockFlags: u64,
            ) -> i32;
            fn CVPixelBufferGetBaseAddress(
                pixelBuffer: *mut c_void,
            ) -> *mut c_void;
            fn CVPixelBufferGetWidth(pixelBuffer: *mut c_void) -> usize;
            fn CVPixelBufferGetHeight(pixelBuffer: *mut c_void) -> usize;
            fn CVPixelBufferGetBytesPerRow(pixelBuffer: *mut c_void) -> usize;
        }

        #[link(name = "CoreMedia", kind = "framework")]
        unsafe extern "C" {
            fn CMSampleBufferGetImageBuffer(sbuf: *mut c_void) -> *mut c_void;
            fn CMSampleBufferInvalidate(sampleBuffer: *mut c_void);
        }

        // ── Build input URL ──────────────────────────────────────────────
        // NSString::from_str takes a Rust &str directly — no CString needed.
        let ns_input_path = NSString::from_str(input_path);

        // ── Build URL ────────────────────────────────────────────────────
        let input_url = NSURL::fileURLWithPath(&ns_input_path);

        // ── Create asset + reader ────────────────────────────────────────
        let asset = AVURLAsset::initWithURL_options(
            AVURLAsset::alloc(),
            &input_url,
            None,
        );

        let reader = AVAssetReader::initWithAsset_error(
            AVAssetReader::alloc(),
            &asset,
        ).expect("failed to create AVAssetReader");

        // ── Get video track ──────────────────────────────────────────────
        let tracks = asset.tracksWithMediaType(&*AVMediaTypeVideo.unwrap());
        let video_track = tracks.firstObject().expect("no video track found");

        // ── Configure output: 32RGBA ─────────────────────────────────────
        // Use the library constant kCVPixelFormatType_32RGBA instead of
        // a hand-rolled magic number.
        let format_val = NSNumber::numberWithInteger(kCVPixelFormatType_32RGBA as NSInteger);

        // kCVPixelBufferPixelFormatTypeKey is a CFString (not NSString), so we can't
        // use from_retained_objects (which requires NSCopying keys). Instead we build
        // the dictionary with raw pointers via initWithObjects_forKeys_count.
        let output_settings: Retained<NSDictionary<NSString, AnyObject>> =
            Retained::cast_unchecked(NSDictionary::<NSString, AnyObject>::initWithObjects_forKeys_count(
                NSDictionary::alloc(),
                NonNull::new(Retained::into_raw(format_val) as *mut _).unwrap().as_ptr(),
                NonNull::new(
                    &*kCVPixelBufferPixelFormatTypeKey as *const _ as *mut _,
                ).unwrap().as_ptr(),
                1,
            ));

        let reader_output = AVAssetReaderTrackOutput::initWithTrack_outputSettings(
            AVAssetReaderTrackOutput::alloc(),
            &video_track,
            Some(&*output_settings),
        );

        reader.addOutput(&reader_output);
        assert!(reader.startReading(), "failed to start AVAssetReader reading");

        // ── Open output file via tokio::fs ───────────────────────────────
        let mut out_file = tokio::fs::File::create(output_path)
            .await
            .expect("failed to create output file");

        // ── Frame extraction loop ────────────────────────────────────────
        // In objc2 0.6, naturalSize() returns CGSize directly (not Option).
        let size = video_track.naturalSize();
        let _width = size.width as usize; // unused — CVPixelBuffer dimensions are used instead
        let _height = size.height as usize; // unused — CVPixelBuffer dimensions are used instead

        loop {
            // In objc2 0.6, copyNextSampleBuffer() returns Option<Retained<CMSampleBuffer>>.
            // When there are no more samples, it returns None. The method is unsafe.
            let sample_buffer = match reader_output.copyNextSampleBuffer() {
                Some(sb) => sb,
                None => break,
            };

            // Get the CVPixelBuffer from the sample buffer
            let sbuf_ptr: *mut c_void =
                Retained::as_ptr(&sample_buffer) as *mut c_void;

            let image_buffer = CMSampleBufferGetImageBuffer(sbuf_ptr);

            if !image_buffer.is_null() {
                CVPixelBufferLockBaseAddress(image_buffer, 1); // 1 = ReadOnly

                let base_address = CVPixelBufferGetBaseAddress(image_buffer);
                let width_val = CVPixelBufferGetWidth(image_buffer);
                let height_val = CVPixelBufferGetHeight(image_buffer);
                let bytes_per_row = CVPixelBufferGetBytesPerRow(image_buffer);

                if !base_address.is_null() {
                    let u8_ptr = base_address as *const u8;

                    if bytes_per_row == (width_val * 4) {
                        // Fast path: continuous, no padding
                        let total_bytes = bytes_per_row * height_val;
                        let slice = std::slice::from_raw_parts(u8_ptr, total_bytes);
                        out_file.write_all(slice).await
                            .expect("failed to write frame data");
                    } else {
                        // Slow path: strip row padding
                        for row in 0..height_val {
                            let row_offset = row * bytes_per_row;
                            let row_ptr = u8_ptr.add(row_offset);
                            let slice =
                                std::slice::from_raw_parts(row_ptr, width_val * 4);
                            out_file.write_all(slice).await
                                .expect("failed to write frame row");
                        }
                    }
                }

                CVPixelBufferUnlockBaseAddress(image_buffer, 1);
            }

            // Invalidate the sample buffer so its internal backing data is released early.
            // No CFRelease needed — Retained<T> handles ARC release automatically on drop.
            CMSampleBufferInvalidate(sbuf_ptr);
        }

        // Flush any buffered writes before we're done
        out_file.flush().await.expect("failed to flush output file");

        // Reader should have completed successfully
        assert!(
            reader.status() == AVAssetReaderStatus::Completed,
            "AVAssetReader did not complete successfully"
        );
    }
}