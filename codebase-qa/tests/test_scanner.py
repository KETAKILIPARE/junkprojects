import os
import pytest
import tempfile
from app.services.scanner import FileScanner


@pytest.fixture
def temp_project(tmp_path):
    """Creates a temp project with various file types"""
    # Java file
    java_dir = tmp_path / "src" / "main" / "java"
    java_dir.mkdir(parents=True)
    (java_dir / "UserService.java").write_text(
        "public class UserService {\n"
        "    public User findById(Long id) {\n"
        "        return repository.findById(id);\n"
        "    }\n"
        "}\n"
    )

    # Python file
    (tmp_path / "utils.py").write_text(
        "def calculate_hash(data: str) -> str:\n"
        "    import hashlib\n"
        "    return hashlib.sha256(data.encode()).hexdigest()\n"
    )

    # Should be ignored
    node_modules = tmp_path / "node_modules" / "lodash"
    node_modules.mkdir(parents=True)
    (node_modules / "index.js").write_text("module.exports = {};")

    target_dir = tmp_path / "target" / "classes"
    target_dir.mkdir(parents=True)
    (target_dir / "UserService.class").write_text("binary")

    (tmp_path / ".git").mkdir()
    (tmp_path / ".git" / "config").write_text("[core]")

    # Unsupported file type — should be ignored
    (tmp_path / "README.md").write_text("# Project")

    return tmp_path


class TestFileScanner:

    def test_scan_finds_java_files(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        paths = [f.file_path for f in files]
        assert any("UserService.java" in p for p in paths)

    def test_scan_finds_python_files(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        paths = [f.file_path for f in files]
        assert any("utils.py" in p for p in paths)

    def test_scan_ignores_node_modules(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        paths = [f.file_path for f in files]
        assert not any("node_modules" + os.sep in p or p.endswith("node_modules") for p in paths)

    def test_scan_ignores_target_directory(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        paths = [f.file_path for f in files]
        assert not any(os.sep + "target" + os.sep in p for p in paths)

    def test_scan_ignores_git_directory(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        paths = [f.file_path for f in files]
        assert not any(".git" in p for p in paths)

    def test_scan_ignores_unsupported_file_types(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        paths = [f.file_path for f in files]
        assert not any("README.md" in p for p in paths)

    def test_scan_returns_correct_language(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        java_file = next(f for f in files if "UserService.java" in f.file_path)
        python_file = next(f for f in files if "utils.py" in f.file_path)
        assert java_file.language == "java"
        assert python_file.language == "python"

    def test_scan_raises_error_for_nonexistent_folder(self):
        scanner = FileScanner()
        with pytest.raises(ValueError, match="does not exist"):
            scanner.scan("/nonexistent/path")

    def test_scan_returns_file_content(self, temp_project):
        scanner = FileScanner()
        files = scanner.scan(str(temp_project))
        java_file = next(f for f in files if "UserService.java" in f.file_path)
        assert "UserService" in java_file.content
        assert "findById" in java_file.content
