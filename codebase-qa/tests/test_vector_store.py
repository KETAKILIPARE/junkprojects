import pytest
from app.models.schemas import CodeChunk
from app.services.vector_store import VectorStore


@pytest.fixture
def sample_chunks():
    return [
        CodeChunk(
            chunk_id="chunk_1",
            file_path="UserService.java",
            language="java",
            start_line=1,
            end_line=10,
            content="public User findById(Long id) { return repository.findById(id); }",
            context_name="findById"
        ),
        CodeChunk(
            chunk_id="chunk_2",
            file_path="AuthService.java",
            language="java",
            start_line=1,
            end_line=15,
            content="public String generateToken(String username) { return jwtUtil.generate(username); }",
            context_name="generateToken"
        ),
        CodeChunk(
            chunk_id="chunk_3",
            file_path="utils.py",
            language="python",
            start_line=1,
            end_line=5,
            content="def calculate_hash(data): return hashlib.sha256(data.encode()).hexdigest()",
            context_name="calculate_hash"
        ),
    ]


class TestVectorStore:

    def test_add_chunks_stores_all_chunks(self, sample_chunks):
        store = VectorStore(persist_path=":memory:")
        store.add_chunks(sample_chunks)
        assert store.count() == 3

    def test_add_chunks_is_idempotent(self, sample_chunks):
        store = VectorStore(persist_path=":memory:")
        store.add_chunks(sample_chunks)
        store.add_chunks(sample_chunks)
        assert store.count() == 3

    def test_search_returns_top_k_results(self, sample_chunks):
        store = VectorStore(persist_path=":memory:")
        store.add_chunks(sample_chunks)
        results = store.search("find user by id", top_k=2)
        assert len(results) <= 2

    def test_search_returns_code_chunks(self, sample_chunks):
        store = VectorStore(persist_path=":memory:")
        store.add_chunks(sample_chunks)
        results = store.search("authentication token", top_k=3)
        assert all(isinstance(r, CodeChunk) for r in results)

    def test_search_returns_empty_when_store_is_empty(self):
        store = VectorStore(persist_path=":memory:")
        results = store.search("anything", top_k=3)
        assert results == []

    def test_clear_removes_all_chunks(self, sample_chunks):
        store = VectorStore(persist_path=":memory:")
        store.add_chunks(sample_chunks)
        store.clear()
        assert store.count() == 0

    def test_get_indexed_files_returns_unique_file_paths(self, sample_chunks):
        store = VectorStore(persist_path=":memory:")
        store.add_chunks(sample_chunks)
        files = store.get_indexed_files()
        assert set(files) == {"UserService.java", "AuthService.java", "utils.py"}
