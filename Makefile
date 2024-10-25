SHELL = '/bin/bash'
DEV_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.dev.yml
TEST_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.test.yml
LOCAL_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.local.yml
PROJECT_NAME = hmpps-assess-risks-and-needs

export COMPOSE_PROJECT_NAME=${PROJECT_NAME}

default: help

help: ## The help text you're reading.
	@grep --no-filename -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

up: ## Starts/restarts the API in a production container.
	docker compose ${LOCAL_COMPOSE_FILES} down arns-handover
	docker compose ${LOCAL_COMPOSE_FILES} up arns-handover --wait --no-recreate

down: ## Stops and removes all containers in the project.
	docker compose ${DEV_COMPOSE_FILES} down
	docker compose ${LOCAL_COMPOSE_FILES} down

build-api: ## Builds a production image of the API.
	docker-compose -f docker/docker-compose.yml build arns-handover

dev-up: ## Starts/restarts the API in a development container. A remote debugger can be attached on port 5005.
	docker compose ${DEV_COMPOSE_FILES} down arns-handover
	docker compose ${DEV_COMPOSE_FILES} up --wait --no-recreate arns-handover

dev-build: ## Builds a development image of the API.
	docker compose ${DEV_COMPOSE_FILES} build arns-handover --no-cache

dev-down: ## Stops and removes the API container.
	docker compose down

rebuild: ## Re-builds and reloads the API.
	docker compose ${DEV_COMPOSE_FILES} exec arns-handover gradle compileKotlin --parallel --build-cache --configuration-cache

watch: ## Watches for file changes and live-reloads the API. To be used in conjunction with dev-up e.g. "make dev-up watch"
	docker compose ${DEV_COMPOSE_FILES} exec arns-handover gradle compileKotlin --continuous --parallel --build-cache --configuration-cache

test: ## Runs all the test suites.
	docker compose ${DEV_COMPOSE_FILES} exec arns-handover gradle test --parallel

lint: ## Runs the Kotlin linter.
	docker compose ${DEV_COMPOSE_FILES} exec arns-handover gradle ktlintCheck --parallel

lint-fix: ## Runs the Kotlin linter and auto-fixes.
	docker compose ${DEV_COMPOSE_FILES} exec arns-handover gradle ktlintFormat --parallel

test-up: ## Stands up a test environment.
	docker compose ${TEST_COMPOSE_FILES} pull --policy missing
	docker compose ${TEST_COMPOSE_FILES} -p ${PROJECT_NAME}-test up arns-handover --wait

test-down: ## Stops and removes all of the test containers.
	docker compose ${TEST_COMPOSE_FILES} -p ${PROJECT_NAME}-test down

BASE_URL_CI ?= "http://oasys-ui:3000"
e2e-ci: ## Run the end-to-end tests in a headless browser. Used in CI. Override the default base URL with BASE_URL_CI=...
	docker compose ${TEST_COMPOSE_FILES} -p ${PROJECT_NAME}-test run --rm cypress --headless -c baseUrl=${BASE_URL_CI}

clean: ## Stops and removes all project containers. Deletes local build/cache directories.
	docker compose down
	docker volume ls -qf "dangling=true" | xargs -r docker volume rm
	rm -rf .gradle build

update: ## Downloads the latest versions of containers.
	docker compose ${LOCAL_COMPOSE_FILES} pull

dev-api-token: ## Generates a JWT for authenticating with the handover service.
	docker compose ${DEV_COMPOSE_FILES} exec arns-handover \
		curl --location 'http://hmpps-auth:9090/auth/oauth/token' \
	--header 'authorization: Basic aG1wcHMtYXNzZXNzLXJpc2tzLWFuZC1uZWVkcy1vYXN0dWItdWk6Y2xpZW50c2VjcmV0' \
	--header 'Content-Type: application/x-www-form-urlencoded' \
	--data-urlencode 'grant_type=client_credentials' \
	| jq -r '.access_token' \
	| xargs printf "\nToken:\n%s\n"

save-logs: ## Saves docker container logs in a directory defined by OUTPUT_LOGS_DIR=
	mkdir -p ${OUTPUT_LOGS_DIR}
	docker logs ${PROJECT_NAME}-arns-handover-1 > ${OUTPUT_LOGS_DIR}/arns-handover.log
	docker logs ${PROJECT_NAME}-coordinator-api-1 > ${OUTPUT_LOGS_DIR}/coordinator-api.log
	docker logs ${PROJECT_NAME}-oasys-ui-1 > ${OUTPUT_LOGS_DIR}/oasys-ui.log
	docker logs ${PROJECT_NAME}-hmpps-auth-1 > ${OUTPUT_LOGS_DIR}/hmpps-auth.log
	docker logs ${PROJECT_NAME}-san-ui-1 > ${OUTPUT_LOGS_DIR}/san-ui.log
	docker logs ${PROJECT_NAME}-san-api-1 > ${OUTPUT_LOGS_DIR}/san-api.log
