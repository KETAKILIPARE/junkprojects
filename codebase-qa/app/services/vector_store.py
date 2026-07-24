import json
import os
import re
import math
import numpy as np
from app.models.schemas import CodeChunk


class VectorStore:

    def __init__(self, persist_path: str = "./vector_store.json"):
        self._chunks: dict[str, CodeChunk] = {}
        self._vectors: dict[str, list[float]] = {}
        self._vocab: list[str] = []
        self._persist_path = persist_path

        if persist_path != ":memory:" and os.path.exists(persist_path):
            self._load()

    def add_chunks(self, chunks: list[CodeChunk]) -> None:
        new_chunks = [c for c in chunks if c.chunk_id not in self._chunks]
        if not new_chunks:
            return

        print(f"Indexing {len(new_chunks)} chunks...")

        # Add new chunks to store
        for chunk in new_chunks:
            self._chunks[chunk.chunk_id] = chunk

        # Rebuild TF-IDF vectors for all chunks
        self._rebuild_vectors()

        print(f"Done. Total chunks: {len(self._chunks)}")

        if self._persist_path != ":memory:":
            self._save()

    def search(self, query: str, top_k: int = 5) -> list[CodeChunk]:
        if not self._chunks:
            return []

        query_vector = self._tfidf_vector(self._tokenize(query), self._vocab)
        chunk_ids = list(self._vectors.keys())
        matrix = np.array([self._vectors[cid] for cid in chunk_ids])
        q = np.array(query_vector)

        norms = np.linalg.norm(matrix, axis=1) * np.linalg.norm(q)
        norms = np.where(norms == 0, 1e-10, norms)
        similarities = np.dot(matrix, q) / norms

        top_indices = np.argsort(similarities)[::-1][:top_k]
        return [self._chunks[chunk_ids[i]] for i in top_indices]

    def count(self) -> int:
        return len(self._chunks)

    def clear(self) -> None:
        self._chunks.clear()
        self._vectors.clear()
        self._vocab = []
        if self._persist_path != ":memory:" and os.path.exists(self._persist_path):
            os.remove(self._persist_path)

    def get_indexed_files(self) -> list[str]:
        return list({c.file_path for c in self._chunks.values()})

    def _tokenize(self, text: str) -> list[str]:
        text = text.lower()
        # Split on non-alphanumeric, keep meaningful tokens
        tokens = re.findall(r'[a-z][a-z0-9]*', text)
        return [t for t in tokens if len(t) > 1]

    def _rebuild_vectors(self) -> None:
        all_tokens = [self._tokenize(c.content) for c in self._chunks.values()]
        chunk_ids = list(self._chunks.keys())

        # Build vocabulary
        vocab_set = set()
        for tokens in all_tokens:
            vocab_set.update(tokens)
        self._vocab = sorted(vocab_set)

        # Compute IDF
        n = len(all_tokens)
        idf = {}
        for term in self._vocab:
            df = sum(1 for tokens in all_tokens if term in tokens)
            idf[term] = math.log((n + 1) / (df + 1)) + 1

        # Compute TF-IDF vectors
        for cid, tokens in zip(chunk_ids, all_tokens):
            vector = self._tfidf_vector(tokens, self._vocab, idf)
            self._vectors[cid] = vector

    def _tfidf_vector(self, tokens: list[str], vocab: list[str],
                      idf: dict = None) -> list[float]:
        tf = {}
        for t in tokens:
            tf[t] = tf.get(t, 0) + 1
        total = len(tokens) if tokens else 1
        vector = []
        for term in vocab:
            tf_val = tf.get(term, 0) / total
            idf_val = idf.get(term, 1) if idf else 1
            vector.append(tf_val * idf_val)
        return vector

    def _save(self) -> None:
        data = {
            "chunks": {cid: c.model_dump() for cid, c in self._chunks.items()},
            "vectors": self._vectors,
            "vocab": self._vocab,
        }
        with open(self._persist_path, "w") as f:
            json.dump(data, f)

    def _load(self) -> None:
        with open(self._persist_path, "r") as f:
            data = json.load(f)
        self._chunks = {cid: CodeChunk(**c) for cid, c in data["chunks"].items()}
        self._vectors = data["vectors"]
        self._vocab = data.get("vocab", [])
