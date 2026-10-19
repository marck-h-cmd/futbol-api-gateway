#!/usr/bin/env bash
set -e

PASARELA_URL="http://localhost:8080"
EUREKA_URL="http://localhost:8761"
EQUIPOS_URL="http://localhost:8081"
ENCUENTROS_URL="http://localhost:8082"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}    BATERÍA DE PRUEBAS DE MICROSERVICIOS Y PASARELA    ${NC}"
echo -e "${BLUE}======================================================${NC}"

total_tests=0
passed_tests=0

assert_status() {
    local test_num="$1"
    local desc="$2"
    local expected="$3"
    local actual="$4"
    local response="$5"

    total_tests=$((total_tests + 1))
    if [ "$actual" -eq "$expected" ]; then
        echo -e "${GREEN}[PASS] Test #$test_num: $desc (HTTP $actual)${NC}"
        [ -n "$response" ] && echo -e "       Respuesta: $response"
        passed_tests=$((passed_tests + 1))
    else
        echo -e "${RED}[FAIL] Test #$test_num: $desc (Esperado: HTTP $expected, Recibido: HTTP $actual)${NC}"
        [ -n "$response" ] && echo -e "       Respuesta: $response"
    fi
}

echo -e "\n${YELLOW}--- 1. Pruebas Funcionales a través de la Pasarela (Puerto 8080) ---${NC}"

# Test 1: Crear Real Madrid
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/equipos" \
    -H "Content-Type: application/json" \
    -d '{"nombre": "Real Madrid"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 1 "POST /api/equipos -> Crear Real Madrid" 201 "$code" "$body"

# Test 2: Crear FC Barcelona
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/equipos" \
    -H "Content-Type: application/json" \
    -d '{"nombre": "FC Barcelona"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 2 "POST /api/equipos -> Crear FC Barcelona" 201 "$code" "$body"

# Test 3: Crear Atlético Madrid
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/equipos" \
    -H "Content-Type: application/json" \
    -d '{"nombre": "Atlético Madrid"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 3 "POST /api/equipos -> Crear Atlético Madrid" 201 "$code" "$body"

# Test 4: Listar equipos
res=$(curl -s -w "\n%{http_code}" -X GET "$PASARELA_URL/api/equipos")
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 4 "GET /api/equipos -> Listar equipos (3 esperados)" 200 "$code" "$body"

# Test 5: Obtener equipo por ID 1
res=$(curl -s -w "\n%{http_code}" -X GET "$PASARELA_URL/api/equipos/1")
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 5 "GET /api/equipos/1 -> Obtener equipo por ID 1" 200 "$code" "$body"

# Test 6: Validación nombre duplicado
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/equipos" \
    -H "Content-Type: application/json" \
    -d '{"nombre": "Real Madrid"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 6 "POST /api/equipos -> Validación nombre duplicado" 400 "$code" "$body"

# Test 7: Partido RM 3 - 1 FCB
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/encuentros" \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 1, "equipoVisitanteId": 2, "golesLocal": 3, "golesVisitante": 1, "fecha": "2026-10-19"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 7 "POST /api/encuentros -> Partido RM 3 - 1 FCB" 201 "$code" "$body"

# Test 8: Partido FCB 2 - 0 ATM
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/encuentros" \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 2, "equipoVisitanteId": 3, "golesLocal": 2, "golesVisitante": 0, "fecha": "2026-10-19"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 8 "POST /api/encuentros -> Partido FCB 2 - 0 ATM" 201 "$code" "$body"

# Test 9: Listar encuentros con nombres resueltos via Feign
res=$(curl -s -w "\n%{http_code}" -X GET "$PASARELA_URL/api/encuentros")
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 9 "GET /api/encuentros -> Listar encuentros con nombres" 200 "$code" "$body"

# Test 10: Validar tabla de posiciones
res=$(curl -s -w "\n%{http_code}" -X GET "$PASARELA_URL/api/encuentros/tabla-posiciones")
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 10 "GET /api/encuentros/tabla-posiciones -> Validar tabla calculada" 200 "$code" "$body"

# Test 11: Validación mismo equipo
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/encuentros" \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 1, "equipoVisitanteId": 1, "golesLocal": 0, "golesVisitante": 0, "fecha": "2026-10-19"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 11 "POST /api/encuentros -> Validación mismo equipo (local == visita)" 400 "$code" "$body"

# Test 12: Validación goles negativos
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/encuentros" \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 1, "equipoVisitanteId": 2, "golesLocal": -1, "golesVisitante": 2, "fecha": "2026-10-19"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 12 "POST /api/encuentros -> Validación goles negativos" 400 "$code" "$body"

# Test 13: Validación equipo inexistente (ID 999)
res=$(curl -s -w "\n%{http_code}" -X POST "$PASARELA_URL/api/encuentros" \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 999, "equipoVisitanteId": 2, "golesLocal": 1, "golesVisitante": 1, "fecha": "2026-10-19"}')
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 13 "POST /api/encuentros -> Validación equipo inexistente (ID 999)" 400 "$code" "$body"

echo -e "\n${YELLOW}--- 2. Pruebas Directas a los Servicios ---${NC}"

# Test 14: Acceso directo a servicio-equipos (8081)
res=$(curl -s -w "\n%{http_code}" -X GET "$EQUIPOS_URL/api/equipos")
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 14 "GET http://localhost:8081/api/equipos (Directo)" 200 "$code" "$body"

# Test 15: Acceso directo a servicio-encuentros (8082)
res=$(curl -s -w "\n%{http_code}" -X GET "$ENCUENTROS_URL/api/encuentros")
body=$(echo "$res" | sed '$d')
code=$(echo "$res" | tail -n1)
assert_status 15 "GET http://localhost:8082/api/encuentros (Directo)" 200 "$code" "$body"

echo -e "\n${BLUE}======================================================${NC}"
echo -e "${BLUE}                   RESUMEN DE PRUEBAS                 ${NC}"
echo -e "${BLUE}======================================================${NC}"
echo -e "Total pruebas ejecutadas : $total_tests"
echo -e "Pruebas exitosas         : ${GREEN}$passed_tests${NC}"
if [ "$passed_tests" -eq "$total_tests" ]; then
    echo -e "${GREEN}¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE!${NC}"
    exit 0
else
    echo -e "${RED}Hubo fallos en $((total_tests - passed_tests)) prueba(s).${NC}"
    exit 1
fi
