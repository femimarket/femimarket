use crate::api::{handle_claude_sonnet4_6_ngrok, handle_qwen3_6_35b_a3b,
                 handle_qwen3_6_35b_a3b_0gen_music_video_prompt,
                 handle_qwen3_6_35b_a3b_1gen_new_angle_from_xmp_image_prompt,
                 handle_qwen3_6_35b_a3b_2gen_augment_idea_from_xmp_prompt_and_text,
                 handle_qwen3_6_35b_a3b_3gen_50_100_word_multishot_timestamped_prompt_from_3_xmp_image_prompts,
                 handle_qwen3_6_35b_a3b_2gen_50_100_word_multishot_timestamped_prompt_from_2_xmp_image_prompts,
                 handle_qwen3_6_35b_a3b_4gen_augment_idea_from_3_xmp_prompts_and_text,
                 handle_qwen3_6_35b_a3b_1gen_random_idea_from_xmp_prompt,
                 handle_qwen3_6_35b_a3b_1gen_3_variants_from_xmp_prompt_as_jsonarray, handle_flux2_pro_fal, handle_ltx2_3a2v_comfyui, handle_flux2_klein_i2i_comfyui, handle_flux2_dev_i2i_comfyui, handle_qwen3_asr_flash_alibaba, handle_qwen3_asr_0_6b, handle_nano_banana2_fal, handle_z_image_turbo_fal, Model};
use crate::upload_handler;
use crate::api::upload_handler::*;

#[
tracing::instrument(
    skip_all,
    fields(
                user=tracing::field::Empty,
                status=tracing::field::Empty,
                message=tracing::field::Empty,
                action = ?req,
                file=tracing::field::Empty,
                prompt=tracing::field::Empty,
                reply=tracing::field::Empty,
    ),
)
]
pub async fn api_handler(
    req: Model,
) -> Result<impl axum::response::IntoResponse, (axum::http::StatusCode, String)> {
    match req {
        // Model::ClaudeSonnet4_6 { .. } => {
        //     handle_claude_sonnet4_6_ngrok::handle_claude_sonnet4_6(req).await
        // }
        // Model::Qwen3_6_35bA3b { .. } => {
        //     handle_qwen3_6_35b_a3b::handle_qwen3_6_35b_a3b(req).await
        // }
        // Model::Qwen3_6_35bA3b0GenMusicVideoPrompt { .. } => {
        //     handle_qwen3_6_35b_a3b_0gen_music_video_prompt::handle_qwen3_6_35b_a3b_0gen_music_video_prompt(req).await
        // }
        // Model::Qwen3_6_35bA3b1GenNewAngleFromXmpImagePrompt { .. } => {
        //     handle_qwen3_6_35b_a3b_1gen_new_angle_from_xmp_image_prompt::handle_qwen3_6_35b_a3b_1gen_new_angle_from_xmp_image_prompt(req).await
        // }
        // Model::Qwen3_6_35bA3b2GenAugmentIdeaFromXmpPromptAndText { .. } => {
        //     handle_qwen3_6_35b_a3b_2gen_augment_idea_from_xmp_prompt_and_text::handle_qwen3_6_35b_a3b_2gen_augment_idea_from_xmp_prompt_and_text(req).await
        // }
        // Model::Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts { .. } => {
        //     handle_qwen3_6_35b_a3b_3gen_50_100_word_multishot_timestamped_prompt_from_3_xmp_image_prompts::handle_qwen3_6_35b_a3b_3gen_50_100_word_multishot_timestamped_prompt_from_3_xmp_image_prompts(req).await
        // }
        // Model::Qwen3_6_35bA3b2Gen50100WordMultishotTimestampedPromptFrom2XmpImagePrompts { .. } => {
        //     handle_qwen3_6_35b_a3b_2gen_50_100_word_multishot_timestamped_prompt_from_2_xmp_image_prompts::handle_qwen3_6_35b_a3b_2gen_50_100_word_multishot_timestamped_prompt_from_2_xmp_image_prompts(req).await
        // }
        // Model::Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText { .. } => {
        //     handle_qwen3_6_35b_a3b_4gen_augment_idea_from_3_xmp_prompts_and_text::handle_qwen3_6_35b_a3b_4gen_augment_idea_from_3_xmp_prompts_and_text(req).await
        // }
        // Model::Qwen3_6_35bA3b1GenRandomIdeaFromXmpPrompt { .. } => {
        //     handle_qwen3_6_35b_a3b_1gen_random_idea_from_xmp_prompt::handle_qwen3_6_35b_a3b_1gen_random_idea_from_xmp_prompt(req).await
        // }
        // Model::Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray { .. } => {
        //     handle_qwen3_6_35b_a3b_1gen_3_variants_from_xmp_prompt_as_jsonarray::handle_qwen3_6_35b_a3b_1gen_3_variants_from_xmp_prompt_as_jsonarray(req).await
        // }
        Model::ZImageTurbo { .. } => {
            handle_z_image_turbo_fal::handle_z_image_turbo(req).await
        }
        // Model::NanoBanana2 { .. } => {
        //     handle_nano_banana2_fal::handle_nano_banana2(req).await
        // }
        // Model::Flux2Pro { .. } => {
        //     handle_flux2_pro_fal::handle_flux2_pro(req).await
        // }
        // Model::Ltx2_3A2V { .. } => {
        //     handle_ltx2_3a2v_comfyui::handle_ltx2_3a2v(req).await
        // }
        // Model::Flux2KleinI2I { .. } => {
        //     handle_flux2_klein_i2i_comfyui::handle_flux2_klein_i2i(req).await
        // }
        Model::Flux2DevI2I { .. } => {
            handle_flux2_dev_i2i_comfyui::handle_flux2_dev_i2i(req).await
        }
        // Model::Qwen3AsrFlash { .. } => {
        //     handle_qwen3_asr_flash_alibaba::handle_qwen3_asr_flash(req).await
        // }
        Model::ForceAlignQwen306b { .. } => {
            handle_qwen3_asr_0_6b::handle_qwen3_asr_0_6b(req).await
        }
        _ => {
            unimplemented!()
        }
    }
}


#[utoipa::path(
    post,
    path = "/",
    request_body(content_type = "multipart/form-data", content = Model),
    responses(
        (status = 200, description = "Upload successful", body = Model),
        (status = 500, description = "Internal server error", body = String)
    )
)]
pub async fn api(
    _headers: axum::http::HeaderMap,
    multipart: axum::extract::Multipart,
) -> impl axum::response::IntoResponse {
    let v = handle_form::<Model>(multipart, &["z_image_turbo_file"])
        .await
        .map_err(|err| {
            tracing::error!("{}: {}", err.0, err.1);
            (axum::http::StatusCode::INTERNAL_SERVER_ERROR, err.1)
        })?;
    let response_data = match api_handler(v).await {
        Ok(result) => result,
        Err(err) => {
            tracing::error!("{}: {}", err.0, err.1);
            return Err((axum::http::StatusCode::INTERNAL_SERVER_ERROR, err.1));
        }
    };
    Ok(response_data)
}



// upload_handler!(
//     api,
//     Model,
//     Model,
//     { },
//     ["audio"],
//     api_handler
// );