#!/bin/bash

# K6 Stress Test Runner
# Usage: ./run.sh [test-name] [options]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"

# Create reports directory
mkdir -p "${REPORTS_DIR}"

# Default environment variables
export BASE_URL="${BASE_URL:-http://localhost:8081}"
export ORDER_SERVICE_URL="${ORDER_SERVICE_URL:-http://localhost:8081}"
export ASSET_SERVICE_URL="${ASSET_SERVICE_URL:-http://localhost:8082}"
export KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"

function show_help() {
    echo "K6 Stress Test Runner"
    echo ""
    echo "Usage: ./run.sh [test-name] [options]"
    echo ""
    echo "Available tests:"
    echo "  order-flow          - Order creation and cancellation flow"
    echo "  asset-operations    - Deposit, withdraw, and balance checks"
    echo "  full-trading        - Complete trading scenario (multi-service)"
    echo "  spike               - Spike test (sudden load increase)"
    echo "  all                 - Run all tests sequentially"
    echo ""
    echo "Options:"
    echo "  --vus N            - Override default VU count"
    echo "  --duration D       - Override default duration"
    echo "  --cloud            - Run on K6 Cloud"
    echo ""
    echo "Environment Variables:"
    echo "  ORDER_SERVICE_URL  - Order service URL (default: http://localhost:8081)"
    echo "  ASSET_SERVICE_URL  - Asset service URL (default: http://localhost:8082)"
    echo "  KEYCLOAK_URL       - Keycloak URL (default: http://localhost:8180)"
    echo ""
    echo "Examples:"
    echo "  ./run.sh order-flow"
    echo "  ./run.sh spike --vus 500"
    echo "  ./run.sh all"
}

function run_test() {
    local test_name=$1
    local script_path="${SCRIPT_DIR}/scripts/${test_name}.js"

    if [ ! -f "${script_path}" ]; then
        echo "Error: Test script not found: ${script_path}"
        exit 1
    fi

    echo "============================================="
    echo "Running test: ${test_name}"
    echo "Timestamp: $(date)"
    echo "============================================="
    echo ""

    k6 run "${script_path}" ${@:2}

    echo ""
    echo "Test completed: ${test_name}"
    echo "Report saved to: ${REPORTS_DIR}/${test_name}-summary.json"
    echo ""
}

function run_all_tests() {
    echo "Running all K6 stress tests..."
    echo ""

    run_test "order-flow"
    sleep 5

    run_test "asset-operations"
    sleep 5

    run_test "full-trading"
    sleep 5

    run_test "spike"

    echo ""
    echo "============================================="
    echo "All tests completed!"
    echo "Reports are available in: ${REPORTS_DIR}"
    echo "============================================="
}

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "Error: k6 is not installed"
    echo "Install with: brew install k6 (macOS) or see https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# Parse arguments
case "${1}" in
    order-flow|asset-operations|full-trading|spike|full-trading-scenario)
        test_name="${1}"
        if [ "${test_name}" = "full-trading" ]; then
            test_name="full-trading-scenario"
        fi
        if [ "${test_name}" = "spike" ]; then
            test_name="spike-test"
        fi
        run_test "${test_name}" ${@:2}
        ;;
    all)
        run_all_tests
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo "Error: Unknown test '${1}'"
        echo ""
        show_help
        exit 1
        ;;
esac
