.PHONY: help build test clean run docker-up docker-down deploy

help:
	@echo "ClinicOS Development Commands"
	@echo "=============================="
	@echo "make build          - Build all modules with Maven"
	@echo "make test           - Run all tests"
	@echo "make clean          - Clean build artifacts"
	@echo "make run            - Run all services (requires separate terminals)"
	@echo "make docker-up      - Start services with Docker Compose"
	@echo "make docker-down    - Stop Docker services"
	@echo "make docker-clean   - Remove Docker containers and volumes"
	@echo "make logs           - Follow Docker logs"
	@echo "make db-reset       - Reset database (removes data)"

build:
	@echo "Building ClinicOS..."
	mvn clean install -DskipTests --batch-mode

test:
	@echo "Running tests..."
	mvn test --batch-mode

clean:
	@echo "Cleaning build artifacts..."
	mvn clean

run-patient:
	cd clinic-patient && mvn spring-boot:run

run-appointment:
	cd clinic-appointment && mvn spring-boot:run

run-followup:
	cd clinic-followup && mvn spring-boot:run

run-notification:
	cd clinic-notification && mvn spring-boot:run

run-billing:
	cd clinic-billing && mvn spring-boot:run

run-gateway:
	cd clinic-gateway && mvn spring-boot:run

docker-up:
	@echo "Starting ClinicOS with Docker Compose..."
	docker-compose up -d
	@echo "Services are starting..."
	@echo "Gateway: http://localhost:8080"
	@echo "Patient: http://localhost:8081"
	@echo "Appointment: http://localhost:8082"
	@echo "Followup: http://localhost:8083"
	@echo "Notification: http://localhost:8084"
	@echo "Billing: http://localhost:8085"

docker-down:
	@echo "Stopping Docker services..."
	docker-compose down

docker-clean:
	@echo "Removing Docker containers and volumes..."
	docker-compose down -v
	docker system prune -f

logs:
	docker-compose logs -f

db-reset:
	@echo "WARNING: This will delete all database data!"
	@read -p "Are you sure? (y/n) " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v; \
		docker-compose up -d postgres; \
		echo "Database reset. Other services will start when ready."; \
	fi

format:
	@echo "Formatting code with Maven..."
	mvn fmt:format

deploy-gcp:
	@echo "Deploying to GCP Cloud Run..."
	@echo "Make sure you have gcloud CLI configured"
	bash scripts/deploy-gcp.sh

.DEFAULT_GOAL := help

