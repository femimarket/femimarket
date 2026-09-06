use crate::api::Model;

// Hand-written (not derived) so the large base64 string fields — file / image /
// audio — log as a size instead of dumping their contents (see api_handler's
// `action` span field). Every other field prints exactly as the derive would.
impl std::fmt::Debug for Model {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Model::ZImageTurbo { prompt } => f
                .debug_struct("ZImageTurbo")
                .field("prompt", prompt)
                .finish(),
            Model::NanoBanana2 { prompt, fal_request_id, file } => f
                .debug_struct("NanoBanana2")
                .field("prompt", prompt)
                .field("fal_request_id", fal_request_id)
                .field("file", &format_args!("<{} bytes>", file.len()))
                .finish(),
            Model::Flux2Pro { prompt, fal_request_id, file } => f
                .debug_struct("Flux2Pro")
                .field("prompt", prompt)
                .field("fal_request_id", fal_request_id)
                .field("file", &format_args!("<{} bytes>", file.len()))
                .finish(),
            Model::Ltx2_3A2V { image, audio, prompt, comfy_request_id, file } => f
                .debug_struct("Ltx2_3A2V")
                .field("image", &format_args!("<{} bytes>", image.len()))
                .field("audio", &format_args!("<{} bytes>", audio.len()))
                .field("prompt", prompt)
                .field("comfy_request_id", comfy_request_id)
                .field("file", &format_args!("<{} bytes>", file.len()))
                .finish(),
            Model::Flux2KleinI2I { image, image2, prompt, comfy_request_id, file } => f
                .debug_struct("Flux2KleinI2I")
                .field("image", &format_args!("<{} bytes>", image.len()))
                .field("image2", &format_args!("<{} bytes>", image2.len()))
                .field("prompt", prompt)
                .field("comfy_request_id", comfy_request_id)
                .field("file", &format_args!("<{} bytes>", file.len()))
                .finish(),
            Model::Flux2DevI2I { image, prompt } => f
                .debug_struct("Flux2DevI2I")
                .field("image", &format_args!("<{} bytes>", image.len()))
                .field("prompt", prompt)
                .finish(),
            Model::ClaudeSonnet4_6 { messages } => f
                .debug_struct("ClaudeSonnet4_6")
                .field("messages", messages)
                .finish(),
            Model::Qwen3_6_35bA3b { messages } => f
                .debug_struct("Qwen3_6_35bA3b")
                .field("messages", messages)
                .finish(),
            Model::Qwen3AsrFlash { audio, lyrics } => f
                .debug_struct("Qwen3AsrFlash")
                .field("audio", &format_args!("<{} bytes>", audio.len()))
                .field("lyrics", lyrics)
                .finish(),
            Model::ForceAlignQwen306b { audio: audio, lyrics: lyrics, out_words: words } => f
                .debug_struct("Qwen3Asr0_6b")
                .field("audio", &format_args!("<{} bytes>", audio.len()))
                .field("lyrics", lyrics)
                .field("words", &format_args!("<{} words>", words.len()))
                .finish(),
            Model::Qwen3_6_35bA3b0GenMusicVideoPrompt { result } => f
                .debug_struct("Qwen3_6_35bA3b0GenMusicVideoPrompt")
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b1GenNewAngleFromXmpImagePrompt { xmp_prompt, result } => f
                .debug_struct("Qwen3_6_35bA3b1GenNewAngleFromXmpImagePrompt")
                .field("xmp_prompt", xmp_prompt)
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b2GenAugmentIdeaFromXmpPromptAndText { xmp_prompt, text, result } => f
                .debug_struct("Qwen3_6_35bA3b2GenAugmentIdeaFromXmpPromptAndText")
                .field("xmp_prompt", xmp_prompt)
                .field("text", text)
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts { xmp_prompt, xmp_prompt2, xmp_prompt3, result } => f
                .debug_struct("Qwen3_6_35bA3b3Gen50100WordMultishotTimestampedPromptFrom3XmpImagePrompts")
                .field("xmp_prompt", xmp_prompt)
                .field("xmp_prompt2", xmp_prompt2)
                .field("xmp_prompt3", xmp_prompt3)
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b2Gen50100WordMultishotTimestampedPromptFrom2XmpImagePrompts { xmp_prompt, xmp_prompt2, result } => f
                .debug_struct("Qwen3_6_35bA3b2Gen50100WordMultishotTimestampedPromptFrom2XmpImagePrompts")
                .field("xmp_prompt", xmp_prompt)
                .field("xmp_prompt2", xmp_prompt2)
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText { xmp_prompt, xmp_prompt2, xmp_prompt3, text, result } => f
                .debug_struct("Qwen3_6_35bA3b4GenAugmentIdeaFrom3XmpPromptsAndText")
                .field("xmp_prompt", xmp_prompt)
                .field("xmp_prompt2", xmp_prompt2)
                .field("xmp_prompt3", xmp_prompt3)
                .field("text", text)
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b1GenRandomIdeaFromXmpPrompt { xmp_prompt, result } => f
                .debug_struct("Qwen3_6_35bA3b1GenRandomIdeaFromXmpPrompt")
                .field("xmp_prompt", xmp_prompt)
                .field("result", result)
                .finish(),
            Model::Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray { xmp_prompt, result } => f
                .debug_struct("Qwen3_6_35bA3b1Gen3VariantsFromXmpPromptAsJsonarray")
                .field("xmp_prompt", xmp_prompt)
                .field("result", result)
                .finish(),
        }
    }
}
