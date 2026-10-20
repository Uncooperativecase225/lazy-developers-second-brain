# Lazy Developer's Second Brain

A local-first developer memory application that helps you resume work on a Git repository without having to reconstruct everything from memory. It uses **React + Vite**, **Spring Boot**, **MySQL**, and your local **Qwen 3.5 9B model through Ollama**.

> **Status:** Initial implementation. The frontend and backend source are included. Run the checks listed below in your environment before relying on it for important work. This is designed for a single-user local workstation, not for direct public-internet exposure.

## What it does

- **Register a repository:** Add a local Git repository by its absolute path. The app does not copy, move, or delete repository files.
- **Inspect recent activity:** View branch, current commit, tracked working-tree status, changed-file list, diff summary, and recent commits.
- **Resume my work:** Ask Qwen to summarize recent activity, identify likely completed and pending work, and suggest one next action. The result is saved as a session snapshot.
- **Persistent session memory:** Browse the latest 30 saved summaries for a project.
- **Task tracking:** Create tasks manually, update status, and remove tasks. Pending items from successful AI summaries are added as *suggestions* and are not treated as verified facts.
- **Ask your project:** Ask a question grounded in recent Git activity, session summaries, and saved memory.
- **Local AI:** The backend calls Ollama on the same machine. No external AI API is configured.

### Current scope and limitations

- Session capture is user-triggered; this version does **not** monitor editors or automatically detect when a coding session ends.
- Git context is read-only and focused on commits and tracked working-tree changes. Untracked files are intentionally excluded from the Git status summary, and large diffs are truncated before being sent to the model.
- AI output can be wrong or incomplete. Verify suggestions against the repository before acting on them.
- The application stores project paths, summaries, tasks, and memory in MySQL. Repository source files are not copied into MySQL.
- The application does not execute model-proposed shell commands or edit source files.
- Authentication is not included because the initial target is a single-user application bound to loopback. Do not expose the backend or database to a LAN or the internet as-is.

## Architecture

```text
React + Vite (127.0.0.1:5173)
          |
          | HTTP / JSON
          v
Spring Boot REST API (127.0.0.1:8080)
   |           |             |
   |           |             +--> Git CLI (read-only commands in registered repositories)
   |           +------------------> Ollama --> Qwen 3.5 9B (127.0.0.1:11434)
   +------------------------------> MySQL 8 (127.0.0.1:3306)
```

Flyway manages the schema. Hibernate uses `ddl-auto=validate`; it does not create or silently alter tables.

## Requirements

Install these before starting the application:

1. **Java 21** (JDK, not only a JRE).
2. **Maven 3.9+**.
3. **Node.js 20.19+ or 22.12+** and npm.
4. **Git**, available on `PATH`.
5. **MySQL 8.0+** (MySQL 8.4 is the recommended version) or Docker Desktop to run the included MySQL container.
6. **Ollama**, installed and running, with your Qwen 3.5 9B model available locally.

Check the tools:

```bash
java -version
mvn -version
node --version
npm --version
git --version
ollama list
```

The model tag in the default configuration is `qwen3.5:9b`. The value must match the tag shown by `ollama list`. If your installed tag is different, set `OLLAMA_MODEL` accordingly.

## 1. Get the code ready

Extract or clone this repository, then open a terminal in the project root (the directory containing this README).

Create your local environment file:

**Windows PowerShell**

```powershell
Copy-Item .env.example .env
Copy-Item frontend/.env.example frontend/.env
```

**macOS / Linux / Git Bash**

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Edit `.env` and change the example MySQL passwords. Keep `MYSQL_PASSWORD` and `DB_PASSWORD` equal. These are local development credentials; do not commit `.env`.

The root `.env` is read by Docker Compose. Spring Boot also imports `../.env` when launched with `backend/` as the working directory. The frontend reads `frontend/.env` through Vite.

## 2. Start MySQL

### Option A — Docker Desktop (recommended)

Make sure Docker Desktop is running, then from the project root:

```bash
docker compose up -d mysql
docker compose ps
```

Wait until the MySQL container reports `healthy`.

The database port is bound to `127.0.0.1` only. The persistent volume is named `lazybrain_mysql_data`.

To stop MySQL without deleting data:

```bash
docker compose stop mysql
```

To stop and remove the container (the named volume remains):

```bash
docker compose down
```

To **permanently delete the local database**, including its saved sessions and tasks, run `docker compose down -v`. This cannot be undone.

### Option B — Existing local MySQL

Create a database and a dedicated user using your MySQL administration tool or a privileged MySQL account. Example SQL (replace the password):

```sql
CREATE DATABASE lazybrain CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'lazybrain'@'localhost' IDENTIFIED BY 'change-this-local-password';
GRANT ALL PRIVILEGES ON lazybrain.* TO 'lazybrain'@'localhost';
FLUSH PRIVILEGES;
```

Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` in the environment or `.env` file. Flyway applies the initial schema when the backend starts.

## 3. Verify Ollama and Qwen

Start Ollama using your normal operating-system method. On systems where Ollama is not already running as a service, run:

```bash
ollama serve
```

In a second terminal, verify the model:

```bash
ollama list
ollama run qwen3.5:9b
```

If the model tag shown by `ollama list` differs, update `OLLAMA_MODEL` in the root `.env`. Do not pull the model again if it is already installed under the correct tag. Exit the interactive model prompt when the test is complete.

The backend calls Ollama at `http://localhost:11434` by default. If Ollama is hosted elsewhere on your machine, change `OLLAMA_BASE_URL`.

## 4. Start the Spring Boot backend

Open a terminal and change into the backend directory. Running from this directory lets Spring Boot import the root `.env` file.

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

The backend listens on `http://localhost:8080`. On startup it connects to MySQL and Flyway applies database migrations. Hibernate validates the schema.

If you use an IDE, import `backend/pom.xml` as a Maven project and run `com.lazybrain.LazyBrainApplication`. Set the required environment variables in the IDE run configuration if you do not want to use the root `.env` file.

Useful URLs:

- Health: `http://localhost:8080/actuator/health`
- API status: `http://localhost:8080/api/system/status`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`

The status endpoint reports whether the database and Ollama API are reachable. An Ollama connection does not by itself guarantee that the configured model tag exists; verify the tag with `ollama list`.

## 5. Start the React frontend

Open a second terminal from the project root:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

The Vite development server is bound to loopback. The frontend API base defaults to `http://localhost:8080/api`. To change it, edit `frontend/.env`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

Restart Vite after changing environment variables.

## 6. First-run workflow

1. Confirm MySQL, Ollama, and the Spring Boot backend are running.
2. Open the frontend in your browser.
3. Choose **+** beside Workspace or **Add your first repository**.
4. Enter a project name and an absolute path to a local Git repository. The folder must already exist and contain a Git work tree.
5. Select the repository in the sidebar.
6. Review recent activity and working-tree status.
7. Click **Resume my work**. The backend collects recent commits, tracked status and diff context, then asks Qwen to create a concise summary.
8. Review the suggested next action and pending tasks. Treat AI-generated tasks as suggestions, not verified facts.
9. Use **Session memory** to revisit saved snapshots, **Tasks** to manage work, and **Ask your project** to query the available context.

If AI inference fails, the resume endpoint saves a fallback session and marks `aiGenerated` as `false`. Check that Ollama is running, that the model tag is correct, and that enough RAM/VRAM is available.

## API overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/status` | Backend, MySQL and Ollama status |
| `GET` | `/api/projects` | List registered repositories |
| `POST` | `/api/projects` | Register a local Git repository |
| `GET` | `/api/projects/{id}` | Read project details |
| `DELETE` | `/api/projects/{id}` | Remove project metadata and associated app data; does not delete repository files |
| `GET` | `/api/projects/{id}/activity` | Read recent Git activity |
| `POST` | `/api/projects/{id}/resume` | Generate and persist a session snapshot |
| `GET` | `/api/projects/{id}/sessions` | Read recent saved sessions |
| `GET` | `/api/projects/{id}/tasks` | List tasks |
| `POST` | `/api/projects/{id}/tasks` | Create a task |
| `PATCH` | `/api/projects/{id}/tasks/{taskId}` | Update task fields/status |
| `DELETE` | `/api/projects/{id}/tasks/{taskId}` | Delete a task |
| `POST` | `/api/projects/{id}/ask` | Ask a question about available project context |

Interactive API documentation is available at `/swagger-ui.html` while the backend is running.

## Configuration reference

| Variable | Default | Meaning |
|---|---|---|
| `MYSQL_DATABASE` | `lazybrain` | Database created by Docker Compose |
| `MYSQL_USER` | `lazybrain` | MySQL app user created by Compose |
| `MYSQL_PASSWORD` | example value | MySQL app-user password for Compose |
| `MYSQL_ROOT_PASSWORD` | example value | MySQL root password for Compose |
| `DB_HOST` | `localhost` | MySQL host used by Spring Boot |
| `DB_PORT` | `3306` | MySQL port used by Spring Boot |
| `DB_NAME` | `lazybrain` | Database name used by Spring Boot |
| `DB_USER` | `lazybrain` | MySQL username used by Spring Boot |
| `DB_PASSWORD` | example value | MySQL password used by Spring Boot |
| `SERVER_PORT` | `8080` | Backend port; bind address remains loopback |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama API base URL |
| `OLLAMA_MODEL` | `qwen3.5:9b` | Exact installed Ollama model tag |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Single allowed frontend origin for CORS |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Frontend API base URL |

## Build and verification commands

Frontend production build:

```bash
cd frontend
npm install
npm run lint
npm test
npm run build
```

Backend tests and package:

```bash
cd backend
mvn clean verify
```

Serve the built frontend locally for a smoke test:

```bash
npm run preview
```

The frontend production output is written to `frontend/dist/`. This source bundle does not include a generated `package-lock.json` because the build environment could not reach the npm registry; run `npm install` once, then commit the generated lockfile and use `npm ci` for reproducible installs. A local production build of the frontend does not turn this into a hardened multi-user web service: the backend intentionally binds to loopback and the initial version has no user authentication.

## Troubleshooting

### Backend cannot connect to MySQL

- Confirm the MySQL service/container is running and healthy.
- Check `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`.
- Make sure `DB_PASSWORD` matches the account password, and the database/user exist.
- Inspect backend startup logs for Flyway or Hibernate schema-validation errors.

### Ollama is connected but Resume my work uses fallback mode

- Run `ollama list` and confirm the configured model tag exactly matches.
- Run `ollama run qwen3.5:9b` directly to verify the model loads.
- Check that `OLLAMA_BASE_URL` is reachable from the backend process.
- A 9B model may use system RAM in addition to VRAM; close other GPU-heavy applications and try a shorter context if inference is too slow.

### Repository registration fails

- Use an absolute path, not a relative path.
- Make sure the directory exists and is a Git repository (`git status` should work there).
- Ensure the account running the backend has read access to that directory and Git is on `PATH`.
- Paths longer than 700 characters are rejected to stay within the indexed MySQL path column limit.

### Browser reports a CORS error

- Start Vite at `http://localhost:5173` (the default allowed origin).
- If you use another origin, set `FRONTEND_ORIGIN` and restart the backend.
- Do not expose the backend on a public interface.

### Flyway migration errors

Do not edit a migration that has already been applied to a database. Create a new uniquely numbered migration under `backend/src/main/resources/db/migration/`. For a disposable development database, only reset it if you are comfortable losing all saved project sessions and tasks.

## Security and privacy notes

- The backend binds to `127.0.0.1`, and Docker Compose binds MySQL to `127.0.0.1`.
- Repository paths are canonicalized, checked for an existing Git work tree, and stored after validation. Git commands use argument arrays rather than shell-string interpolation and have a time limit and bounded captured output.
- Git operations are read-only. The app never runs commands proposed by Qwen.
- Source diffs and repository metadata are sent only to the configured local Ollama endpoint by this implementation. Do not change the Ollama URL to a remote host unless you intentionally accept that data flow.
- This is not a hardened multi-user deployment. Before exposing it beyond the local machine, add authentication/authorization, CSRF and origin review, TLS, secrets management, audit logging, rate limits, and stricter filesystem sandboxing.

## License

MIT. See [`LICENSE`](LICENSE).
