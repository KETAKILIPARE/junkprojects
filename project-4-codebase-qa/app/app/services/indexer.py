import os
from typing import List
from app.services.scanner import FileScanner
from app.services.chunker import CodeChunker
from app.services.vector_store import VectorStore
from langchain_ollama import OllamaEmbeddings

_store = VectorStore()
_scanner = FileScanner()
_chunker = CodeChunker()
_embeddings = OllamaEmbeddings(model="nomic-embed-text")


class Indexer:
    def index(self, folder_path: str) -> dict:
        _store.clear()
        files = _scanner.scan(folder_path)

        for file_path in files:
            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                language = _chunker.detect_language(file_path)
                chunks = _chunker.chunk(file_path, content, language)
                for chunk in chunks:
                    embedding = _embeddings.embed_query(chunk.content)
                    _store.add(chunk, embedding)
            except Exception:
                continue

        _store.save()
        return {"indexed_files": len(files), "total_chunks": _store.size}

    def status(self) -> dict:
        files = list({c.file_path for c in _store.chunks})
        return {"indexed": _store.size > 0, "chunk_count": _store.size, "files": files}

    def clear(self):
        _store.clear()


def get_store() -> VectorStore:
    return _store
