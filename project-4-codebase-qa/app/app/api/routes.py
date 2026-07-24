from fastapi import APIRouter, HTTPException
from app.models.schemas import IndexRequest, QueryRequest, QueryResponse, StatusResponse
from app.services.indexer import Indexer
from app.services.qa_service import QAService

router = APIRouter()
indexer = Indexer()
qa_service = QAService()


@router.post("/index")
def index_codebase(request: IndexRequest):
    try:
        result = indexer.index(request.path)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/query", response_model=QueryResponse)
def query_codebase(request: QueryRequest):
    try:
        return qa_service.query(request.question)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/status", response_model=StatusResponse)
def get_status():
    return indexer.status()


@router.delete("/index")
def delete_index():
    indexer.clear()
    return {"message": "Index cleared"}
