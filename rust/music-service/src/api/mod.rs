mod comfyui;
mod fal;
pub mod handler;
mod handle_z_image_turbo_fal;
mod handle_nano_banana2_fal;
mod handle_flux2_pro_fal;
mod handle_ltx2_3a2v_comfyui;
mod handle_flux2_klein_i2i_comfyui;
mod handle_claude_sonnet4_6_ngrok;
mod impl_debug;
mod handle_flux2_dev_i2i_comfyui;
mod alibaba;
mod handle_qwen3_asr_flash_alibaba;
pub mod handle_qwen3_asr_0_6b;
mod handle_qwen3_6_35b_a3b;
mod handle_qwen3_6_35b_a3b_0gen_music_video_prompt;
mod handle_qwen3_6_35b_a3b_1gen_new_angle_from_xmp_image_prompt;
mod handle_qwen3_6_35b_a3b_2gen_augment_idea_from_xmp_prompt_and_text;
mod handle_qwen3_6_35b_a3b_3gen_50_100_word_multishot_timestamped_prompt_from_3_xmp_image_prompts;
mod handle_qwen3_6_35b_a3b_2gen_50_100_word_multishot_timestamped_prompt_from_2_xmp_image_prompts;
mod handle_qwen3_6_35b_a3b_4gen_augment_idea_from_3_xmp_prompts_and_text;
mod handle_qwen3_6_35b_a3b_1gen_random_idea_from_xmp_prompt;
mod handle_qwen3_6_35b_a3b_1gen_3_variants_from_xmp_prompt_as_jsonarray;
mod upload_handler;

use serde::*;
use utoipa::ToSchema;
use uuid::Uuid;
use crate::api::handle_qwen3_asr_0_6b::WordAlignment;


#[derive(Clone, PartialEq, Deserialize, Serialize, ToSchema)]
pub enum Model {
    ZImageTurbo {
        prompt: String,
    },
    NanoBanana2 {
        prompt: String,
        fal_request_id: String,
        /// base64 return
        file: String,
    },
    Flux2Pro {
        prompt: String,
        fal_request_id: String,
        /// base64 return
        file: String,
    },
    Ltx2_3A2V {
        /// input image as base64 — data URI (web) or raw base64 (android/ios), empty if unused; type detected server-side
        image: String,
        /// input audio as base64 — data URI (web) or raw base64 (android/ios), empty if unused; type detected server-side
        audio: String,
        prompt: String,
        comfy_request_id: String,
        /// base64 return
        file: String,
    },
    Flux2KleinI2I {
        /// input subject image as base64 — data URI (web) or raw base64 (android/ios); type detected server-side
        image: String,
        /// reference image as base64 — data URI (web) or raw base64 (android/ios); type detected server-side
        image2: String,
        prompt: String,
        comfy_request_id: String,
        /// base64 return
        file: String,
    },
    Flux2DevI2I {
        /// input image as base64 — data URI (web) or raw base64 (android/ios); type detected server-side
        image: String,
        prompt: String,
    },
    ClaudeSonnet4_6 {
        messages: Vec<ApiChatMessage>,
    },
    Qwen3_6_35bA3b {
        messages: Vec<ApiChatMessage>,
    },
    Qwen3AsrFlash {
        /// input audio as base64 — a real container (mp3/m4a/wav); data URI (web) or raw base64 (android/ios)
        audio: String,
        /// transcribed lyrics (return)
        lyrics: String,
    },
    ForceAlignQwen306b {
        /// input audio as base64 — 16 kHz mono f32, raw little-endian samples; data URI (web) or raw base64 (android/ios)
        audio: String,
        /// transcribed lyrics (return)
        lyrics: String,
        /// per-word timestamps from forced alignment (return)
        #[serde(default)]
        out_words: Vec<WordAlignment>,
    },
    Qwen3_6_35bA3b0GenMusicVideoPrompt {
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b1GenNewAngleFromXmpImagePrompt {
        xmp_prompt: String,
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b2GenAugmentIdeaFromXmpPromptAndText {
        xmp_prompt: String,
        text: String,
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts {
        xmp_prompt: String,
        xmp_prompt2: String,
        xmp_prompt3: String,
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b2Gen50100WordMultishotTimestampedPromptFrom2XmpImagePrompts {
        xmp_prompt: String,
        xmp_prompt2: String,
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText {
        xmp_prompt: String,
        xmp_prompt2: String,
        xmp_prompt3: String,
        text: String,
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b1GenRandomIdeaFromXmpPrompt {
        xmp_prompt: String,
        /// generated prompt (return)
        result: String,
    },
    Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray {
        xmp_prompt: String,
        /// generated prompts as a JSON array of 3 strings (return)
        result: String,
    },
}

impl Default for Model {
    fn default() -> Self { Model::ZImageTurbo { prompt: "".to_string() } }
}


#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, ToSchema)]
pub enum ApiChatRole {
    User,
    Assistant,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, ToSchema)]
pub struct ApiChatMessage {
    pub role: ApiChatRole,
    pub content: String,
}

// #[derive(Clone, Debug, PartialEq, Deserialize, Serialize, Default, ToSchema)]
// /// default values
// pub struct Model {
//     /// uuid v7 preferred
//     pub id: Uuid,
//     pub user_id: String,
//     pub action: ApiAction,
// }
