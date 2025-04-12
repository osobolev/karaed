import json
import logging
import os
import sys
import ssl


logging.getLogger("speechbrain.utils.torch_audio_backend").setLevel(logging.ERROR)
logging.getLogger("speechbrain.utils.train_logger").setLevel(logging.ERROR)

ssl._create_default_https_context = ssl._create_unverified_context


import torch
import whisperx


def align(audio, text_json, aligned_json):
    with open(text_json, "r", encoding="utf-8") as json_file:
        data = json.load(json_file)

    device = "cuda" if torch.cuda.is_available() else "cpu"
    language = data["language"]
    if language == "ru":
        model = "jonatasgrosman/wav2vec2-large-xlsr-53-russian"
    elif language == "sr":
        model = "dnikolic/wav2vec2-xlsr-530-serbian-colab"
    elif language == "en":
#        model = None #"WAV2VEC2_ASR_LARGE_LV60K_960H"
        model = "WAV2VEC2_ASR_LARGE_LV60K_960H"
    else:
        model = None
    align_model, align_metadata = whisperx.load_align_model(language, device, model_name=model)
    aligned = whisperx.align(data["segments"], align_model, align_metadata, audio, device, return_char_alignments=True)
    
    with open(aligned_json, "w", encoding="utf-8") as js_file:
        json.dump(aligned, js_file, sort_keys=True, indent=4, allow_nan=True, ensure_ascii=False)


if __name__ == '__main__':
    align(sys.argv[1], sys.argv[2], sys.argv[3])
