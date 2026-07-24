from pydantic import BaseModel, Field
from typing import Optional


class CodeChunk(BaseModel):
    chunk_id: str
    file_path: str
    language: str
    start_line: int
    end_line: int
    content: str
    context_name: Optional[str] = None  # function or class name if detected


class IndexRequest(BaseModel):
    folder_path: str


class IndexResponse(BaseModel):
    indexed_files: int
    total_chunks: int
    folder_path: str


class QueryRequest(BaseModel):
    question: str
    top_k: int = Field(default=8, ge=1, le=20)


class QueryResponse(BaseModel):
    answer: str
    sources: list[CodeChunk]


class StatusResponse(BaseModel):
    total_chunks: int
    indexed_files: list[str]
    is_ready: bool
