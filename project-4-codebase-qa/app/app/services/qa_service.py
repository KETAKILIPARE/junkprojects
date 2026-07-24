from app.services.indexer import get_store
from app.models.schemas import QueryResponse
from langchain_ollama import OllamaEmbeddings, OllamaLLM

_embeddings = OllamaEmbeddings(model="nomic-embed-text")
_llm = OllamaLLM(model="llama3.2")


class QAService:
    def query(self, question: str) -> QueryResponse:
        store = get_store()
        query_embedding = _embeddings.embed_query(question)
        results = store.search(query_embedding, top_k=5)

        if not results:
            return QueryResponse(answer="No code has been indexed yet.", sources=[])

        context = "\n\n".join([
            f"File: {chunk.file_path} (lines {chunk.start_line}-{chunk.end_line})\n{chunk.content}"
            for chunk, _ in results
        ])

        prompt = f"""You are a code assistant. Answer the question using ONLY the provided code context.
If the answer is not in the context, say so.

Context:
{context}

Question: {question}

Answer:"""

        answer = _llm.invoke(prompt)

        sources = [
            {"file": chunk.file_path, "function": chunk.function_name,
             "start_line": chunk.start_line, "end_line": chunk.end_line}
            for chunk, _ in results
        ]

        return QueryResponse(answer=answer, sources=sources)
