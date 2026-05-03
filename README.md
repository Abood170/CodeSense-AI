<div align="center">

# 🔍 CodeSense AI

### AI-Powered Code Review Platform

[![Java](https://img.shields.io/badge/Java-24-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gemini AI](https://img.shields.io/badge/Gemini_2.5_Flash-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-22c55e?style=for-the-badge)](LICENSE)

**Paste code or a GitHub URL and get an instant, structured AI review — bugs, security issues, performance tips, and a fixed version, all in seconds.**

[Features](#-features) · [Tech Stack](#-tech-stack) · [Getting Started](#-getting-started) · [API Docs](#-api-endpoints) · [Contributing](#-contributing)

</div>

---

## ✨ Features

- 🤖 **AI-Powered Reviews** — Deep code analysis via Google Gemini 2.5 Flash, returning structured JSON with bugs, security flaws, performance issues, and suggestions
- 🔗 **GitHub Integration** — Paste any GitHub file URL and the app fetches and reviews the raw code automatically, with language auto-detection from the file extension
- 🔒 **JWT Authentication** — Secure register/login flow with BCrypt password hashing and stateless JWT sessions
- 📊 **Quality Score** — Animated circular score ring (0–10) with color-coded labels (Excellent → Critical Issues)
- 🗂️ **Review History** — Every review is persisted to PostgreSQL and accessible per-user through the History page
- 🌐 **12 Languages** — Java, Python, JavaScript, TypeScript, Kotlin, Go, Rust, C++, C#, PHP, Ruby, Swift
- 🎨 **Dark UI** — GitHub-inspired dark theme with collapsible result sections and a one-click fixed-code copy button
- ⚡ **Reactive HTTP** — Non-blocking WebClient for all outbound calls (Gemini API, GitHub raw content)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 24 |
| **Framework** | Spring Boot 4.0.6 |
| **AI Model** | Google Gemini 2.5 Flash |
| **Database** | PostgreSQL (Hibernate / Spring Data JPA) |
| **Security** | Spring Security 6 · JWT (JJWT 0.11.5) · BCrypt |
| **HTTP Client** | Spring WebFlux WebClient (reactive) |
| **Build Tool** | Gradle 9 |
| **Frontend** | Vanilla HTML/CSS/JS (single-page, no framework) |

---

## 📸 Screenshots

| Code Review | GitHub URL Tab | Review History |
|:-----------:|:--------------:|:--------------:|
| ![Code Review](docs/screenshots/code-review.png) | ![GitHub Tab](docs/screenshots/github-tab.png) | ![History](docs/screenshots/history.png) |

> _Add your own screenshots to `docs/screenshots/` to populate this section._

---

## 🚀 Getting Started

### Prerequisites

- **Java 24** (or later)
- **PostgreSQL 14+** running locally
- A **Google Gemini API key** — get one free at [aistudio.google.com](https://aistudio.google.com/)

### 1 — Clone the repository

```bash
git clone https://github.com/your-username/codesense-ai.git
cd codesense-ai
```

### 2 — Create the database

```sql
CREATE DATABASE code_reviewer;
```

> Tables are created automatically on first run via Hibernate `ddl-auto=update`.

### 3 — Configure environment variables

Copy the example properties file and fill in your values:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

See the [Environment Variables](#-environment-variables) section for all required keys.

### 4 — Run the application

```bash
./gradlew bootRun
```

The server starts on **http://localhost:8080**.

### 5 — Open in browser

Navigate to **http://localhost:8080** and create an account to start reviewing code.

---

## ⚙️ Environment Variables

All configuration lives in `src/main/resources/application.properties`.

| Property | Description | Example |
|---|---|---|
| `spring.datasource.url` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/code_reviewer` |
| `spring.datasource.username` | Database username | `postgres` |
| `spring.datasource.password` | Database password | `yourpassword` |
| `gemini.api.key` | Google Gemini API key | `AIzaSy...` |
| `gemini.api.url` | Gemini model endpoint | `https://generativelanguage.googleapis.com/...` |
| `jwt.secret` | HMAC-SHA256 signing secret (≥ 32 chars) | `MyS3cr3tK3y...` |
| `jwt.expiration` | Token validity in milliseconds | `86400000` _(24 hours)_ |

> ⚠️ **Never commit real secrets.** Add `application.properties` to `.gitignore` and use environment variable substitution or a secrets manager in production.

---

## 📡 API Endpoints

### Authentication

| Method | Endpoint | Body | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | `{ username, email, password }` | Create a new account, returns JWT |
| `POST` | `/api/auth/login` | `{ email, password }` | Login, returns JWT |

### Code Review

> All review endpoints require `Authorization: Bearer <token>` header.

| Method | Endpoint | Body | Description |
|--------|----------|------|-------------|
| `POST` | `/api/review` | `{ code, language }` | Review pasted code |
| `POST` | `/api/review/github` | `{ githubUrl }` | Fetch a GitHub file and review it |
| `GET`  | `/api/reviews` | — | Return the authenticated user's review history |

### Example — Review pasted code

```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "code": "public int divide(int a, int b) { return a / b; }",
    "language": "java"
  }'
```

### Example response

```json
{
  "summary": "Simple division method with no null-check or zero-division guard.",
  "bugs": ["Division by zero throws ArithmeticException when b = 0"],
  "security": [],
  "performance": [],
  "suggestions": ["Add input validation", "Consider returning Optional<Integer>"],
  "overallScore": 5.5,
  "fixedCode": "public int divide(int a, int b) {\n    if (b == 0) throw new IllegalArgumentException(\"Divisor cannot be zero\"); // FIXED: guard against division by zero\n    return a / b;\n}"
}
```

### Example — Review from GitHub

```bash
curl -X POST http://localhost:8080/api/review/github \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "githubUrl": "https://github.com/user/repo/blob/main/src/Main.java"
  }'
```

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** your changes: `git commit -m "Add amazing feature"`
4. **Push** to the branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

Please make sure your code compiles cleanly (`./gradlew build`) before submitting.

### Ideas for contributions

- [ ] OAuth2 / GitHub login
- [ ] Support for more languages (Scala, Dart, Elixir…)
- [ ] Export review as PDF or Markdown
- [ ] VS Code extension
- [ ] Rate limiting per user
- [ ] Webhook support for CI/CD pipelines

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Built with ☕ and 🤖 by [Abood170](https://github.com/Abood170)

⭐ Star this repo if you find it useful!

</div>
