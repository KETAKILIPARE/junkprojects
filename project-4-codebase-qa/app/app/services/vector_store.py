import json
import os
import numpy as np
from typing import List, Tuple
from app.models.schemas import CodeChunk

VECTOR_STORE_PATH = "vector_store.json"


class VectorStore:
    def __init__(self):
        self.chunks: List[CodeChunk] = []
        self.embeddings: List[List[float]] = []

    def add(self, chunk: CodeChunk, embedding: List[float]):
        self.chunks.append(chunk)
        self.embeddings.append(embedding)

    def search(self, query_embedding: List[float], top_k: int = 5) -> List[Tuple[CodeChunk, float]]:
        if not self.embeddings:
            return []

        q = np.array(query_embedding)
        scores = []
        for i, emb in enumerate(self.embeddings):
            e = np.array(emb)
            score = float(np.dot(q, e) / (np.linalg.norm(q) * np.linalg.norm(e) + 1e-10))
            scores.append((i, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        return [(self.chunks[i], s) for i, s in scores[:top_k]]

    def save(self):
        data = {
            "chunks": [c.model_dump() for c in self.chunks],
            "embeddings": self.embeddings
        }
        with open(VECTOR_STORE_PATH, 'w') as f:
            json.dump(data, f)

    def load(self):
        if not os.path.exists(VECTOR_STORE_PATH):
            return
        with open(VECTOR_STORE_PATH, 'r') as f:
            data = json.load(f)
        self.chunks = [CodeChunk(**c) for c in data["chunks"]]
        self.embeddings = data["embeddings"]

    def clear(self):
        self.chunks = []
        self.embeddings = []
        if os.path.exists(VECTOR_STORE_PATH):
            os.remove(VECTOR_STORE_PATH)

    @property
    def size(self) -> int:
        return len(self.chunks)
