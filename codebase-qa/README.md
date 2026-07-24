# Codebase Q&A Assistant

Ask questions about any codebase in plain English. Built with RAG (Retrieval-Augmented Generation) — indexes your code, finds the most relevant chunks, and answers using only what's actually in your codebase.

## How it works

1. **Index** — scans a project folder recursively, chunks code by function/class boundaries, builds a TF-IDF vector index
2. **Query** — embeds your question, retrieves the most semantically relevant code chunks, sends them as context to an LLM
3. **Answer** — LLM answers using only the retrieved code, cites exact file names and line numbers

## Stack

- **FastAPI** — REST API
- **TF-IDF + cosine similarity** — vector search (no external vector DB needed)
- **Groq API** — LLM inference (llama-3.1-8b-instant, free tier)
- **Python 3.12**

## Setup

```bash
# Create and activate venv
python -m venv venv
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Add your Groq API key to .env
echo GROQ_API_KEY=your_key_here > .env
```

Get a free Groq API key at https://console.groq.com

## Run

```bash
uvicorn app.main:app --port 8000
```

Open http://localhost:8000

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /index | Index a folder |
| POST | /query | Ask a question |
| GET | /status | Index stats |
| DELETE | /index | Clear index |

## Example questions

- "How does JWT authentication work?"
- "Where are tasks created and validated?"
- "How is rate limiting implemented?"
- "What happens when a user is not a workspace member?"
- "Explain the RBAC system"

## Supported languages

Java, Python, JavaScript, TypeScript, Go, Rust, C#, C++, C, Ruby, Kotlin, Scala, PHP, Swift

## Testing

```bash
python -m pytest -v -p no:respx
```

39 tests — scanner, chunker, vector store, QA service, API endpoints.
