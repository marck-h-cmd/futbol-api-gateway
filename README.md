# Arquitectura de Microservicios: Liga de Fútbol con Spring Cloud y Eureka (Solo Backend)

Este ecosistema backend distribuye la funcionalidad del monolito [`futbol_capas`](./futbol_capas) en una arquitectura pura de microservicios desacoplados utilizando **Spring Boot 3.3.5**, **Java 21** y **Spring Cloud 2023.0.3**. No incluye interfaz frontend, operando exclusivamente mediante APIs REST.

---

## 🏛️ Componentes Backend

| Componente | Puerto | Descripción | Tecnologías Clave |
|---|---|---|---|
| **`eureka-server`** | `8761` | Servidor central de registro y descubrimiento de servicios | Netflix Eureka Server |
| **`servicio-equipos`** | `8081` | Microservicio de gestión de equipos de fútbol | Eureka Client, Spring Data JPA, H2 / MySQL |
| **`servicio-encuentros`** | `8082` | Microservicio de partidos y cálculo de tabla de posiciones | Eureka Client, OpenFeign, Spring Data JPA, H2 / MySQL |
| **`pasarela`** | `8080` | API Gateway exclusivo para enrutamiento inteligente backend | Spring Cloud Gateway MVC, LoadBalancer |

---

## 🚀 Guía de Ejecución

### 1. Compilación y Empaquetado
Para compilar y empaquetar todos los proyectos:
```bash
(cd eureka-server && ./mvnw clean package -DskipTests)
(cd servicio-equipos && ./mvnw clean package -DskipTests)
(cd servicio-encuentros && ./mvnw clean package -DskipTests)
(cd pasarela && ./mvnw clean package -DskipTests)
```

### 2. Inicio de los Servicios Backend
```bash
# 1. Servidor de Descubrimiento (Puerto 8761)
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar

# 2. Microservicio de Equipos (Puerto 8081)
java -jar servicio-equipos/target/servicio-equipos-0.0.1-SNAPSHOT.jar

# 3. Microservicio de Encuentros (Puerto 8082)
java -jar servicio-encuentros/target/servicio-encuentros-0.0.1-SNAPSHOT.jar

# 4. Pasarela / API Gateway (Puerto 8080)
java -jar pasarela/target/pasarela-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Pruebas Automatizadas

El script [`test_microservicios.sh`](./test_microservicios.sh) ejecuta la batería de pruebas REST automatizadas contra la Pasarela y los microservicios:

```bash
./test_microservicios.sh
```

---

## 🌐 Endpoints REST Backend

- **Dashboard Eureka:** [http://localhost:8761](http://localhost:8761)
- **API Gateway (Pasarela):**
  - `GET / POST http://localhost:8080/api/equipos`
  - `GET http://localhost:8080/api/equipos/{id}`
  - `GET / POST http://localhost:8080/api/encuentros`
  - `GET http://localhost:8080/api/encuentros/tabla-posiciones`
- **Servicio Equipos Directo:** `GET / POST http://localhost:8081/api/equipos`
- **Servicio Encuentros Directo:** `GET / POST http://localhost:8082/api/encuentros`
