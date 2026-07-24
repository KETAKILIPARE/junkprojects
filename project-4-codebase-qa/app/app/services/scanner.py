import os
from typing import List

SUPPORTED_EXTENSIONS = {'.java', '.py', '.js', '.ts', '.jsx', '.tsx', '.go', '.rb', '.cs', '.cpp', '.c', '.rs'}
IGNORE_DIRS = {'node_modules', 'target', '.git', '__pycache__', 'dist', 'build', '.venv', 'venv', '.idea', '.vscode'}


class FileScanner:
    def scan(self, folder_path: str) -> List[str]:
        if not os.path.isdir(folder_path):
            raise ValueError(f"Not a valid directory: {folder_path}")

        files = []
        for root, dirs, filenames in os.walk(folder_path):
            dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]
            for filename in filenames:
                if any(filename.endswith(ext) for ext in SUPPORTED_EXTENSIONS):
                    files.append(os.path.join(root, filename))
        return files
