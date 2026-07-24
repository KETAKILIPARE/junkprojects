import pytest
from unittest.mock import MagicMock, patch
from app.models.schemas import CodeChunk
from app.services.qa_service import QAService


@pytest.fixture
def mock_vector_store():
    store = MagicMock()
    store.count.return_value = 3
    store.search.return_value = [
        CodeChunk(
            chunk_id="chunk_1",
            file_path="AuthService.java",
            language="java",
            start_line=5,
            end_line=15,
            content="public String generateToken(String username) { return jwtUtil.generate(username); }",
            context_name="generateToken"
        )
    ]
    store.get_indexed_files.return_value = ["AuthService.java"]
    return store


@pytest.fixture
def mock_llm():
    with patch("app.services.qa_service.OllamaLLM") as mock:
        llm = MagicMock()
        llm.invoke.return_value = "The JWT token is generated in AuthService.generateToken using jwtUtil."
        mock.return_value = llm
        yield mock


class TestQAService:

    def test_query_returns_answer_and_sources(self, mock_vector_store, mock_llm):
        service = QAService(vector_store=mock_vector_store)
        response = service.query("How is JWT token generated?", top_k=3)
        assert response.answer
        assert len(response.sources) > 0

    def test_query_sources_are_code_chunks(self, mock_vector_store, mock_llm):
        service = QAService(vector_store=mock_vector_store)
        response = service.query("How is JWT token generated?", top_k=3)
        assert all(isinstance(s, CodeChunk) for s in response.sources)

    def test_query_calls_vector_store_search(self, mock_vector_store, mock_llm):
        service = QAService(vector_store=mock_vector_store)
        service.query("How is JWT token generated?", top_k=5)
        mock_vector_store.search.assert_called_once_with("How is JWT token generated?", top_k=5)

    def test_query_returns_no_results_message_when_store_empty(self, mock_llm):
        empty_store = MagicMock()
        empty_store.count.return_value = 0
        empty_store.search.return_value = []
        service = QAService(vector_store=empty_store)
        response = service.query("anything", top_k=3)
        assert "no code" in response.answer.lower() or "not indexed" in response.answer.lower()
        assert response.sources == []

    def test_query_passes_retrieved_context_to_llm(self, mock_vector_store, mock_llm):
        service = QAService(vector_store=mock_vector_store)
        service.query("How is JWT token generated?", top_k=3)
        llm_instance = mock_llm.return_value
        call_args = llm_instance.invoke.call_args[0][0]
        assert "generateToken" in call_args or "AuthService" in call_args

    def test_query_includes_file_path_in_sources(self, mock_vector_store, mock_llm):
        service = QAService(vector_store=mock_vector_store)
        response = service.query("How is JWT token generated?", top_k=3)
        assert any("AuthService.java" in s.file_path for s in response.sources)
