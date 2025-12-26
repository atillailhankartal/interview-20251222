#!/bin/bash

# =============================================================================
# ORANGE BROKER HUB - PRESENTATION MODE
# =============================================================================
# Fully automated live demo for interviews
#
# Features:
# - Auto-starts all services in background with progress
# - Shows intro slide (grab your coffee!)
# - Runs through Customer -> Broker -> Admin flows
# - Demonstrates all PDF requirements
# - Shows closing slide with service URLs
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
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN} ██████╗ ██████╗  █████╗ ███╗   ██╗ ██████╗ ███████╗${NC}                   ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██╔═══██╗██╔══██╗██╔══██╗████╗  ██║██╔════╝ ██╔════╝${NC}                   ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██║   ██║██████╔╝███████║██╔██╗ ██║██║  ███╗█████╗${NC}                     ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}██║   ██║██╔══██╗██╔══██║██║╚██╗██║██║   ██║██╔══╝${NC}                     ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN}╚██████╔╝██║  ██║██║  ██║██║ ╚████║╚██████╔╝███████╗${NC}                   ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}     ${BOLD}${GREEN} ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝ ╚═════╝ ╚══════╝${NC}                   ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}                                                                              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}                     ${BOLD}${WHITE}BROKER HUB - LIVE PRESENTATION${NC}                         ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}║${NC}                                                                              ${BOLD}${BLUE}║${NC}"
    echo -e "${BOLD}${BLUE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

progress_bar() {
    local current=$1
    local total=$2
    local width=50
    local percent=$((current * 100 / total))
    local filled=$((width * current / total))
    local empty=$((width - filled))

    printf "\r  ["
    printf "%${filled}s" | tr ' ' '█'
    printf "%${empty}s" | tr ' ' '░'
    printf "] %3d%% " "$percent"
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

    while true; do
        if check_service "$name" "$url"; then
            return 0
        fi

        local elapsed=$(($(date +%s) - start_time))
        if [ $elapsed -ge $max_wait ]; then
            return 1
        fi

        sleep 2
    done
}

spinner() {
    local pid=$1
    local delay=0.15
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while ps -p $pid > /dev/null 2>&1; do
        for i in $(seq 0 9); do
            printf "\r  ${CYAN}${spinstr:$i:1}${NC} Starting services... "
            sleep $delay
        done
    done
    printf "\r                                      \r"
}

wait_for_service_with_spinner() {
    local name=$1
    local url=$2
    local max_wait=${3:-120}
    local start_time=$(date +%s)
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    local spin_idx=0

    while true; do
        if check_service "$name" "$url"; then
            return 0
        fi

        local elapsed=$(($(date +%s) - start_time))
        if [ $elapsed -ge $max_wait ]; then
            return 1
        fi

        # Show spinner with elapsed time
        printf "\r  %-20s ${CYAN}${spinstr:$spin_idx:1}${NC} Waiting... (%ds) " "$name" "$elapsed"
        spin_idx=$(( (spin_idx + 1) % 10 ))
        sleep 0.5
    done
}

wait_for_port_with_spinner() {
    local name=$1
    local port=$2
    local max_wait=${3:-60}
    local start_time=$(date +%s)
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    local spin_idx=0

    while true; do
        if nc -z localhost "$port" 2>/dev/null; then
            return 0
        fi

        local elapsed=$(($(date +%s) - start_time))
        if [ $elapsed -ge $max_wait ]; then
            return 1
        fi

        # Show spinner with elapsed time
        printf "\r  %-20s ${CYAN}${spinstr:$spin_idx:1}${NC} Waiting... (%ds) " "$name" "$elapsed"
        spin_idx=$(( (spin_idx + 1) % 10 ))
        sleep 0.5
    done
}

start_services_with_progress() {
    echo -e "${BOLD}${CYAN}Starting Platform Services...${NC}"
    echo -e "${DIM}This may take 2-4 minutes on first run.${NC}"
    echo ""

    # Start services in background
    cd "$PROJECT_ROOT"
    echo -e "  ${CYAN}Running 'make start' in background...${NC}"
    make start >/dev/null 2>&1 &
    local start_pid=$!

    # Brief pause to let docker compose start
    sleep 3
    echo -e "  ${GREEN}✓${NC} Docker Compose started"
    echo ""

    # Services to check with order - infrastructure first, then apps
    echo -e "${BOLD}${WHITE}Infrastructure:${NC}"

    # Infrastructure services (port checks)
    local infra_services=(
        "PostgreSQL:5432:90"
        "MongoDB:27017:60"
        "Redis:6379:30"
        "Kafka:9092:90"
    )

    for service_info in "${infra_services[@]}"; do
        IFS=':' read -r name port timeout <<< "$service_info"
        if wait_for_port_with_spinner "$name" "$port" "$timeout"; then
            printf "\r  %-20s ${GREEN}● Ready${NC}                    \n" "$name"
        else
            printf "\r  %-20s ${RED}✗ Timeout${NC}                  \n" "$name"
            echo -e "\n${RED}${BOLD}Error:${NC} $name failed to start within ${timeout}s"
            echo -e "Try running: ${CYAN}make logs-$name${NC} to see what's wrong"
            kill $start_pid 2>/dev/null
            exit 1
        fi
    done

    echo ""
    echo -e "${BOLD}${WHITE}Authentication:${NC}"

    if wait_for_service_with_spinner "Keycloak" "http://localhost:8180" 180; then
        printf "\r  %-20s ${GREEN}● Ready${NC}                    \n" "Keycloak"
    else
        printf "\r  %-20s ${YELLOW}○ Still starting...${NC}       \n" "Keycloak"
    fi

    echo ""
    echo -e "${BOLD}${WHITE}Backend Services:${NC}"

    local backend_services=(
        "Order Service|http://localhost:7081/actuator/health|120"
        "Asset Service|http://localhost:7082/actuator/health|120"
        "Customer Service|http://localhost:7083/actuator/health|120"
        "Notification|http://localhost:7085/actuator/health|120"
        "Web API|http://localhost:7087/actuator/health|120"
    )

    for service_info in "${backend_services[@]}"; do
        IFS='|' read -r name url timeout <<< "$service_info"
        if wait_for_service_with_spinner "$name" "$url" "$timeout"; then
            printf "\r  %-20s ${GREEN}● Ready${NC}                    \n" "$name"
        else
            printf "\r  %-20s ${YELLOW}○ Still starting...${NC}       \n" "$name"
        fi
    done

    echo ""
    echo -e "${BOLD}${WHITE}Frontend:${NC}"

    local frontend_services=(
        "API Gateway|http://localhost:4500|60"
        "Web Client|http://localhost:4000|60"
    )

    for service_info in "${frontend_services[@]}"; do
        IFS='|' read -r name url timeout <<< "$service_info"
        if wait_for_service_with_spinner "$name" "$url" "$timeout"; then
            printf "\r  %-20s ${GREEN}● Ready${NC}                    \n" "$name"
        else
            printf "\r  %-20s ${YELLOW}○ Still starting...${NC}       \n" "$name"
        fi
    done

    echo ""
    echo -e "${GREEN}${BOLD}✓ Services startup complete!${NC}"
    echo ""
    sleep 2
}

# =============================================================================
# MAIN SCRIPT
# =============================================================================

show_banner

# =============================================================================
# PHASE 1: CHECK PREREQUISITES
# =============================================================================

echo -e "${BOLD}${CYAN}Phase 1: Checking Prerequisites...${NC}"
echo ""

prereq_failed=false

# Check Docker
printf "  %-22s" "Docker"
if docker info >/dev/null 2>&1; then
    echo -e "${GREEN}● Running${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
    prereq_failed=true
fi

# Check Node.js
printf "  %-22s" "Node.js"
if command -v node >/dev/null 2>&1; then
    node_version=$(node --version 2>/dev/null)
    echo -e "${GREEN}● ${node_version}${NC}"
else
    echo -e "${RED}✗ Not installed${NC}"
    prereq_failed=true
fi

# Check npm
printf "  %-22s" "npm"
if command -v npm >/dev/null 2>&1; then
    npm_version=$(npm --version 2>/dev/null)
    echo -e "${GREEN}● v${npm_version}${NC}"
else
    echo -e "${RED}✗ Not installed${NC}"
    prereq_failed=true
fi

# Check if node_modules exists
printf "  %-22s" "Dependencies"
if [ -d "$FRONTEND_DIR/node_modules" ]; then
    echo -e "${GREEN}● Installed${NC}"
else
    echo -e "${YELLOW}○ Not installed${NC}"
    echo ""
    echo -e "${YELLOW}Installing npm dependencies...${NC}"
    cd "$FRONTEND_DIR"
    if npm install --silent 2>/dev/null; then
        echo -e "  Dependencies           ${GREEN}● Installed${NC}"
    else
        echo -e "  Dependencies           ${RED}✗ Failed${NC}"
        prereq_failed=true
    fi
fi

# Check Playwright
printf "  %-22s" "Playwright"
if [ -d "$FRONTEND_DIR/node_modules/@playwright" ]; then
    playwright_version=$(cd "$FRONTEND_DIR" && npx playwright --version 2>/dev/null || echo "installed")
    echo -e "${GREEN}● ${playwright_version}${NC}"
else
    echo -e "${RED}✗ Not installed${NC}"
    prereq_failed=true
fi

# Check Playwright browsers
printf "  %-22s" "Chromium Browser"
if [ -d "$HOME/Library/Caches/ms-playwright/chromium"* ] 2>/dev/null || \
   [ -d "$HOME/.cache/ms-playwright/chromium"* ] 2>/dev/null || \
   ls "$HOME/Library/Caches/ms-playwright/" 2>/dev/null | grep -q chromium || \
   ls "$HOME/.cache/ms-playwright/" 2>/dev/null | grep -q chromium; then
    echo -e "${GREEN}● Installed${NC}"
else
    echo -e "${YELLOW}○ Not installed${NC}"
    echo ""
    echo -e "${YELLOW}Installing Playwright browsers...${NC}"
    cd "$FRONTEND_DIR"
    if npx playwright install chromium 2>/dev/null; then
        echo -e "  Chromium Browser       ${GREEN}● Installed${NC}"
    else
        echo -e "  Chromium Browser       ${RED}✗ Failed${NC}"
        prereq_failed=true
    fi
fi

echo ""

# If any prerequisite failed, show help and exit
if [ "$prereq_failed" = true ]; then
    echo -e "${RED}${BOLD}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}${BOLD}  PREREQUISITES CHECK FAILED${NC}"
    echo -e "${RED}${BOLD}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${WHITE}Please install missing prerequisites:${NC}"
    echo ""
    echo -e "  ${BOLD}Docker:${NC}      Install Docker Desktop from https://docker.com"
    echo -e "  ${BOLD}Node.js:${NC}     Install from https://nodejs.org (v18+ recommended)"
    echo -e "  ${BOLD}npm:${NC}         Comes with Node.js"
    echo ""
    echo -e "  ${BOLD}Dependencies:${NC}"
    echo -e "    cd $FRONTEND_DIR"
    echo -e "    npm install"
    echo ""
    echo -e "  ${BOLD}Playwright:${NC}"
    echo -e "    cd $FRONTEND_DIR"
    echo -e "    npx playwright install chromium"
    echo ""
    exit 1
fi

echo -e "${GREEN}${BOLD}✓ All prerequisites satisfied!${NC}"
echo ""

# =============================================================================
# PHASE 2: CHECK/START SERVICES
# =============================================================================

echo -e "${BOLD}${CYAN}Phase 2: Checking services...${NC}"
echo ""

services_running=true

# Quick check key services
for service in \
    "Keycloak:http://localhost:8180" \
    "Order Service:http://localhost:7081/actuator/health" \
    "Frontend:http://localhost:4000"
do
    name="${service%%:*}"
    url="${service#*:}"
    printf "  %-20s" "$name"
    if check_service "$name" "$url"; then
        echo -e "${GREEN}● Running${NC}"
    else
        echo -e "${YELLOW}○ Not Running${NC}"
        services_running=false
    fi
done

echo ""

if [ "$services_running" = false ]; then
    echo ""
    echo -e "${BOLD}${YELLOW}⚠️  Services are not running!${NC}"
    echo ""
    echo -e "${WHITE}You have two options:${NC}"
    echo ""
    echo -e "  ${BOLD}${GREEN}Option 1:${NC} Start services first (recommended)"
    echo -e "           Run: ${CYAN}make start${NC}"
    echo -e "           Then run: ${CYAN}make present${NC}"
    echo ""
    echo -e "  ${BOLD}${GREEN}Option 2:${NC} Let me start them for you now"
    echo ""
    echo -e "${BOLD}${WHITE}Do you want me to start the services now? [y/N]${NC} "
    read -r -t 30 response

    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo ""
        start_services_with_progress
    else
        echo ""
        echo -e "${YELLOW}Please start services first with: ${CYAN}make start${NC}"
        echo ""
        exit 1
    fi
fi

# =============================================================================
# PHASE 3: READY TO PRESENT
# =============================================================================

cd "$FRONTEND_DIR"

# =============================================================================
# PHASE 4: SHOW PRESENTATION INFO
# =============================================================================

show_banner

echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}${WHITE}                        LIVE PRESENTATION OVERVIEW${NC}"
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}${GREEN}📋 DEMO FLOW:${NC}"
echo ""
echo -e "  ${WHITE}1.${NC} ${CYAN}Introduction Slide${NC}    - Welcome & Overview"
echo -e "  ${WHITE}2.${NC} ${GREEN}Customer Flow${NC}         - Login → Dashboard → Deposit → Order → Logout"
echo -e "  ${WHITE}3.${NC} ${YELLOW}Broker Flow${NC}           - Login → Dashboard → Match Order → Mailpit → Logout"
echo -e "  ${WHITE}4.${NC} ${PURPLE}Admin Flow${NC}            - Full system management & Reports"
echo -e "  ${WHITE}5.${NC} ${GREEN}Customer Withdraw${NC}     - Withdraw request demonstration"
echo -e "  ${WHITE}6.${NC} ${CYAN}Closing Slide${NC}         - Service URLs & Thank You"
echo ""
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${BOLD}${WHITE}☕ Grab your coffee and sit back!${NC}"
echo ""

# Countdown
echo -e "${BOLD}${WHITE}Starting in...${NC}"
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
echo -e "${DIM}Browser will open automatically. Enjoy the show!${NC}"
echo ""
sleep 2

# Create a temp file for output
PLAYWRIGHT_OUTPUT=$(mktemp)
PLAYWRIGHT_PID_FILE=$(mktemp)

# Run playwright presentation in background with KEEP_BROWSER_OPEN
(
    KEEP_BROWSER_OPEN=true npx playwright test \
        --config=playwright.present.config.ts \
        --headed \
        --reporter=list 2>&1
    echo $? > "${PLAYWRIGHT_PID_FILE}.exit"
) > "$PLAYWRIGHT_OUTPUT" 2>&1 &
PLAYWRIGHT_PID=$!
echo $PLAYWRIGHT_PID > "$PLAYWRIGHT_PID_FILE"

# Follow output in real-time with colors
tail -f "$PLAYWRIGHT_OUTPUT" 2>/dev/null | while IFS= read -r line; do
    # Stop following when we see the "Press Enter" message
    if [[ "$line" == *"Press Enter"* ]]; then
        echo -e "${BOLD}${CYAN}$line${NC}"
        break
    fi

    # Color code output
    if [[ "$line" == *"✓"* ]] || [[ "$line" == *"passed"* ]]; then
        echo -e "${GREEN}$line${NC}"
    elif [[ "$line" == *"✗"* ]] || [[ "$line" == *"failed"* ]]; then
        echo -e "${RED}$line${NC}"
    elif [[ "$line" == *"Running"* ]] || [[ "$line" == *"›"* ]]; then
        echo -e "${CYAN}$line${NC}"
    elif [[ "$line" == *"PASSED"* ]] || [[ "$line" == *"COMPLETE"* ]]; then
        echo -e "${BOLD}${GREEN}$line${NC}"
    elif [[ "$line" == *"Thank you"* ]]; then
        echo -e "${BOLD}${WHITE}$line${NC}"
    else
        echo "$line"
    fi
done &
TAIL_PID=$!

# Wait for presentation to reach the "keep open" state or finish
sleep 5
while kill -0 $PLAYWRIGHT_PID 2>/dev/null; do
    if grep -q "Press Enter" "$PLAYWRIGHT_OUTPUT" 2>/dev/null; then
        break
    fi
    if [ -f "${PLAYWRIGHT_PID_FILE}.exit" ]; then
        break
    fi
    sleep 1
done

# Kill the tail process
kill $TAIL_PID 2>/dev/null

# Check if browser is still open (presentation waiting)
if kill -0 $PLAYWRIGHT_PID 2>/dev/null && grep -q "Press Enter" "$PLAYWRIGHT_OUTPUT" 2>/dev/null; then
    echo ""
    echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BOLD}${WHITE}  🖥️  Browser is open. You can interact with the application.${NC}"
    echo ""
    echo -e "${BOLD}${YELLOW}  Press ENTER to close the browser and end the presentation...${NC}"
    echo ""
    echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"

    read -r

    echo ""
    echo -e "${DIM}Closing browser...${NC}"
    kill $PLAYWRIGHT_PID 2>/dev/null
    wait $PLAYWRIGHT_PID 2>/dev/null
    exit_code=0
else
    # Presentation finished normally or failed
    wait $PLAYWRIGHT_PID 2>/dev/null
    exit_code=$?
    if [ -f "${PLAYWRIGHT_PID_FILE}.exit" ]; then
        exit_code=$(cat "${PLAYWRIGHT_PID_FILE}.exit")
    fi
fi

# Cleanup temp files
rm -f "$PLAYWRIGHT_OUTPUT" "$PLAYWRIGHT_PID_FILE" "${PLAYWRIGHT_PID_FILE}.exit" 2>/dev/null

# =============================================================================
# PHASE 6: RESULTS
# =============================================================================

echo ""
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"

if [ $exit_code -eq 0 ]; then
    echo ""
    echo -e "${BOLD}${GREEN}  ╔═══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${GREEN}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${GREEN}  ║   ✅  PRESENTATION COMPLETED SUCCESSFULLY!                           ║${NC}"
    echo -e "${BOLD}${GREEN}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${GREEN}  ╚═══════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
else
    echo ""
    echo -e "${BOLD}${YELLOW}  ╔═══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${YELLOW}  ║                                                                       ║${NC}"
    echo -e "${BOLD}${YELLOW}  ║   ⚠️  Presentation had some issues. Check results above.             ║${NC}"
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
echo -e "  ${WHITE}Frontend:${NC}      ${BLUE}http://localhost:4000${NC}"
echo -e "  ${WHITE}Grafana:${NC}       ${BLUE}http://localhost:3001${NC} ${DIM}(admin/admin123)${NC}"
echo -e "  ${WHITE}Keycloak:${NC}      ${BLUE}http://localhost:8180${NC} ${DIM}(admin/admin123)${NC}"
echo -e "  ${WHITE}Kafka UI:${NC}      ${BLUE}http://localhost:8089${NC}"
echo -e "  ${WHITE}Mailpit:${NC}       ${BLUE}http://localhost:8026${NC}"
echo -e "  ${WHITE}pgAdmin:${NC}       ${BLUE}http://localhost:5050${NC} ${DIM}(admin@brokage.com/admin123)${NC}"
echo -e "  ${WHITE}Mongo Express:${NC} ${BLUE}http://localhost:8027${NC} ${DIM}(admin/admin123)${NC}"
echo ""
echo -e "${BOLD}${WHITE}👤 Demo Users:${NC}"
echo -e "  ${GREEN}nick.fury@brokage.com${NC}     - ADMIN   ${DIM}(admin123)${NC}"
echo -e "  ${GREEN}tony.stark@brokage.com${NC}    - BROKER  ${DIM}(broker123)${NC}"
echo -e "  ${GREEN}peter.parker@brokage.com${NC}  - CUSTOMER ${DIM}(customer123)${NC}"
echo ""

# Open report
if [ -f "$FRONTEND_DIR/presentation-report/index.html" ]; then
    echo -e "${BOLD}Opening HTML report...${NC}"
    sleep 2
    open "$FRONTEND_DIR/presentation-report/index.html" 2>/dev/null || \
    xdg-open "$FRONTEND_DIR/presentation-report/index.html" 2>/dev/null || \
    echo "Open: $FRONTEND_DIR/presentation-report/index.html"
fi

echo ""
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "                    ${BOLD}Thank you for watching!${NC}"
echo ""
echo -e "                    ${BOLD}${GREEN}Atilla Ilhan KARTAL${NC}"
echo -e "                    ${DIM}Software Architect${NC}"
echo ""
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${NC}"
echo ""
