# TelemetryPulse: Real-Time Vehicle & IoT Telemetry Ingestion Engine

![Java](https://img.shields.io/badge/Java-21-orange) ![Build](https://img.shields.io/badge/Build-Maven-blue) ![AWS](https://img.shields.io/badge/AWS-SQS%20%7C%20DynamoDB%20%7C%20S3-yellow) ![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Ports%20%26%20Adapters-brightgreen) ![Methodology](https://img.shields.io/badge/Methodology-TDD%20(JUnit%205%20%2B%20Mockito)-red)

A high-throughput, cloud-native telemetry ingestion engine engineered in Java 21 to process continuous streams of vehicle and IoT sensor data with fault tolerance, low-latency operational queries, and cost-optimized long-term archival.

## 🛑 The Problem: Traffic Bursts & Database Exhaustion
Modern connected vehicles and industrial IoT fleets generate continuous streams of high-frequency sensor data every second (e.g., GPS coordinates, speed, battery charge, engine temperatures).

Traditional web architectures—where an API server receives an HTTP request and immediately writes it directly to a relational database—fail under this load for two reasons:
1. **Traffic Bursts:** If 10,000 vehicles send data at the exact same millisecond, direct database connections saturate, causing connection timeouts, dropped data packets, and server crashes.
2. **Read/Write Asymmetry:** Writing every raw millisecond ping into an expensive relational database causes rapid disk exhaustion. Fleet managers need immediate access to a vehicle's *current* status, but historical data only needs to be kept for cheap, long-term auditing.

## 💡 The Solution: Event-Driven Cloud Integration
TelemetryPulse solves this using an asynchronous, decoupled cloud pipeline. Instead of writing directly to a database, vehicles push data to an infinite cloud queue. A background Java worker processes this queue at a safe speed, routing urgent data to a fast database and old data to a cheap storage bucket.

**Cloud Components Used (What, How, & Why)**
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

## 🏗️ Project Layout (Hexagonal Architecture)
The codebase strictly adheres to the Separation of Concerns principle. Business logic is completely isolated from AWS SDK code, making the system highly testable.

```text
src/main/java/fahiye/telemetry/
├── domain/                  # Core Business Rules (Zero external dependencies)
│   ├── TelemetryPayload     # Record: Validates incoming vehicle JSON data constraints.
│   └── VehicleState         # Record: Represents the current operational health of a vehicle.
├── ports/                   # The "Job Descriptions" (Interfaces)
│   ├── QueueConsumer        # Contract for fetching data from a queue.
│   ├── StateRepository      # Contract for saving hot data.
│   └── ArchiveStorage       # Contract for archiving cold data.
├── service/                 # The Orchestrator
│   └── TelemetryIngestionService # Core pipeline logic: Fetches, converts, and routes data.
├── adapters/aws/            # The "Employees" (Technical Implementations)
│   ├── SqsQueueConsumer     # AWS SDK implementation of QueueConsumer.
│   ├── DynamoDbStateRepository # AWS SDK implementation of StateRepository.
│   ├── S3ArchiveStorage     # AWS SDK implementation of ArchiveStorage.
│   └── simulator/
│       └── IotTrafficSimulator   # Standalone utility to blast fake vehicle data into SQS.
└── Application              # The main entry point featuring the continuous polling loop.

🚀 Engineering Roadmap & Progress Tracking
[x] Initial Maven scaffolding (Java 21, AWS SDK v2 BOM, JUnit 5, Mockito).

[x] Project architecture design, UML modeling, and hexagonal layout.

[x] Milestone 1: Domain record design & boundary unit testing (TDD).

[x] Milestone 2: Service layer ingestion orchestration with Mockito.

[x] Milestone 3: LocalStack cloud container integration tests (SQS/DynamoDB/S3).

[x] Milestone 4: Application entry point & executable Fat JAR packaging.

[x] Milestone 5: IoT telemetry traffic generator & live pipeline verification.

⚙️ How to Run Locally (Windows & Linux)
This project utilizes LocalStack version 3.0.0 to emulate AWS offline. The infrastructure is entirely containerized and designed to run cross-platform on both Windows and Linux without excessive memory consumption.

Prerequisites
Windows/macOS: Install Docker Desktop and ensure the application is running in the background.

Linux: Install docker and docker-compose via your package manager. Ensure the Docker daemon is active (sudo systemctl start docker).

1. Start the Local AWS Cloud
Open your terminal at the root of the project and boot the offline cloud:

Bash
docker-compose pull
docker-compose up -d
Note: Wait approximately 10 to 15 seconds for the internal AWS services (SQS, DynamoDB, S3) to fully initialize on port 4566 before executing the application.

2. Package the Application
Build the executable Fat JAR containing all AWS SDK dependencies:

Bash
mvn clean package
3. Execute the Ingestion Engine
Start the continuous background worker to poll the AWS SQS queue for incoming telemetry data:

Bash
java -jar target/telemetry-pulse-1.0-SNAPSHOT.jar
4. Simulate Live IoT Traffic
To verify the engine is working, open a secondary terminal (or use your IDE) and run the traffic simulator. This will blast 10 randomized vehicle payloads directly into the local SQS queue, which the engine will instantly process and archive.

Bash
java -cp target/telemetry-pulse-1.0-SNAPSHOT.jar fahiye.telemetry.adapters.aws.simulator.IotTrafficSimulator


👤 Technical Author
Developer: Abdirashid Fahiye

Programme: WeThinkCode_ Software Engineering (Cloud Computing Elective Project)

📹 Demo Video Link : https://youtu.be/63zhEypNsDM

🔒 WeThinkCode_ Verification
Verification Code: WTC-KWYQBXQ6