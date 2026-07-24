import re
import hashlib
from app.models.schemas import CodeChunk

MAX_CHUNK_LINES = 60
OVERLAP_LINES = 5


class CodeChunker:

    def chunk(self, content: str, file_path: str, language: str) -> list[CodeChunk]:
        lines = content.splitlines()
        if not lines:
            return []

        boundaries = self._detect_boundaries(lines, language)

        if not boundaries:
            return self._chunk_by_lines(lines, file_path, language)

        return self._chunk_by_boundaries(lines, boundaries, file_path, language)

    def _detect_boundaries(self, lines: list[str], language: str) -> list[tuple[int, str]]:
        """Returns list of (line_index, context_name) for function/class starts."""
        boundaries = []

        if language in ("java", "kotlin", "scala", "csharp", "cpp", "c"):
            patterns = [
                r'^\s*(?:public|private|protected|static|final|abstract|override)[\w\s<>\[\]]*\s+(\w+)\s*\(',
                r'^\s*(?:public|private|protected)?\s*(?:class|interface|enum)\s+(\w+)',
            ]
        elif language == "python":
            patterns = [
                r'^\s*(?:async\s+)?def\s+(\w+)\s*\(',
                r'^\s*class\s+(\w+)',
            ]
        elif language in ("javascript", "typescript"):
            patterns = [
                r'^\s*(?:export\s+)?(?:async\s+)?function\s+(\w+)',
                r'^\s*(?:export\s+)?class\s+(\w+)',
                r'^\s*(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?\(',
                r'^\s*(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?function',
            ]
        else:
            patterns = []

        for i, line in enumerate(lines):
            for pattern in patterns:
                match = re.match(pattern, line)
                if match:
                    boundaries.append((i, match.group(1)))
                    break

        return boundaries

    def _chunk_by_boundaries(self, lines: list[str], boundaries: list[tuple[int, str]],
                              file_path: str, language: str) -> list[CodeChunk]:
        chunks = []
        for idx, (start, context_name) in enumerate(boundaries):
            end = boundaries[idx + 1][0] - 1 if idx + 1 < len(boundaries) else len(lines) - 1

            # Split large chunks
            if end - start > MAX_CHUNK_LINES:
                sub_chunks = self._split_large_chunk(
                    lines, start, end, file_path, language, context_name)
                chunks.extend(sub_chunks)
            else:
                content = "\n".join(lines[start:end + 1])
                if content.strip():
                    chunks.append(self._make_chunk(
                        content, file_path, language, start + 1, end + 1, context_name))

        return chunks if chunks else self._chunk_by_lines(lines, file_path, language)

    def _split_large_chunk(self, lines: list[str], start: int, end: int,
                            file_path: str, language: str, context_name: str) -> list[CodeChunk]:
        chunks = []
        current = start
        while current <= end:
            chunk_end = min(current + MAX_CHUNK_LINES - 1, end)
            content = "\n".join(lines[current:chunk_end + 1])
            if content.strip():
                chunks.append(self._make_chunk(
                    content, file_path, language, current + 1, chunk_end + 1, context_name))
            current = chunk_end + 1 - OVERLAP_LINES
            if current <= start:
                break
        return chunks

    def _chunk_by_lines(self, lines: list[str], file_path: str, language: str) -> list[CodeChunk]:
        chunks = []
        for i in range(0, len(lines), MAX_CHUNK_LINES - OVERLAP_LINES):
            end = min(i + MAX_CHUNK_LINES, len(lines))
            content = "\n".join(lines[i:end])
            if content.strip():
                chunks.append(self._make_chunk(
                    content, file_path, language, i + 1, end, None))
        return chunks

    def _make_chunk(self, content: str, file_path: str, language: str,
                    start_line: int, end_line: int, context_name: str) -> CodeChunk:
        chunk_id = hashlib.md5(
            f"{file_path}:{start_line}:{end_line}:{content[:50]}".encode()
        ).hexdigest()
        return CodeChunk(
            chunk_id=chunk_id,
            file_path=file_path,
            language=language,
            start_line=start_line,
            end_line=end_line,
            content=content,
            context_name=context_name,
        )
