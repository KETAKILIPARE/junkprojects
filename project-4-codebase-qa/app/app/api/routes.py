from fastapi import APIRouter, HTTPException
import os
from app.models.schemas import IndexRequest, IndexResponse, QueryRequest, QueryResponse, StatusResponse
from app.services.vector_store import VectorStore
from app.services.indexer import Indexer
from app.services.qa_service import QAService

router = APIRouter()

vector_store = VectorStore(persist_path="./vector_store.json")
indexer = Indexer(vector_store=vector_store)
qa_service = QAService(vector_store=vector_store)


@router.post("/index", response_model=IndexResponse, status_code=201)
def index_folder(request: IndexRequest):
    if not request.folder_path:
        raise HTTPException(status_code=400, detail="folder_path cannot be empty")
    try:
        return indexer.index_folder(request.folder_path)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/query", response_model=QueryResponse)
def query(request: QueryRequest):
    if not request.question.strip():
        raise HTTPException(status_code=400, detail="question cannot be empty")
    return qa_service.query(request.question, top_k=request.top_k)


@router.get("/status", response_model=StatusResponse)
def status():
    count = vector_store.count()
    return StatusResponse(
        total_chunks=count,
        indexed_files=vector_store.get_indexed_files(),
        is_ready=count > 0
    )


@router.delete("/index", status_code=204)
def delete_index():
    vector_store.clear()
