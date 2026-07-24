import os
import httpx
from pathlib import Path
from dotenv import load_dotenv
from groq import Groq
from app.models.schemas import CodeChunk, QueryResponse
from app.services.vector_store import VectorStore

load_dotenv(dotenv_path=Path(__file__).parent.parent.parent / ".env")

GROQ_MODEL = "llama-3.1-8b-instant"

PROMPT_TEMPLATE = """\
You are a code assistant analyzing a Java Spring Boot codebase.
Answer the question based on the code context below.
Even if the context is partial, explain what you can see.
Only say you cannot find the answer if the context has absolutely nothing relevant.

CODE CONTEXT:
{context}

QUESTION: {question}

ANSWER (be specific, reference the actual class and method names you see in the context):"""


class QAService:

    def __init__(self, vector_store: VectorStore):
        self._store = vector_store
        api_key = os.environ.get("GROQ_API_KEY")
        http_client = httpx.Client(verify=False, trust_env=False)
        self._client = Groq(api_key=api_key, http_client=http_client)

    def query(self, question: str, top_k: int = 5) -> QueryResponse:
        if self._store.count() == 0:
            return QueryResponse(
                answer="No code has been indexed yet. Please index a project folder first.",
                sources=[]
            )

        chunks = self._store.search(question, top_k=top_k)

        if not chunks:
            return QueryResponse(
                answer="I couldn't find relevant code for this question in the indexed codebase.",
                sources=[]
            )

        context = self._build_context(chunks)
        prompt = PROMPT_TEMPLATE.format(context=context, question=question)

        response = self._client.chat.completions.create(
            model=GROQ_MODEL,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
        )

        answer = response.choices[0].message.content
        return QueryResponse(answer=answer, sources=chunks)

    def _build_context(self, chunks: list[CodeChunk]) -> str:
        parts = []
        for chunk in chunks:
            header = f"File: {chunk.file_path.split(chr(92))[-1]}"
            if chunk.context_name:
                header += f" | Function/Class: {chunk.context_name}"
            header += f" | Lines {chunk.start_line}-{chunk.end_line}"
            parts.append(f"{header}\n```{chunk.language}\n{chunk.content}\n```")
        return "\n\n---\n\n".join(parts)
