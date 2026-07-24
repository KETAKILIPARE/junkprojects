import pytest
from app.services.chunker import CodeChunker
from app.models.schemas import CodeChunk


JAVA_CODE = """\
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User save(User user) {
        return repository.save(user);
    }
}
"""

PYTHON_CODE = """\
def calculate_hash(data: str) -> str:
    import hashlib
    return hashlib.sha256(data.encode()).hexdigest()


def validate_email(email: str) -> bool:
    return "@" in email and "." in email


class UserValidator:
    def validate(self, user: dict) -> bool:
        return validate_email(user.get("email", ""))
"""

SHORT_CODE = "x = 1\n"


class TestCodeChunker:

    def test_chunk_java_produces_chunks(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(JAVA_CODE, "UserService.java", "java")
        assert len(chunks) > 0

    def test_chunk_python_produces_chunks(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(PYTHON_CODE, "utils.py", "python")
        assert len(chunks) > 0

    def test_each_chunk_has_required_fields(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(JAVA_CODE, "UserService.java", "java")
        for chunk in chunks:
            assert chunk.chunk_id
            assert chunk.file_path == "UserService.java"
            assert chunk.language == "java"
            assert chunk.start_line >= 1
            assert chunk.end_line >= chunk.start_line
            assert chunk.content.strip()

    def test_chunk_ids_are_unique(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(JAVA_CODE, "UserService.java", "java")
        ids = [c.chunk_id for c in chunks]
        assert len(ids) == len(set(ids))

    def test_chunks_cover_all_lines(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(JAVA_CODE, "UserService.java", "java")
        total_lines = len(JAVA_CODE.splitlines())
        max_line = max(c.end_line for c in chunks)
        assert max_line <= total_lines + 1

    def test_chunk_detects_function_context_in_python(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(PYTHON_CODE, "utils.py", "python")
        context_names = [c.context_name for c in chunks if c.context_name]
        assert any("calculate_hash" in name for name in context_names)

    def test_chunk_detects_class_context_in_java(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(JAVA_CODE, "UserService.java", "java")
        context_names = [c.context_name for c in chunks if c.context_name]
        assert any("UserService" in name for name in context_names)

    def test_short_file_produces_at_least_one_chunk(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(SHORT_CODE, "tiny.py", "python")
        assert len(chunks) >= 1

    def test_chunk_content_is_not_empty(self):
        chunker = CodeChunker()
        chunks = chunker.chunk(JAVA_CODE, "UserService.java", "java")
        for chunk in chunks:
            assert len(chunk.content.strip()) > 0
