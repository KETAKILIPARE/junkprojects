import os
from dataclasses import dataclass

SUPPORTED_EXTENSIONS = {
    ".java": "java",
    ".py": "python",
    ".js": "javascript",
    ".ts": "typescript",
    ".jsx": "javascript",
    ".tsx": "typescript",
    ".go": "go",
    ".rs": "rust",
    ".cs": "csharp",
    ".cpp": "cpp",
    ".c": "c",
    ".rb": "ruby",
    ".kt": "kotlin",
    ".scala": "scala",
    ".php": "php",
    ".swift": "swift",
}

IGNORED_DIRS = {
    "node_modules", "target", ".git", "__pycache__",
    ".idea", ".vscode", "dist", "build", "out",
    ".gradle", "vendor", "venv", ".env", "coverage",
}


@dataclass
class ScannedFile:
    file_path: str
    language: str
    content: str


def _is_ignored(path: str) -> bool:
    parts = path.replace("\\", "/").split("/")
    return any(part.lower() in {d.lower() for d in IGNORED_DIRS} for part in parts)


class FileScanner:

    def scan(self, folder_path: str) -> list[ScannedFile]:
        if not os.path.exists(folder_path):
            raise ValueError(f"Folder does not exist: {folder_path}")

        results = []
        for root, dirs, files in os.walk(folder_path):
            dirs[:] = [d for d in dirs if d.lower() not in {ig.lower() for ig in IGNORED_DIRS}]

            if _is_ignored(root):
                continue

            for filename in files:
                ext = os.path.splitext(filename)[1].lower()
                if ext not in SUPPORTED_EXTENSIONS:
                    continue

                file_path = os.path.join(root, filename)

                if _is_ignored(file_path):
                    continue

                try:
                    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                        content = f.read()
                    results.append(ScannedFile(
                        file_path=file_path,
                        language=SUPPORTED_EXTENSIONS[ext],
                        content=content,
                    ))
                except (IOError, OSError):
                    continue

        return results
