Ollama AI Task Manager
This project is a robust Spring Boot application designed to manage tasks with the assistance of an integrated Large Language Model (LLM). It leverages a distributed architecture using Docker Compose, providing persistence, caching, and local LLM inference capabilities.

✨ Features
Intelligent Task Management: Utilizes a Large Language Model (LLM) to potentially assist with task creation, categorization, summarization, or other intelligent operations.

High Performance Caching: Integrates Redis to cache LLM responses and other frequently accessed data, drastically reducing latency and Ollama API calls for repetitive queries.

Scalable Data Storage: Employs PostgreSQL as a reliable and scalable relational database for persistent task data.

Local LLM Inference: Runs Ollama locally via Docker, enabling private, cost-effective, and low-latency interaction with open-source LLMs like Gemma.

Containerized Environment: Fully orchestrated using Docker Compose, simplifying setup, deployment, and ensuring a consistent development and production environment.

🚀 Technologies Used
Backend: Spring Boot (Java)

Database: PostgreSQL

Caching: Redis

Large Language Model (LLM) Runtime: Ollama

LLM Model: Gemma (specifically gemma3:latest as used in development)

Containerization: Docker & Docker Compose

HTTP Client: Spring WebFlux's WebClient

⚙️ Setup and Running Locally (with Docker Compose)
To get this project up and running on your local machine, follow these steps:

Clone the Repository:

Bash

git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
Ensure Docker is Running:
Make sure Docker Desktop (or your Docker daemon) is running on your system.

Pull the Ollama Model:
Before starting the services, ensure Ollama has the necessary model. For this project, we primarily use gemma3:latest.

Bash

docker compose up -d ollama # Start just the Ollama service in detached mode
docker exec -it taskmanager-ollama-1 ollama pull gemma3:latest # Pull the model inside the container
# Wait for the pull to complete (it can take several minutes for larger models)
docker compose down ollama # Stop the ollama service if you only started it for the pull
(Note: taskmanager-ollama-1 is the default service name based on your docker-compose.yml. Adjust if yours is different.)

Build the Spring Boot Application:
Navigate to the root of your Spring Boot project (where pom.xml is located) and build the JAR:

Bash

mvn clean install -DskipTests # -DskipTests is optional, but useful if tests are not yet fully configured for Docker environment
Start the Entire Stack:
From the root of your project where docker-compose.yml resides:

Bash

docker compose up -d --build
This command will:

Build the app (Spring Boot) service Docker image.

Start the postgres, redis, and ollama services.

Link them together as defined in docker-compose.yml.

Verify Services:
Check that all services are running:

Bash

docker compose ps
You should see Up status for app, postgres, redis, and ollama.

🛠️ Configuration
The application's configuration is primarily managed via application.properties and environment variables in docker-compose.yml. Key configurations include:

PostgreSQL Connection:

spring.datasource.url=jdbc:postgresql://postgres:5432/taskdb

spring.datasource.username=user

spring.datasource.password=password

Redis Connection & Caching:

spring.redis.host=redis

spring.redis.port=6379

spring.cache.redis.time-to-live=<duration_in_ms> (e.g., 300000 for 5 minutes)

Custom RedisCacheConfiguration in AgenticAiTaskManagerApplication.java for specific cache TTLs.

Ollama Integration:

ollama.api.url=http://ollama:11434

ollama.model=gemma3:latest (or your desired model)

💡 Usage
(Add instructions on how to use your application, e.g., API endpoints)

Access the API: Your Spring Boot application should be accessible on http://localhost:8080.

Example Endpoint:

POST /api/generate-task (or whatever your endpoint is for LLM interaction)

Example Request Body: {"prompt": "Tell me a joke?"}

🔮 Future Enhancements
Implement more sophisticated LLM interactions (e.g., chained prompts, function calling).

Add user authentication and authorization.

Develop a frontend UI (e.g., React, Angular, Vue.js).

Integrate monitoring and logging tools.

Optimize Docker images for production deployment.