# 📘 Guía de Ejecución, Pruebas y Evidencias de Laboratorio

Este documento está diseñado como una guía paso a paso para que cualquier evaluador o desarrollador pueda descargar, ejecutar y validar el ecosistema de microservicios de la **Liga de Fútbol**, incluyendo la toma de capturas de pantalla como evidencia de funcionamiento.

---

## 📋 Requisitos Previos

- **Java Development Kit (JDK):** Versión 21 LTS instalada (`java -version`).
- **Git:** Para clonar el repositorio (`git --version`).
- **cURL** o **Postman:** Para ejecutar las pruebas HTTP.
- **Docker / MariaDB** *(Opcional)*: Para persistencia externa y visualización en phpMyAdmin.

---

## 🚀 Paso 1: Clonar y Compilar los Proyectos

1. **Clonar el repositorio:**
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd futbol-api-gateway
   ```

2. **Compilar y empaquetar los 4 componentes (usando Maven Wrapper incluido):**
   ```bash
   (cd eureka-server && ./mvnw clean package -DskipTests)
   (cd servicio-equipos && ./mvnw clean package -DskipTests)
   (cd servicio-encuentros && ./mvnw clean package -DskipTests)
   (cd pasarela && ./mvnw clean package -DskipTests)
   ```

> **📸 [Insertar Captura de Pantalla 1: Compilación exitosa BUILD SUCCESS de los 4 proyectos]**
> *(Captura de la terminal mostrando el mensaje BUILD SUCCESS tras compilar)*

---

## 🏃 Paso 2: Iniciar los Microservicios en Orden

Abre 4 terminales independientes (o ejecuta en segundo plano) en la carpeta raíz del proyecto:

### Terminal 1: Servidor de Descubrimiento Eureka (Puerto 8761)
```bash
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar
```
*Espera a que muestre: `Started EurekaServerApplication in X seconds`.*

### Terminal 2: Microservicio de Equipos (Puerto 8081)
```bash
java -jar servicio-equipos/target/servicio-equipos-0.0.1-SNAPSHOT.jar
```
*Implementa Rate Limiting (máx. 2 solicitudes por minuto).*

### Terminal 3: Microservicio de Encuentros (Puerto 8082)
```bash
java -jar servicio-encuentros/target/servicio-encuentros-0.0.1-SNAPSHOT.jar
```
*Restringido a red local (`server.address=127.0.0.1`).*

### Terminal 4: Pasarela / API Gateway (Puerto 8080)
```bash
java -jar pasarela/target/pasarela-0.0.1-SNAPSHOT.jar
```
*Enrutamiento dinámico mediante Spring Cloud Gateway MVC y LoadBalancer.*

---

## 🔍 Paso 3: Verificación de Registro en Eureka Server

Abre en tu navegador web:
👉 **[http://localhost:8761](http://localhost:8761)**

Verifica en la tabla **"Instances currently registered with Eureka"** que aparezcan los 3 servicios en estado **`UP`**:
- `PASARELA`
- `SERVICIO-EQUIPOS`
- `SERVICIO-ENCUENTROS`

O mediante cURL en la terminal:
```bash
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | jq .
```

> **📸 [Insertar Captura de Pantalla 2: Dashboard de Eureka con las 3 instancias en estado UP]**
> *(Captura del navegador en http://localhost:8761 mostrando las instancias registradas)*

---

## 🔒 Paso 4: Evidencia de Restricción a Red Local (Servicio 2)

El microservicio `servicio-encuentros` está configurado con `server.address=127.0.0.1` (Opción A), vinculándose exclusivamente a la interfaz local.

Ejecuta el siguiente comando para verificar los puertos abiertos en el sistema operativo:
```bash
ss -tulpn | grep 8082
```
*Salida esperada:*
```text
tcp   LISTEN 0   100   127.0.0.1:8082   0.0.0.0:*
```
*(Nota que indica `127.0.0.1:8082` en lugar de `0.0.0.0:8082` o `*:8082`, impidiendo el acceso desde interfaces externas a la máquina).*

> **📸 [Insertar Captura de Pantalla 3: Salida de ss / netstat mostrando el bind exclusivo a 127.0.0.1 en el puerto 8082]**

---

## ⏱️ Paso 5: Evidencia de Rate Limiting (Servicio 1 - Caso A)

El microservicio `servicio-equipos` implementa un filtro Token Bucket con **Bucket4j**, permitiendo un máximo de **2 solicitudes por minuto**. A partir de la 3ª solicitud, responde **`429 Too Many Requests`**.

Ejecuta tres peticiones consecutivas al endpoint directo o a través de la pasarela:

```bash
# Solicitud 1 (Exitosa - HTTP 200)
curl -i -X GET http://localhost:8081/api/equipos

# Solicitud 2 (Exitosa - HTTP 200)
curl -i -X GET http://localhost:8081/api/equipos

# Solicitud 3 (Bloqueada por Rate Limit - HTTP 429)
curl -i -X GET http://localhost:8081/api/equipos
```

*Respuesta esperada en la 3ª solicitud:*
```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json;charset=UTF-8

{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Límite de 2 solicitudes excedido para el servicio de equipos. Intente de nuevo más tarde."
}
```

> **📸 [Insertar Captura de Pantalla 4: Terminal ejecutando las 3 peticiones consecutivas y recibiendo HTTP 429 en la tercera]**

---

## ⚽ Paso 6: Pruebas Funcionales del Negocio (vía Pasarela :8080)

> [!NOTE]
> Tras comprobar el Rate Limit, espera 60 segundos para que el bucket de tokens se reponga, o realiza las peticiones respetando el intervalo.

### 6.1 Crear Equipos
```bash
# Equipo 1: Real Madrid
curl -i -X POST http://localhost:8080/api/equipos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Real Madrid"}'
```
*Esperado: `HTTP 201 Created` con `{"id":1,"nombre":"Real Madrid"}`.*

```bash
# Equipo 2: FC Barcelona
curl -i -X POST http://localhost:8080/api/equipos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "FC Barcelona"}'
```
*Esperado: `HTTP 201 Created`.*

```bash
# Equipo 3: Atlético Madrid
curl -i -X POST http://localhost:8080/api/equipos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Atlético Madrid"}'
```
*Esperado: `HTTP 201 Created`.*

> **📸 [Insertar Captura de Pantalla 5: Registro de equipos exitoso (HTTP 201)]**

---

### 6.2 Validación de Nombre Duplicado
```bash
curl -i -X POST http://localhost:8080/api/equipos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Real Madrid"}'
```
*Esperado: `HTTP 400 Bad Request` con mensaje "Ya existe un equipo registrado con el nombre: Real Madrid".*

> **📸 [Insertar Captura de Pantalla 6: Validación de nombre duplicado (HTTP 400)]**

---

### 6.3 Listar Equipos
```bash
curl -i -X GET http://localhost:8080/api/equipos
```
*Esperado: `HTTP 200 OK` con la lista de los 3 equipos.*

> **📸 [Insertar Captura de Pantalla 7: Listado de equipos en JSON (HTTP 200)]**

---

### 6.4 Registrar Encuentros (Comunicación OpenFeign)

El `servicio-encuentros` invoca internamente al `servicio-equipos` mediante OpenFeign para validar la existencia y obtener los nombres de los equipos:

```bash
# Partido 1: Real Madrid 3 - 1 FC Barcelona
curl -i -X POST http://localhost:8080/api/encuentros \
  -H "Content-Type: application/json" \
  -d '{
    "equipoLocalId": 1,
    "equipoVisitanteId": 2,
    "golesLocal": 3,
    "golesVisitante": 1,
    "fecha": "2026-10-19"
  }'
```
*Esperado: `HTTP 201 Created` con los nombres de ambos equipos resueltos.*

```bash
# Partido 2: FC Barcelona 2 - 0 Atlético Madrid
curl -i -X POST http://localhost:8080/api/encuentros \
  -H "Content-Type: application/json" \
  -d '{
    "equipoLocalId": 2,
    "equipoVisitanteId": 3,
    "golesLocal": 2,
    "golesVisitante": 0,
    "fecha": "2026-10-19"
  }'
```
*Esperado: `HTTP 201 Created`.*

> **📸 [Insertar Captura de Pantalla 8: Registro de partidos con nombres resueltos vía Feign (HTTP 201)]**

---

### 6.5 Consultar Tabla de Posiciones de la Liga
```bash
curl -i -X GET http://localhost:8080/api/encuentros/tabla-posiciones
```
*Esperado: `HTTP 200 OK` con la tabla calculada y ordenada según criterios oficiales (Puntos DESC, Dif. Goles DESC, Goles Favor DESC, Nombre ASC).*

```json
[
  {
    "equipoId": 1,
    "equipoNombre": "Real Madrid",
    "partidosJugados": 1,
    "partidosGanados": 1,
    "partidosEmpatados": 0,
    "partidosPerdidos": 0,
    "golesAFavor": 3,
    "golesEnContra": 1,
    "diferenciaGoles": 2,
    "puntos": 3
  },
  {
    "equipoId": 2,
    "equipoNombre": "FC Barcelona",
    "partidosJugados": 2,
    "partidosGanados": 1,
    "partidosEmpatados": 0,
    "partidosPerdidos": 1,
    "golesAFavor": 3,
    "golesEnContra": 3,
    "diferenciaGoles": 0,
    "puntos": 3
  },
  {
    "equipoId": 3,
    "equipoNombre": "Atlético Madrid",
    "partidosJugados": 1,
    "partidosGanados": 0,
    "partidosEmpatados": 0,
    "partidosPerdidos": 1,
    "golesAFavor": 0,
    "golesEnContra": 2,
    "diferenciaGoles": -2,
    "puntos": 0
  }
]
```

> **📸 [Insertar Captura de Pantalla 9: Tabla de posiciones calculada (HTTP 200)]**

---

### 6.6 Validaciones de Reglas de Negocio

- **Mismo equipo (Local == Visitante):**
  ```bash
  curl -i -X POST http://localhost:8080/api/encuentros \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 1, "equipoVisitanteId": 1, "golesLocal": 0, "golesVisitante": 0, "fecha": "2026-10-19"}'
  ```
  *Esperado: `HTTP 400 Bad Request` ("Un equipo no puede enfrentarse consigo mismo").*

- **Goles negativos:**
  ```bash
  curl -i -X POST http://localhost:8080/api/encuentros \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 1, "equipoVisitanteId": 2, "golesLocal": -1, "golesVisitante": 2, "fecha": "2026-10-19"}'
  ```
  *Esperado: `HTTP 400 Bad Request` ("Los goles no pueden ser nulos ni negativos").*

- **Equipo inexistente (validado por Feign):**
  ```bash
  curl -i -X POST http://localhost:8080/api/encuentros \
    -H "Content-Type: application/json" \
    -d '{"equipoLocalId": 999, "equipoVisitanteId": 2, "golesLocal": 1, "golesVisitante": 1, "fecha": "2026-10-19"}'
  ```
  *Esperado: `HTTP 400 Bad Request` ("No existe el equipo local con ID: 999").*

> **📸 [Insertar Captura de Pantalla 10: Validaciones de reglas de negocio en encuentros (HTTP 400)]**

---

## 🤖 Paso 7: Ejecución Automatizada de la Suite de Pruebas

Para validar automáticamente todos los casos anteriores en un solo paso:

```bash
./test_microservicios.sh
```

*Salida esperada:*
```text
======================================================
    BATERÍA DE PRUEBAS DE MICROSERVICIOS Y PASARELA    
======================================================
...
Total pruebas ejecutadas : 15
Pruebas exitosas         : 15
¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE!
```

> **📸 [Insertar Captura de Pantalla 11: Ejecución del script test_microservicios.sh con todas las pruebas en verde]**

---

## 🗄️ Paso 8 (Opcional): Persistencia y phpMyAdmin

Para visualizar los datos en phpMyAdmin usando MySQL / MariaDB:

1. **Levantar los servicios con el perfil MySQL activo:**
   ```bash
   java -jar servicio-equipos/target/servicio-equipos-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql &
   java -jar servicio-encuentros/target/servicio-encuentros-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql &
   ```
2. **Levantar phpMyAdmin vía Docker:**
   ```bash
   docker run -d --name phpmyadmin -p 8085:80 -e PMA_HOST=127.0.0.1 -e PMA_PORT=3306 phpmyadmin/phpmyadmin
   ```
3. **Acceder a [http://localhost:8085](http://localhost:8085):**
   - Usuario: `root`
   - Contraseña: *(vacía)*
   - Bases de datos creadas: `LigaEquiposDB` y `LigaEncuentrosDB`.

> **📸 [Insertar Captura de Pantalla 12: phpMyAdmin mostrando las bases de datos LigaEquiposDB y LigaEncuentrosDB]**
