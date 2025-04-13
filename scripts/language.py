import json
import sys
import ssl


ssl._create_default_https_context = ssl._create_unverified_context


import faster_whisper
from faster_whisper.audio import decode_audio


def detect_language(audio):
    model = faster_whisper.WhisperModel("large-v2", compute_type="float32")

    audio_model = decode_audio(audio, sampling_rate=model.feature_extractor.sampling_rate)
    language, language_probability, all_language_probs = model.detect_language(audio_model)
    json_data = { 'language': language, 'langprob': language_probability }
    json.dump(json_data, sys.stdout, sort_keys=True, indent=4, allow_nan=True, ensure_ascii=False)


if __name__ == '__main__':
    detect_language(sys.argv[1])
