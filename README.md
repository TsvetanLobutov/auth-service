# 🐾 Pet Shop Microservices

A production-grade, event-driven e-commerce platform built with **Java Spring Boot**, **React**, **Kafka**, and **Kubernetes**. This project demonstrates a full-stack, polyglot persistence architecture with secure JWT authentication and asynchronous messaging.

## 🏗️ Architecture Overview

The system is built using a **Microservices Architecture**, routed through a central **API Gateway**. It uses **Polyglot Persistence** (PostgreSQL for transactional data, MongoDB for document data) and **Event-Driven Design** with Apache Kafka.

### 🧩 The Services

| Service | Port | Description |
| :--- | :--- | :--- |
| **React Frontend** | `30080` | Single Page Application (SPA). Handles UI, stores JWT in `localStorage`, and attaches it to secure requests. |
| **API Gateway** | `8081` | Single entry point. Handles CORS, routes traffic, and verifies JWT tokens for secure endpoints (`/api/orders`). |
| **Auth Service** | `8083` | Manages user registration and login. Hashes passwords with **BCrypt** and generates **JWT** tokens. Connected to `authdb` (PostgreSQL). |
| **Product Service** | `8080` | Serves the product catalog. Acts as a **Kafka Consumer** to automatically reduce stock when an order is placed. Connected to `postgres` (PostgreSQL). |
| **Order Service** | `8082` | Processes purchases. Saves orders and acts as a **Kafka Producer**, publishing `order-topic` events. Connected to `orderdb` (PostgreSQL). |
| **Review Service** | `8084` | Manages product reviews. Demonstrates **NoSQL** usage by storing nested tags and comments in `review_db` (MongoDB). |
| **Kafka** | `9092` | Message broker connecting the Order Service (Producer) and Product Service (Consumer). |

---

## 🛠️ Technology Stack

### Frontend
*   **React.js** (Vite)
*   **Nginx** (for serving static production files)

### Backend & Spring Ecosystem
*   **Java 21** & **Spring Boot 4.1.2**
*   **Spring Cloud Gateway** (API Routing)
*   **Spring Security** & **JJWT** (Authentication & Authorization)
*   **Spring Data JPA** (PostgreSQL)
*   **Spring Data MongoDB** (NoSQL)
*   **Spring Kafka** (Event Streaming)
*   **Lombok** (Boilerplate reduction)

### Databases & Messaging
*   **PostgreSQL** (Relational Data)
*   **MongoDB** (Document Data)
*   **Apache Kafka** (KRaft mode - no Zookeeper)

### DevOps & Orchestration
*   **Docker** & **Docker Compose**
*   **Kubernetes** (Minikube)
*   **Docker Hub** (Container Registry)

---

## 🚀 Key Features & Learnings

This project was a deep dive into distributed systems and cloud-native development. Key technical challenges solved include:

*   **Polyglot Persistence:** Using SQL for strict transactional integrity (Orders/Auth) and NoSQL for flexible document schemas (Reviews).
*   **Event-Driven Architecture:** Decoupling the Order and Product services using Kafka to handle asynchronous stock updates.
*   **Security:** End-to-end JWT authentication. The Gateway acts as a reverse proxy that validates tokens before allowing requests to reach the Order Service.
*   **Kubernetes Orchestration:** Containerizing 7+ services and managing deployments, services, and networking within a local K8s cluster.
*   **Troubleshooting:** Navigated complex issues including WSL2 networking, Docker ghost caches, CORS preflight errors, and Kubernetes DNS caching.

---

## 🏃‍♂️ How to Run Locally

*(Assuming you have Docker, Kubernetes, and Minikube installed)*

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/TsvetanLobutov/shop-frontend.git
    cd shop-infrastructure

    Start the infrastructure (Databases & Kafka):

**2. Start Minikube**
 
```bash
minikube start
```
 
**3. Start the infrastructure (databases and Kafka)**
 
```bash
kubectl apply -f k8s/infrastructure.yaml
```
 
**4. Deploy the microservices**
 
```bash
kubectl apply -f k8s/services.yaml
```
 
**5. Deploy the frontend**
 
```bash
kubectl apply -f k8s/frontend.yaml
```
 
**6. Open the app**
 
```bash
minikube service shop-frontend
```
 
---
 
## 🛠️ Troubleshooting
 
**Kafka starts but nothing can connect (`Connection to node -1 could not be established`)**
 
When running Kafka in KRaft mode inside a single pod, the controller quorum must point to `localhost`, not to the Kubernetes Service:
 
```yaml
- name: KAFKA_CONTROLLER_QUORUM_VOTERS
  value: "1@localhost:9093"
```
 
Make sure this variable appears only once in the Kafka deployment. After Kafka logs `Kafka Server started`, restart the order service:
 
```bash
kubectl rollout restart deployment order-service
```
 
---
 
## 👤 Author
 
**Tsvetan Lobutov**
 
- [GitHub Profile](https://github.com/TsvetanLobutov)
- Full-Stack Microservices Developer
