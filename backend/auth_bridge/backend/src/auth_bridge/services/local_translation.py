"""CPU translation with an installed Argos English–Russian model."""
import json
from pathlib import Path
import re
from threading import Lock


class ArgosTranslator:
    def __init__(self, model_dir: str) -> None:
        import ctranslate2
        import sentencepiece

        root = Path(model_dir)
        metadata = json.loads((root / "metadata.json").read_text())
        if metadata.get("from_code") != "en" or metadata.get("to_code") != "ru":
            raise ValueError("An English–Russian model is required")
        self._prefix = metadata.get("target_prefix", "")
        self._tokenizer = sentencepiece.SentencePieceProcessor(model_file=str(root / "sentencepiece.model"))
        self._translator = ctranslate2.Translator(
            str(root / "model"), device="cpu", compute_type="int8", inter_threads=1, intra_threads=2,
        )
        self._lock = Lock()

    def translate(self, text: str) -> str:
        sentences = [part.strip() for part in re.split(r"(?<=[.!?])\s+|\n+", text) if part.strip()]
        if not sentences:
            return ""
        with self._lock:
            tokens = [self._tokenizer.encode(sentence, out_type=str) for sentence in sentences]
            results = self._translator.translate_batch(
                tokens, target_prefix=[[self._prefix]] * len(tokens) if self._prefix else None,
                beam_size=4, num_hypotheses=1, replace_unknowns=True,
                max_decoding_length=512, length_penalty=0.2,
            )
            translated = [self._tokenizer.decode(result.hypotheses[0]).removeprefix(self._prefix).strip()
                          for result in results]
        return " ".join(translated)
