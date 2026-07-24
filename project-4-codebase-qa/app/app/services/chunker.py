import re
from typing import List
from app.models.schemas import CodeChunk


class CodeChunker:
    def chunk(self, file_path: str, content: str, language: str) -> List[CodeChunk]:
        if language in ('python',):
            return self._chunk_by_pattern(file_path, content, language, r'^(def |class )')
        elif language in ('java', 'javascript', 'typescript', 'go'):
            return self._chunk_by_pattern(file_path, content, language, r'(public |private |protected |func |function )')
        else:
            return self._chunk_fixed(file_path, content, language)

    def _chunk_by_pattern(self, file_path: str, content: str, language: str, pattern: str) -> List[CodeChunk]:
        lines = content.splitlines()
        chunks = []
        current_start = 0
        current_name = None

        for i, line in enumerate(lines):
            if re.match(pattern, line.strip()) and i > current_start:
                if current_name or i > 0:
                    chunks.append(CodeChunk(
                        file_path=file_path,
                        function_name=current_name,
                        start_line=current_start + 1,
                        end_line=i,
                        language=language,
                        content='\n'.join(lines[current_start:i])
                    ))
                current_start = i
                current_name = line.strip().split('(')[0].split()[-1]

        if current_start < len(lines):
            chunks.append(CodeChunk(
                file_path=file_path,
                function_name=current_name,
                start_line=current_start + 1,
                end_line=len(lines),
                language=language,
                content='\n'.join(lines[current_start:])
            ))

        return chunks if chunks else self._chunk_fixed(file_path, content, language)

    def _chunk_fixed(self, file_path: str, content: str, language: str, size: int = 50) -> List[CodeChunk]:
        lines = content.splitlines()
        chunks = []
        for i in range(0, len(lines), size):
            chunk_lines = lines[i:i + size]
            chunks.append(CodeChunk(
                file_path=file_path,
                function_name=None,
                start_line=i + 1,
                end_line=i + len(chunk_lines),
                language=language,
                content='\n'.join(chunk_lines)
            ))
        return chunks

    def detect_language(self, file_path: str) -> str:
        ext_map = {
            '.py': 'python', '.java': 'java', '.js': 'javascript',
            '.ts': 'typescript', '.jsx': 'javascript', '.tsx': 'typescript',
            '.go': 'go', '.rb': 'ruby', '.cs': 'csharp', '.cpp': 'cpp',
            '.c': 'c', '.rs': 'rust'
        }
        ext = '.' + file_path.rsplit('.', 1)[-1] if '.' in file_path else ''
        return ext_map.get(ext, 'unknown')
