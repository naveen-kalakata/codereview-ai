# CodeReview AI

AI-powered code review assistant built with Spring Boot and Spring AI. Integrates with GitHub PRs, uses RAG (Retrieval-Augmented Generation) for context-aware reviews against team coding standards, and supports real-time streaming responses.

## Features

- **Direct Code Review** — Submit code snippets and receive structured feedback (bugs, improvements, security issues, test suggestions)
- **GitHub PR Review** — Paste a PR URL and get a full review with diff analysis and file context
- **RAG-Enhanced Reviews** — Reviews are grounded in team coding standards using vector similarity search
- **Streaming Responses** — Real-time token-by-token output via Server-Sent Events (SSE)

## Tech Stack

- Java 17, Spring Boot 3.4.4
- Spring AI 1.1.4 with Google Gemini (gemini-2.5-flash)
- SimpleVectorStore for in-memory RAG
- Gemini Embedding API (gemini-embedding-001) for vector embeddings
- JUnit 5 + Mockito for testing
- Maven

## API Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| POST | `/review` | Review a code snippet | JSON (structured) |
| POST | `/review/stream` | Review with real-time streaming | SSE (text/event-stream) |
| POST | `/review/pr` | Review a GitHub Pull Request | JSON (structured) |

## Getting Started

### Prerequisites
- Java 17+
- A [Google AI Studio](https://aistudio.google.com/) API key

### Run Locally

```bash
# Clone the repo
git clone https://github.com/naveen-kalakata/codereview-ai.git
cd codereview-ai

# Set your Gemini API key
export GEMINI_API_KEY=your-api-key-here

# Run
./mvnw spring-boot:run
```

### Example Request

```bash
curl -X POST http://localhost:8080/review \
  -H "Content-Type: application/json" \
  -d '{"code": "public class Hello { public static void main(String[] args) { String password = \"admin123\"; } }"}'
```

### Example Response

```json
{
  "bugs": ["Hardcoded password will be compiled into bytecode"],
  "improvements": ["Use environment variables for sensitive data"],
  "security": ["Credential exposure — password visible in source code"],
  "tests": ["Test that password retrieval uses secure config source"]
}
```

## Architecture

```
Client Request
    │
    ▼
CodeReviewController ──── /review (JSON) or /review/stream (SSE)
    │
    ▼
CodeReviewService
    │
    ├── CodingStandardsService (RAG)
    │       │
    │       ├── VectorStore.similaritySearch() ── find relevant standards
    │       └── Returns top 3 matching standards
    │
    └── ChatClient
            │
            └── Gemini API ── generates the review
```

## Running Tests

```bash
./mvnw test
```

12 unit tests covering:
- Service layer (CodeReviewService, CodingStandardsService)
- Controller layer (MockMvc HTTP tests)
- Input validation and error handling
