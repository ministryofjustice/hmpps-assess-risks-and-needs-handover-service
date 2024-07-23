SHELL = '/bin/bash'
PROJECT_NAME = hmpps-assess-risks-and-needs-handover-service
TEST_COMPOSE_FILES = -f docker-compose.test.yml
export COMPOSE_PROJECT_NAME=${PROJECT_NAME}

default: help

help: ## The help text you're reading.
	@grep --no-filename -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

BASE_URL_CI ?= "http://oasys-ui:3000"
e2e-ci: ## Run the end-to-end tests in a headless browser. Used in CI. Override the default base URL with BASE_URL_CI=...
	docker compose ${TEST_COMPOSE_FILES} -p ${PROJECT_NAME}-test run --rm cypress --headless -c baseUrl=${BASE_URL_CI}
