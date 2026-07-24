import os
from app.models.schemas import IndexResponse
from app.services.scanner import FileScanner
from app.services.chunker import CodeChunker
from app.services.vector_store import VectorStore

MAX_FILES = 15


class Indexer:

    def __init__(self, vector_store: VectorStore):
        self._scanner = FileScanner()
        self._chunker = CodeChunker()
        self._store = vector_store

    def index_folder(self, folder_path: str) -> IndexResponse:
        folder_path = os.path.normpath(folder_path.strip())
        files = self._scanner.scan(folder_path)
        files = files[:MAX_FILES]
        print(f"Indexing {len(files)} files...")

        all_chunks = []
        for i, scanned_file in enumerate(files):
            print(f"  Chunking file {i + 1}/{len(files)}: {scanned_file.file_path}")
            chunks = self._chunker.chunk(
                scanned_file.content,
                scanned_file.file_path,
                scanned_file.language
            )
            all_chunks.extend(chunks)

        print(f"Total chunks to embed: {len(all_chunks)}")
        self._store.add_chunks(all_chunks)

        return IndexResponse(
            indexed_files=len(files),
            total_chunks=self._store.count(),
            folder_path=folder_path
        )
