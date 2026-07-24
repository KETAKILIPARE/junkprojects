from pydantic import BaseModel
from typing import List, Optional


class IndexRequest(BaseModel):
    path: str


class QueryRequest(BaseModel):
    question: str


class QueryResponse(BaseModel):
    answer: str
    sources: List[dict]


class StatusResponse(BaseModel):
    indexed: bool
    chunk_count: int
    files: List[str]


class CodeChunk(BaseModel):
    file_path: str
    function_name: Optional[str]
    start_line: int
    end_line: int
    language: str
    content: str
