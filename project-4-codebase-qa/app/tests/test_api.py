import pytest
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient
from app.models.schemas import CodeChunk, IndexResponse, QueryResponse, StatusResponse


@pytest.fixture
def mock_indexer():
    with patch("app.api.routes.indexer") as mock:
        mock.index_folder.return_value = IndexResponse(
            indexed_files=5,
            total_chunks=42,
            folder_path="/some/project"
        )
        yield mock


@pytest.fixture
def mock_qa():
    with patch("app.api.routes.qa_service") as mock:
        mock.query.return_value = QueryResponse(
            answer="JWT is generated in AuthService using jwtUtil.",
            sources=[
                CodeChunk(
                    chunk_id="chunk_1",
                    file_path="AuthService.java",
                    language="java",
                    start_line=5,
                    end_line=15,
                    content="public String generateToken(String username) {}",
                    context_name="generateToken"
                )
            ]
        )
        yield mock


@pytest.fixture
def mock_store():
    with patch("app.api.routes.vector_store") as mock:
        mock.count.return_value = 42
        mock.get_indexed_files.return_value = ["AuthService.java", "UserService.java"]
        yield mock


@pytest.fixture
def client(mock_indexer, mock_qa, mock_store):
    from app.main import app
    return TestClient(app)


class TestAPIRoutes:

    def test_index_returns_201_with_stats(self, client):
        response = client.post("/index", json={"folder_path": "/some/project"})
        assert response.status_code == 201
        data = response.json()
        assert data["indexed_files"] == 5
        assert data["total_chunks"] == 42

    def test_index_returns_400_for_empty_path(self, client):
        response = client.post("/index", json={"folder_path": ""})
        assert response.status_code == 400

    def test_query_returns_200_with_answer_and_sources(self, client):
        response = client.post("/query", json={"question": "How is JWT generated?"})
        assert response.status_code == 200
        data = response.json()
        assert "answer" in data
        assert "sources" in data
        assert len(data["sources"]) > 0

    def test_query_returns_400_for_empty_question(self, client):
        response = client.post("/query", json={"question": ""})
        assert response.status_code == 400

    def test_status_returns_index_info(self, client):
        response = client.get("/status")
        assert response.status_code == 200
        data = response.json()
        assert data["total_chunks"] == 42
        assert "AuthService.java" in data["indexed_files"]
        assert data["is_ready"] is True

    def test_status_returns_not_ready_when_empty(self, client, mock_store):
        mock_store.count.return_value = 0
        mock_store.get_indexed_files.return_value = []
        response = client.get("/status")
        assert response.status_code == 200
        assert response.json()["is_ready"] is False

    def test_delete_index_returns_204(self, client):
        response = client.delete("/index")
        assert response.status_code == 204

    def test_query_respects_top_k(self, client, mock_qa):
        client.post("/query", json={"question": "How is auth done?", "top_k": 3})
        mock_qa.query.assert_called_once_with("How is auth done?", top_k=3)
