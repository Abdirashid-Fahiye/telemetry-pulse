# TelemetryPulse: Real-Time Vehicle & IoT Telemetry Ingestion Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-blue.svg)](https://maven.apache.org/)
[![Cloud](https://img.shields.io/badge/AWS-SQS%20%7C%20DynamoDB%20%7C%20S3-232F3E.svg)](https://aws.amazon.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Ports%20%26%20Adapters-brightgreen.svg)]()
[![Testing](https://img.shields.io/badge/Methodology-TDD%20%28JUnit%205%20%2B%20Mockito%29-red.svg)]()

> A high-throughput, cloud-native telemetry ingestion engine engineered in Java 21 to process continuous streams of vehicle and IoT sensor data with fault tolerance, low-latency operational queries, and cost-optimized long-term archival.

---

## 🛑 The Problem: Traffic Bursts & Database Exhaustion

Modern connected vehicles and industrial IoT fleets generate continuous streams of high-frequency sensor data every second (e.g., GPS coordinates, speed, battery charge, engine temperatures).

Traditional web architectures—where an API server receives an HTTP request and immediately writes it directly to a relational database—fail under this load for two reasons:
1. **Traffic Bursts:** If 10,000 vehicles send data at the exact same millisecond, direct database connections saturate, causing connection timeouts, dropped data packets, and server crashes.
2. **Read/Write Asymmetry:** Writing every raw millisecond ping into an expensive relational database causes rapid disk exhaustion. Fleet managers need immediate access to a vehicle’s *current* status, but historical data only needs to be kept for cheap, long-term auditing.

---

## 💡 The Solution: Event-Driven Cloud Integration

**TelemetryPulse** solves this using an asynchronous, decoupled cloud pipeline. Instead of writing directly to a database, vehicles push data to an infinite cloud queue. A background Java worker processes this queue at a safe speed, routing urgent data to a fast database and old data to a cheap storage bucket.

### Cloud Components Used (What, How, & Why)

*   **AWS SQS (Simple Queue Service) — The Decoupling Buffer**
    *   **What it is:** A highly scalable message queue.
    *   **How we use it:** It acts as the entry point for all vehicle data.
    *   **Why we use it:** It acts as a shock absorber. Even if traffic spikes by 1,000%, SQS holds the messages safely. It ensures zero data loss and prevents the backend from crashing.
*   **AWS DynamoDB — The Hot Operational Store**
    *   **What it is:** A fully managed, serverless NoSQL database.
    *   **How we use it:** We store only the absolute latest status of each vehicle (overwriting the old status).
    *   **Why we use it:** It provides single-digit millisecond response times. When a dashboard requests a vehicle's current speed or battery level, DynamoDB delivers it instantly without executing slow SQL joins.
*   **AWS S3 (Simple Storage Service) — The Cold Storage Archive**
    *   **What it is:** Object storage built to store and retrieve any amount of data from anywhere.
    *   **How we use it:** We compress hundreds of raw telemetry pings into batched JSON files and save them to S3 folders partitioned by date.
    *   **Why we use it:** S3 is incredibly cheap compared to a database. It allows us to keep an infinite, permanent record of vehicle data for compliance and analytics without cluttering our fast DynamoDB tables.

For detailed UML class diagrams and sequence flows, refer to the [Architecture Wiki](../../wiki/System-Architecture-&-UML-Specification).

---

## 🏗️ Project Layout (Hexagonal Architecture)

The codebase strictly adheres to the Separation of Concerns principle. Business logic is completely isolated from AWS SDK code, making the system highly testable.

```text
src/
├── main/java/fahiye/telemetry/
│   ├── domain/            # Pure POJOs/Records & validation rules (Zero external dependencies)
│   ├── ports/             # Core interfaces (QueueConsumer, StateRepository, ArchiveStorage)
│   ├── service/           # Orchestration business logic & alert engines
│   └── adapters/          # Technical cloud implementations
│       ├── aws/           # AWS SDK v2 adapters (SQS, DynamoDB, S3)
│       └── simulator/     # IoT vehicle traffic generator
└── test/java/fahiye/telemetry/
    ├── domain/            # Unit tests for domain logic & range boundaries
    ├── service/           # Mockito unit tests for pipeline orchestration
    └── adapters/aws/      # Integration tests using LocalStack



🚀 Engineering Roadmap & Progress Tracking
[x] Initial Maven scaffolding (Java 21, AWS SDK v2 BOM, JUnit 5, Mockito).

[x] Project architecture design, UML modeling, and hexagonal layout.

[ ] Milestone 1: Domain record design & boundary unit testing (TDD).

[ ] Milestone 2: Service layer ingestion orchestration with Mockito.

[ ] Milestone 3: LocalStack cloud container integration tests (SQS/DynamoDB/S3).

[ ] Milestone 4: Live cloud verification & technical demo recording.



👤 Technical Author
Developer: Abdirashid Fahiye

Programme: WeThinkCode_ Software Engineering (Cloud Computing Elective Project)