.PHONY: run test performance e2e down

run:
	docker compose up --build

test:
	mvn clean test

performance:
	mvn verify -Pperformance

e2e:
	docker compose up --build -d
	npm ci
	npm run test:e2e

down:
	docker compose down
