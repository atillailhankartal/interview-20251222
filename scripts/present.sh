#!/bin/bash

# =============================================================================
# BROKAGE TRADING PLATFORM - PRESENTATION MODE
# =============================================================================
# Fully automated demo: Starts services, waits for ready, runs E2E tests
# Just sit back and watch!
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m'
DIM='\033[2m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/frontend/web-client"

# =============================================================================
# FUNCTIONS
# =============================================================================

show_banner() {
    clear
    echo ""
    echo -e "${BOLD}${BLUE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${BLUE}║${NC}                                                                              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██████╗ ██████╗  ██████╗ ██╗  ██╗ █████╗  ██████╗ ███████╗${NC}              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██╔══██╗██╔══██╗██╔═══██╗██║ ██╔╝██╔══██╗██╔════╝ ██╔════╝${NC}              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██████╔╝██████╔╝██║   ██║█████╔╝ ███████║██║  ███╗█████╗${NC}                ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██╔══██╗██╔══██╗██║   ██║██╔═██╗ ██╔══██║██║   ██║██╔══╝${NC}                ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██████╔╝██║  ██║╚██████╔╝██║  ██╗██║  ██║╚██████╔╝███████╗${NC}              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝${NC}              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}                                                                              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}            ${BOLD}${WHITE}TRADING PLATFORM - LIVE PRESENTATION${NC}                          ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}                                                                              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while ps -p $pid > /dev/null 2>&1; do
        for i in $(seq 0 9); do
            printf "\r  ${CYAN}${spinstr:$i:1}${NC} $2"
            sleep $delay
        done
    done
    printf "\r"
}

check_service() {
    local name=$1
    local url=$2
    local max_attempts=${3:-1}

    for ((i=1; i<=max_attempts; i++)); do
        if curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null | grep -qE "200|302|401|404"; then
            return 0
        fi
        if [ $i -lt $max_attempts ]; then
            sleep 2
        fi
    done
    return 1
}

wait_for_service() {
    local name=$1
    local url=$2
    local max_wait=${3:-120}
    local start_time=$(date +%s)

    printf "  %-25s" "$name"

    while true; do
        if check_service "$name" "$url"; then
            echo -e "${GREEN}● Ready${NC}"
            return 0
        fi

        local elapsed=$(($(date +%s) - start_time))
        if [ $elapsed -ge $max_wait ]; then
            echo -e "${RED}○ Timeout${NC}"
            return 1
        fi

        printf "\r  %-25s${YELLOW}○ Waiting... (%ds)${NC}" "$name" "$elapsed"
        sleep 2
    done
}

# =============================================================================
# MAIN SCRIPT
# =============================================================================

show_banner

echo -e "${BOLD}${CYAN}Phase 1: Checking Docker...${NC}"
echo ""

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}${BOLD}Docker is not running!${NC}"
    echo -e "Please start Docker Desktop and run again."
    exit 1
fi
echo -e "  Docker................. ${GREEN}● Running${NC}"
echo ""

# =============================================================================
# PHASE 2: CHECK/START SERVICES
# =============================================================================

echo -e "${BOLD}${CYAN}Phase 2: Checking services...${NC}"
echo ""

services_running=true

# Check each service
printf "  %-25s" "Keycloak"
if check_service "Keycloak" "http://localhost:8180"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "Order Service"
if check_service "Order Service" "http://localhost:7081/actuator/health"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "Asset Service"
if check_service "Asset Service" "http://localhost:7082/actuator/health"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "Customer Service"
if check_service "Customer Service" "http://localhost:7083/actuator/health"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "Web API"
if check_service "Web API" "http://localhost:7087/actuator/health"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "API Gateway"
if check_service "API Gateway" "http://localhost:4500"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "Frontend"
if check_service "Frontend" "http://localhost:4000"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
    services_running=false
fi

printf "  %-25s" "Mailpit (Email)"
if check_service "Mailpit" "http://localhost:8026"; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${YELLOW}○ Not Running${NC}"
fi

echo ""

# Start services if needed
if [ "$services_running" = false ]; then
    echo -e "${BOLD}${YELLOW}Services not running. Starting platform...${NC}"
    echo -e "${DIM}This may take 2-3 minutes on first run.${NC}"
    echo ""

    # Start in background
    cd "$PROJECT_ROOT"
    make start >/dev/null 2>&1 &
    start_pid=$!

    # Wait for services
    echo -e "${BOLD}${CYAN}Waiting for services to be ready...${NC}"
    echo ""

    wait_for_service "Keycloak" "http://localhost:8180" 180
    wait_for_service "Order Service" "http://localhost:7081/actuator/health" 180
    wait_for_service "Asset Service" "http://localhost:7082/actuator/health" 180
    wait_for_service "Customer Service" "http://localhost:7083/actuator/health" 180
    wait_for_service "Web API" "http://localhost:7087/actuator/health" 180
    wait_for_service "API Gateway" "http://localhost:4500" 240
    wait_for_service "Frontend" "http://localhost:4000" 180
    wait_for_service "Mailpit (Email)" "http://localhost:8026" 60

    echo ""
    echo -e "${GREEN}${BOLD}All services ready!${NC}"
    echo ""
    sleep 3
fi

# =============================================================================
# PHASE 3: INSTALL PLAYWRIGHT (IF NEEDED)
# =============================================================================

echo -e "${BOLD}${CYAN}Phase 3: Preparing E2E tests...${NC}"
echo ""

cd "$FRONTEND_DIR"

# Check if playwright browsers installed
if ! npx playwright --version >/dev/null 2>&1; then
    echo -e "  Installing Playwright..."
    npm install >/dev/null 2>&1
fi

if [ ! -d "$HOME/Library/Caches/ms-playwright" ] && [ ! -d "$HOME/.cache/ms-playwright" ]; then
    echo -e "  Installing browsers..."
    npx playwright install chromium >/dev/null 2>&1
fi

echo -e "  Playwright............. ${GREEN}● Ready${NC}"
echo ""

# =============================================================================
# PHASE 4: SHOW SCENARIOS
# =============================================================================

show_banner

echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}${WHITE}                     PDF SCENARIOS - LIVE DEMONSTRATION${NC}"
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}${GREEN}📋 CORE REQUIREMENTS:${NC}"
echo -e "  ${WHITE}•${NC} POST /api/orders     - Create BUY/SELL orders"
echo -e "  ${WHITE}•${NC} GET /api/orders      - List orders with filters"
echo -e "  ${WHITE}•${NC} DELETE /api/orders   - Cancel PENDING orders"
echo -e "  ${WHITE}•${NC} GET /api/assets      - List customer assets"
echo ""
echo -e "${BOLD}${YELLOW}🎁 BONUS FEATURES:${NC}"
echo -e "  ${WHITE}•${NC} Customer Authorization (role-based access)"
echo -e "  ${WHITE}•${NC} Admin Match Endpoint (order settlement)"
echo -e "  ${WHITE}•${NC} Email Notifications (view at Mailpit: http://localhost:8026)"
echo ""
echo -e "${BOLD}${PURPLE}📊 SCHEMA COMPLIANCE:${NC}"
echo -e "  ${WHITE}•${NC} Asset: customerId, assetName, size, usableSize"
echo -e "  ${WHITE}•${NC} TRY stored as asset (not separate table)"
echo ""
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

# Countdown with visual effect
echo -e "${BOLD}${WHITE}Starting live demo in...${NC}"
echo ""
for i in 5 4 3 2 1; do
    echo -ne "\r  ${BOLD}"
    case $i in
        5) echo -ne "${GREEN}" ;;
        4) echo -ne "${CYAN}" ;;
        3) echo -ne "${YELLOW}" ;;
        2) echo -ne "${YELLOW}" ;;
        1) echo -ne "${RED}" ;;
    esac
    echo -ne "▓▓▓▓▓▓▓▓▓▓ $i ${NC}"
    sleep 1
done
echo -ne "\r                              \r"
echo ""

# =============================================================================
# PHASE 5: RUN PRESENTATION
# =============================================================================

echo -e "${BOLD}${GREEN}🎬 LIVE DEMO STARTING...${NC}"
echo -e "${DIM}Browser will open automatically. Sit back and watch!${NC}"
echo ""
sleep 2

# Run playwright with presentation config
npx playwright test \
    --config=playwright.present.config.ts \
    --headed \
    --reporter=list 2>&1 | while IFS= read -r line; do

    # Color code output
    if [[ "$line" == *"✓"* ]] || [[ "$line" == *"passed"* ]]; then
        echo -e "${GREEN}$line${NC}"
    elif [[ "$line" == *"✗"* ]] || [[ "$line" == *"failed"* ]]; then
        echo -e "${RED}$line${NC}"
    elif [[ "$line" == *"Running"* ]] || [[ "$line" == *"›"* ]]; then
        echo -e "${CYAN}$line${NC}"
    elif [[ "$line" == *"PASSED"* ]]; then
        echo -e "${BOLD}${GREEN}$line${NC}"
    else
        echo "$line"
    fi
done

exit_code=${PIPESTATUS[0]}

# =============================================================================
# PHASE 6: RESULTS
# =============================================================================

echo ""
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"

if [ $exit_code -eq 0 ]; then
    echo ""
    echo -e "${BOLD}${GREEN}  ╔═══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${GREEN}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${GREEN}  ║   ✅  ALL PDF SCENARIOS DEMONSTRATED SUCCESSFULLY!                   ║${NC}"
    echo -e "${BOLD}${GREEN}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${GREEN}  ╚═══════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
else
    echo ""
    echo -e "${BOLD}${YELLOW}  ╔═══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${YELLOW}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${YELLOW}  ║   ⚠️  Some tests had issues. Check results above.                    ║${NC}"
    echo -e "${BOLD}${YELLOW}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${YELLOW}  ╚═══════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
fi

echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}${WHITE}📁 Artifacts:${NC}"
echo -e "  ${WHITE}Videos:${NC}     ${BLUE}$FRONTEND_DIR/test-results/${NC}"
echo -e "  ${WHITE}Report:${NC}     ${BLUE}$FRONTEND_DIR/presentation-report/index.html${NC}"
echo ""
echo -e "${BOLD}${WHITE}🔗 Application URLs:${NC}"
echo -e "  ${WHITE}Frontend:${NC}   ${BLUE}http://localhost:4000${NC}"
echo -e "  ${WHITE}Keycloak:${NC}   ${BLUE}http://localhost:8180${NC} ${DIM}(admin/admin123)${NC}"
echo -e "  ${WHITE}Grafana:${NC}    ${BLUE}http://localhost:3001${NC} ${DIM}(admin/admin123)${NC}"
echo -e "  ${WHITE}Kafka UI:${NC}   ${BLUE}http://localhost:8089${NC}"
echo -e "  ${WHITE}Mailpit:${NC}    ${BLUE}http://localhost:8026${NC} ${DIM}(Email notifications)${NC}"
echo ""
echo -e "${BOLD}${WHITE}👤 Demo Users:${NC} ${DIM}(password: password123)${NC}"
echo -e "  ${GREEN}admin@brokage.com${NC}      - ADMIN role"
echo -e "  ${GREEN}broker1@brokage.com${NC}    - BROKER role"
echo -e "  ${GREEN}customer1@brokage.com${NC}  - CUSTOMER role"
echo ""

# Open report option
if [ -f "$FRONTEND_DIR/presentation-report/index.html" ]; then
    echo -e "${BOLD}${CYAN}Would you like to view the HTML report? [y/N]${NC} "
    read -r -t 10 response || response="n"
    if [[ "$response" =~ ^[Yy]$ ]]; then
        open "$FRONTEND_DIR/presentation-report/index.html" 2>/dev/null || \
        xdg-open "$FRONTEND_DIR/presentation-report/index.html" 2>/dev/null || \
        echo "Open: $FRONTEND_DIR/presentation-report/index.html"
    fi
fi

echo ""
echo -e "${DIM}Thank you for watching the Brokage Trading Platform demo!${NC}"
echo ""
