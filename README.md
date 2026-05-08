# 🌾 **AGROMAG - Plataforma Inteligente de Asesoría Agrícola**

> **Soluciones agrícolas impulsadas por Inteligencia Artificial para productores de la región del Magdalena, Colombia**

---

## 📋 Tabla de Contenidos

- [Descripción del Proyecto](#descripción-del-proyecto)
- [Características Principales](#características-principales)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Arquitectura](#arquitectura)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Entidades y Modelos de Datos](#entidades-y-modelos-de-datos)
- [Endpoints REST API](#endpoints-rest-api)
- [Guía de Instalación](#guía-de-instalación)
- [Configuración](#configuración)
- [Cómo Funciona](#cómo-funciona)
- [Flujos Principales](#flujos-principales)
- [Sincronización Offline](#sincronización-offline)
- [Autenticación y Seguridad](#autenticación-y-seguridad)
- [Manejo de Errores](#manejo-de-errores)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Contribución](#contribución)

---

## 📌 Descripción del Proyecto

**Agromag** es un backend REST que proporciona **recomendaciones agrícolas inteligentes** a productores rurales. El sistema:

✅ Registra **cultivos y eventos** (riego, fertilización, observaciones)  
✅ Genera **recomendaciones de riego** basadas en datos climáticos en tiempo real  
✅ Utiliza **IA (OpenAI)** para generar recomendaciones de fertilización contextualizadas  
✅ Consulta **datos climáticos** de Open-Meteo (temperatura, humedad)  
✅ Soporta **funcionamiento offline** con sincronización batch  
✅ Autentica usuarios con **JWT de Supabase** (OAuth2 Resource Server)  
✅ Garantiza **seguridad** (cada usuario solo accede a sus datos)  

**Ideal para**: Productores de banano, mango, yuca, plátano, maíz y palma en zonas con conectividad limitada.

---

## ⭐ Características Principales

### 1. **Gestión de Perfiles de Usuario**
- Auto-registro automático con JWT de Supabase
- Rol basado en acceso (PRODUCER, ADR_TECHNICIAN)
- Ubicación geográfica (30 municipios del Magdalena)
- Actualización de datos de perfil

### 2. **Gestión de Cultivos**
- CRUD completo (Crear, Leer, Actualizar, Eliminar)
- Soporte de 6 tipos de cultivos (Banano, Mango, Yuca, etc.)
- Registro de área en hectáreas y fecha de siembra
- Asociación a municipios específicos

### 3. **Registro de Eventos**
- Eventos de riego, fertilización, pesticidas y observaciones
- Cantidad y unidad customizable
- Notas descriptivas
- Historial ordenado por fecha

### 4. **Recomendaciones Inteligentes**

#### a) **Recomendaciones de Riego** (Lógica Determinista)
- Consulta clima en tiempo real (Open-Meteo)
- Compara temperatura y humedad vs. rangos óptimos del cultivo
- Genera alertas (LOW, MEDIUM, HIGH risk)
- Incluye sugerencias de cantidad de riego

#### b) **Recomendaciones de Fertilización** (Impulsadas por IA)
- Utiliza OpenAI (via OpenRouter) con modelo gpt-4o-mini
- Analiza etapa del cultivo (basada en semanas desde siembra)
- Datos climáticos y parámetros agronómicos
- Genera nutrientes específicos y dosis recomendadas

### 5. **Sincronización Offline-First**
- Los cambios se almacenan localmente en la app móvil
- Sincronización por lotes (batch) cuando hay conexión
- Evita duplicados mediante batch check
- Confirma éxito de sincronización

### 6. **Auditoría y Tracking**
- Timestamps (createdAt, updatedAt)
- Status de sincronización (PENDING, SYNCED)
- Registro de decisiones del usuario

---

## 🛠️ Tecnologías Utilizadas

| Capa | Tecnología | Versión | Propósito |
|------|-----------|---------|----------|
| **Framework** | Spring Boot | 4.0.6 | Backend REST |
| **Lenguaje** | Java | 21 | Lenguaje de programación |
| **Base de Datos** | PostgreSQL | (Supabase) | Almacenamiento persistente |
| **ORM** | Hibernate/JPA | (spring-data-jpa) | Mapeo objeto-relacional |
| **Autenticación** | Supabase Auth | JWT | Validación de identidad |
| **Security** | Spring Security | OAuth2 | Control de acceso |
| **Validación** | Jakarta Validation | v3.0 | Validación de datos |
| **Cliente HTTP** | Spring WebFlux | (WebClient) | Llamadas a APIs externas |
| **IA** | Spring AI | 2.0.0-M5 | Integración con OpenAI |
| **Clima** | Open-Meteo API | v1 | Datos climáticos |
| **Serialización** | Jackson | (spring-boot) | Parseador JSON |
| **Logging** | SLF4J** | (spring-boot) | Rastreo de eventos |
| **Build** | Maven | 3.0+ | Gestor de dependencias |
| **Utilidades** | Lombok | (spring-boot) | Reducción de boilerplate |

---

## 🏗️ Arquitectura

La arquitectura sigue el patrón **Layered Architecture (n-capas)**:

```
┌─────────────────────────────────────────────────────────┐
│            CLIENT LAYER (Mobile/Web)                    │
│  • App móvil con almacenamiento offline (SQLite)       │
│  • Genera UUIDs locales para nuevos registros          │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP + JWT Token
                       ↓
┌─────────────────────────────────────────────────────────┐
│          PRESENTATION LAYER (Controllers)               │
│  • REST Endpoints (@RestController)                    │
│  • Validación de DTOs (@Valid)                        │
│  • Conversión de respuestas (DTO pattern)             │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────┐
│          BUSINESS LOGIC LAYER (Services)                │
│  • Validación de pertenencia (Authorization)           │
│  • Orquestación de operaciones                        │
│  • Consulta a APIs externas                          │
│  • Generación de IA                                   │
│  • Transacciones (@Transactional)                    │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────┐
│        PERSISTENCE LAYER (Repositories)                 │
│  • JPA Spring Data (@Repository)                      │
│  • Consultas a base de datos                         │
│  • Entity Mapping con Hibernate                      │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────┐
│          DATA LAYER (Database + APIs)                   │
│  ├─ PostgreSQL (Supabase) - Datos persistentes        │
│  ├─ Open-Meteo API - Datos climáticos                │
│  ├─ OpenAI (OpenRouter) - Inteligencia artificial    │
│  └─ Supabase Auth - Autenticación JWT               │
└─────────────────────────────────────────────────────────┘
```

### Flujo de una Solicitud

```
Request → Security Filter (JWT validation)
   ↓
Controller (Recibe DTO)
   ↓
Service (Validación + Lógica de negocio)
   ↓
Repository (Acceso a datos)
   ↓
Database (Persistencia)
   ↓
Service (Procesamiento de respuesta)
   ↓
Controller (Conversión a DTO)
   ↓
Response (JSON)
```

---

## 📁 Estructura del Proyecto

```
agromag/
├── src/
│   └── main/
│       ├── java/com/agromag/
│       │   ├── AgromagApplication.java              # Punto de entrada
│       │   │
│       │   ├── config/                              # Configuración
│       │   │   ├── SecurityConfig.java              # JWT + OAuth2
│       │   │   ├── JwtAuthenticationConverter.java  # Conversión JWT → Principal
│       │   │   ├── WebClientConfig.java             # HTTP Client + ObjectMapper
│       │   │   └── OpenMeteoProperties.java         # Props de Open-Meteo
│       │   │
│       │   ├── controller/                          # REST Endpoints
│       │   │   ├── ProfileController.java           # GET/PUT perfil
│       │   │   ├── CropController.java              # CRUD cultivos
│       │   │   ├── CropEventController.java         # CRUD eventos
│       │   │   ├── RecommendationController.java    # Recomendaciones
│       │   │   └── SyncController.java              # Sincronización
│       │   │
│       │   ├── domain/                              # Modelado de datos
│       │   │   ├── entities/                        # Tablas JPA
│       │   │   │   ├── Profile.java                 # Usuarios
│       │   │   │   ├── Crop.java                    # Cultivos
│       │   │   │   ├── CropEvent.java               # Eventos
│       │   │   │   ├── Recommendation.java          # Recomendaciones
│       │   │   │   └── CropParameter.java           # Parámetros de cultivo
│       │   │   │
│       │   │   └── enums/                           # Enumerados
│       │   │       ├── Role.java                    # PRODUCER, ADR_TECHNICIAN
│       │   │       ├── CropType.java                # Tipos de cultivo
│       │   │       ├── EventType.java               # Tipos de evento
│       │   │       ├── RecommendationType.java      # Tipos de recomendación
│       │   │       ├── RiskLevel.java               # Niveles de riesgo
│       │   │       ├── SyncStatus.java              # Estado de sincronización
│       │   │       └── Municipality.java            # Municipios + coordenadas
│       │   │
│       │   ├── dto/                                 # Data Transfer Objects
│       │   │   ├── request/                         # Entrada (API contracts)
│       │   │   │   ├── CropRequest.java
│       │   │   │   ├── CropEventRequest.java
│       │   │   │   ├── ProfileUpdateRequest.java
│       │   │   │   ├── RecommendationDecisionRequest.java
│       │   │   │   └── SyncBatchRequest.java
│       │   │   │
│       │   │   └── response/                        # Salida (API contracts)
│       │   │       ├── ProfileResponse.java
│       │   │       ├── CropResponse.java
│       │   │       ├── CropEventResponse.java
│       │   │       ├── RecommendationResponse.java
│       │   │       ├── IrrigationRecommendationResponse.java
│       │   │       ├── FertilizerRecommendationResponse.java
│       │   │       └── SyncBatchResponse.java
│       │   │
│       │   ├── service/                             # Lógica de negocio
│       │   │   ├── ProfileService.java              # Gestión de perfiles
│       │   │   ├── CropService.java                 # CRUD de cultivos
│       │   │   ├── CropEventService.java            # CRUD de eventos
│       │   │   ├── RecommendationService.java       # Orquestación recomendaciones
│       │   │   ├── AiRecommendationService.java     # Integración OpenAI
│       │   │   ├── ClimateService.java              # Integración Open-Meteo
│       │   │   ├── ClimateData.java                 # Record de datos climáticos
│       │   │   └── SyncService.java                 # Sincronización batch
│       │   │
│       │   ├── repository/                          # Acceso a datos (JPA)
│       │   │   ├── ProfileRepository.java           # findById, custom queries
│       │   │   ├── CropRepository.java              # findByProfile_Id, etc.
│       │   │   ├── CropEventRepository.java         # findByCrop_Id, etc.
│       │   │   ├── CropParameterRepository.java     # findByCropType
│       │   │   └── RecommendationRepository.java    # findByCrop_Id, etc.
│       │   │
│       │   ├── exception/                           # Manejo de errores
│       │   │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice
│       │   │   ├── ResourceNotFoundException.java
│       │   │   ├── UnauthorizedCropAccessException.java
│       │   │   ├── SyncConflictException.java
│       │   │   ├── ClimateServiceException.java
│       │   │   └── AiServiceException.java
│       │   │
│       │   └── util/                                # Utilidades
│       │       └── SecurityUtils.java               # Extrae userId del JWT
│       │
│       └── resources/
│           └── application.properties               # Configuración Spring
│
├── pom.xml                                          # Dependencias Maven
└── README.md                                        # Este archivo
```

---

## 📊 Entidades y Modelos de Datos

### **Profile** (Perfil de Usuario)
```
┌────────────────────────────────────────┐
│           PROFILE                      │
├────────────────────────────────────────┤
│ id (UUID, PK)                          │ ← Sub del JWT
│ email (String, UNIQUE)                 │
│ fullName (String)                      │
│ role (PRODUCER | ADR_TECHNICIAN)       │
│ municipality (Municipality, 30 opciones)│
│ createdAt (LocalDateTime)              │
│ crops (List<Crop> 1:N)                 │
└────────────────────────────────────────┘
```

**Flujo**: Usuario se conecta → JWT → Auto-registro → Profile creado

---

### **Crop** (Cultivo)
```
┌────────────────────────────────────────┐
│            CROP                        │
├────────────────────────────────────────┤
│ id (UUID, PK)                          │ ← Generado por cliente
│ profileId (UUID, FK)                   │ ← Propietario
│ cropType (BANANO|MANGO|YUCA|...)       │
│ areaHectares (BigDecimal)              │
│ municipality (Municipality)            │
│ sownDate (LocalDate)                   │
│ syncStatus (PENDING | SYNCED)          │
│ createdAt, updatedAt (LocalDateTime)   │
│ events (List<CropEvent> 1:N)           │
│ recommendations (List<Rec> 1:N)        │
└────────────────────────────────────────┘
```

**Relación**: 1 Profile ↔ N Crops

---

### **CropEvent** (Evento del Cultivo)
```
┌────────────────────────────────────────┐
│         CROP_EVENT                     │
├────────────────────────────────────────┤
│ id (UUID, PK)                          │ ← Generado por cliente
│ cropId (UUID, FK)                      │
│ eventType (IRRIGATION|FERTILIZER|...)  │
│ quantity (BigDecimal)                  │
│ unit (String, e.g., "liters", "kg")    │
│ notes (String, máx 2000 chars)         │
│ occurredAt (LocalDateTime)             │
│ syncStatus (PENDING | SYNCED)          │
└────────────────────────────────────────┘
```

**Uso**: Productor registra manualmente qué acciones realizó.

---

### **Recommendation** (Recomendación Generada)
```
┌────────────────────────────────────────┐
│      RECOMMENDATION                    │
├────────────────────────────────────────┤
│ id (UUID, PK)                          │
│ cropId (UUID, FK)                      │
│ type (IRRIGATION|FERTILIZER|PHYTO)     │
│ level (LOW | MEDIUM | HIGH)            │
│ message (String, máx 2000 chars)       │
│ followed (Boolean, null = no leída)    │
│ temperature (BigDecimal)               │
│ humidity (BigDecimal)                  │
│ generatedAt (LocalDateTime)            │
│ syncStatus (PENDING | SYNCED)          │
└────────────────────────────────────────┘
```

**Origen**: Sistema (riego o IA), no es entrada del usuario.

---

### **CropParameter** (Parámetros de Referencia)
```
┌────────────────────────────────────────┐
│      CROP_PARAMETER                    │
├────────────────────────────────────────┤
│ id (Long, PK)                          │
│ cropType (UNIQUE)                      │
│ suggestedSpacing (String)              │
│ growthCycleDays (Integer)              │
│ optimalTempMin/Max (BigDecimal)        │
│ humidityMin/Max (BigDecimal)           │
│ phMin/Max (BigDecimal)                 │
│ ecMin/Max (BigDecimal)                 │
│ irrigationNeeds (String)               │
│ recommendedFertilizer (String)         │
│ plantingDepthCm (BigDecimal)           │
└────────────────────────────────────────┘
```

**Nota**: Tabla de referencia (no editable por usuarios). Se carga al iniciar la app.

---

### Diagrama de Relaciones

```
                    ┌──────────────┐
                    │   PROFILE    │
                    │              │
                    │ id (PK, UUID)│
                    │ email        │
                    │ fullName     │
                    │ role         │
                    └──────┬───────┘
                           │ 1:N
                           ↓
                    ┌──────────────┐
                    │    CROP      │
                    │              │
                    │ id (PK, UUID)│
                    │ profileId(FK)│
                    │ cropType     │
                    │ sownDate     │
                    └──────┬───────┘
                           │ 1:N
                ┌──────────┴──────────┐
                ↓                     ↓
        ┌────────────────┐  ┌──────────────────┐
        │  CROP_EVENT    │  │ RECOMMENDATION   │
        │                │  │                  │
        │ id (PK, UUID)  │  │ id (PK, UUID)    │
        │ cropId (FK)    │  │ cropId (FK)      │
        │ eventType      │  │ type             │
        │ quantity       │  │ level            │
        └────────────────┘  │ message          │
                            │ followed         │
                            └──────────────────┘

        ┌──────────────────────┐
        │  CROP_PARAMETER      │
        │                      │
        │ id (PK, Long)        │
        │ cropType (UNIQUE)    │
        │ (referencia maestra) │
        └──────────────────────┘
```

---

## 🔌 Endpoints REST API

### **1. Perfil de Usuario**

```http
GET /api/profile
Authorization: Bearer <JWT_TOKEN>
```
**Descripción**: Obtiene el perfil del usuario autenticado. Si es la primera vez, lo crea automáticamente.  
**Response**: `ProfileResponse`

```http
PUT /api/profile
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "fullName": "Juan Pérez",
  "municipality": "ZONA_BANANERA"
}
```
**Descripción**: Actualiza nombre completo y municipio.  
**Response**: `ProfileResponse`

---

### **2. Cultivos**

```http
POST /api/crops
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "cropType": "BANANO",
  "areaHectares": 5.5,
  "municipality": "ZONA_BANANERA",
  "sownDate": "2024-12-15"
}
```
**Response**: `CropResponse` (201 CREATED)

---

```http
GET /api/crops
Authorization: Bearer <JWT_TOKEN>
```
**Response**: `List<CropResponse>`

---

```http
GET /api/crops/{cropId}
Authorization: Bearer <JWT_TOKEN>
```
**Response**: `CropResponse`

---

```http
PUT /api/crops/{cropId}
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "cropType": "MANGO",
  "areaHectares": 8.0,
  "municipality": "SANTA_MARTA",
  "sownDate": "2024-12-10"
}
```
**Response**: `CropResponse`

---

```http
DELETE /api/crops/{cropId}
Authorization: Bearer <JWT_TOKEN>
```
**Response**: 204 NO CONTENT

---

### **3. Eventos de Cultivo**

```http
POST /api/crops/{cropId}/events
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "id": "660e9500-f39c-52e4-b817-557766551111",
  "cropId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "IRRIGATION",
  "quantity": 100,
  "unit": "liters",
  "notes": "Riego por goteo",
  "occurredAt": "2025-01-15T14:00:00"
}
```
**Response**: `CropEventResponse` (201 CREATED)

---

```http
GET /api/crops/{cropId}/events
Authorization: Bearer <JWT_TOKEN>
```
**Response**: `List<CropEventResponse>` (ordenado por fecha descendente)

---

```http
GET /api/crops/{cropId}/events/{eventId}
Authorization: Bearer <JWT_TOKEN>
```
**Response**: `CropEventResponse`

---

```http
PUT /api/crops/{cropId}/events/{eventId}
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "id": "660e9500-f39c-52e4-b817-557766551111",
  "cropId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "FERTILIZER",
  "quantity": 50,
  "unit": "kg",
  "notes": "NPK 15-15-15",
  "occurredAt": "2025-01-15T15:30:00"
}
```
**Response**: `CropEventResponse`

---

```http
DELETE /api/crops/{cropId}/events/{eventId}
Authorization: Bearer <JWT_TOKEN>
```
**Response**: 204 NO CONTENT

---

### **4. Recomendaciones**

```http
GET /api/crops/{cropId}/recommendations
Authorization: Bearer <JWT_TOKEN>
```
**Response**: `List<RecommendationResponse>`

---

```http
POST /api/crops/{cropId}/recommendations/irrigation
Authorization: Bearer <JWT_TOKEN>
```
**Descripción**: Genera recomendación de riego basada en datos climáticos.  
**Response**: `IrrigationRecommendationResponse` (201 CREATED)

**Ejemplo de respuesta**:
```json
{
  "id": "770fa611-g40d-63f5-c928-668877662222",
  "cropId": "550e8400-e29b-41d4-a716-446655440000",
  "level": "HIGH",
  "message": "¡Alerta! Temperatura: 34.5°C (máx óptima: 30°C), Humedad: 55% (mín óptima: 65%). Se recomienda riego inmediato.",
  "temperature": 34.5,
  "generatedAt": "2025-01-15T14:05:00"
}
```

---

```http
POST /api/crops/{cropId}/recommendations/fertilizer
Authorization: Bearer <JWT_TOKEN>
```
**Descripción**: Genera recomendación de fertilización usando IA (OpenAI).  
**Response**: `FertilizerRecommendationResponse` (201 CREATED)

**Ejemplo de respuesta**:
```json
{
  "id": "880gb722-h51e-74g6-d039-779988773333",
  "cropId": "550e8400-e29b-41d4-a716-446655440000",
  "cropStage": "Floración",
  "weeksSinceSowing": 8,
  "recommendedNutrient": "Potasio (K)",
  "recommendedDose": "150 kg/ha",
  "level": "HIGH",
  "message": "En fase de floración, el potasio incrementa la calidad del fruto y robustez de la planta.",
  "generatedAt": "2025-01-15T14:10:00"
}
```

---

```http
PATCH /api/recommendations/decision
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "recommendationId": "770fa611-g40d-63f5-c928-668877662222",
  "followed": true
}
```
**Descripción**: Registra si el usuario siguió una recomendación.  
**Response**: 204 NO CONTENT

---

### **5. Sincronización (Offline-First)**

```http
POST /api/sync/batch
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "crops": [
    {
      "id": "990hc833-i62f-85h7-e140-880099884444",
      "cropType": "YUCA",
      "areaHectares": 3.0,
      "municipality": "CIENAGA",
      "sownDate": "2025-01-10"
    }
  ],
  "events": [
    {
      "id": "aa1id944-j73g-96i8-f251-991100995555",
      "cropId": "550e8400-e29b-41d4-a716-446655440000",
      "eventType": "OBSERVATION",
      "quantity": null,
      "unit": null,
      "notes": "Plagas detectadas",
      "occurredAt": "2025-01-15T16:00:00"
    }
  ],
  "decisions": [
    {
      "recommendationId": "880gb722-h51e-74g6-d039-779988773333",
      "followed": false
    }
  ]
}
```
**Descripción**: Envía cambios locales acumulados para sincronizar con servidor.  
**Response**: `SyncBatchResponse`

**Ejemplo de respuesta**:
```json
{
  "status": "OK",
  "message": "Sincronización completada: 1 cultivo, 1 evento, 1 decisión procesada",
  "syncedCrops": [...],
  "syncedEvents": [...],
  "timestamp": "2025-01-15T16:05:00"
}
```

---

## 🚀 Guía de Instalación

### **Requisitos Previos**

- **Java 21** o superior
- **Maven 3.8.0** o superior
- **PostgreSQL 12+** (o Supabase PostgreSQL)
- **Git**
- **Cuenta en Supabase** (para autenticación y BD)
- **API Key de OpenRouter/OpenAI** (para IA)

### **Paso 1: Clonar el Repositorio**

```bash
git clone https://github.com/tu-usuario/agromag.git
cd agromag
```

### **Paso 2: Crear Archivo `.env`**

En la raíz del proyecto, crea un archivo `.env`:

```env
# Supabase Database
SUPABASE_JDBC_URL=jdbc:postgresql://db.supabasehost.com:5432/postgres
SUPABASE_PROJECT_REF=your-project-ref
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=your-database-password

# OpenAI / OpenRouter API
OPENAI_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxx
```

**Nota**: El proyecto usa `spring-dotenv` para cargar automáticamente variables del archivo `.env`.

### **Paso 3: Compilar el Proyecto**

```bash
mvn clean compile
```

### **Paso 4: Ejecutar Migraciones (si es necesario)**

Si usas Flyway o Liquibase, ejecuta:

```bash
mvn db:migrate
```

### **Paso 5: Iniciar la Aplicación**

```bash
mvn spring-boot:run
```

O directamente con Java:

```bash
mvn clean install
java -jar target/agromag-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en `http://localhost:8080`

### **Paso 6: Verificar que Está Corriendo**

```bash
curl http://localhost:8080/api/profile \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## ⚙️ Configuración

### **application.properties**

```properties
# Información de la aplicación
spring.application.name=agromag

# Base de datos Supabase
spring.datasource.url=${SUPABASE_JDBC_URL}&prepareThreshold=0
spring.datasource.username=${SUPABASE_DB_USER:postgres}
spring.datasource.password=${SUPABASE_DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate/JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# Supabase Auth - JWT Validation
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://${SUPABASE_PROJECT_REF}.supabase.co/auth/v1/.well-known/jwks.json
spring.security.oauth2.resourceserver.jwt.jws-algorithms=ES256

# Open-Meteo API
openmeteo.url=https://api.open-meteo.com/v1/forecast

# Spring AI - OpenRouter (proxy compatible con OpenAI)
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.base-url=https://openrouter.ai/api/v1
spring.ai.openai.chat.options.model=openai/gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.3

# Seed data
spring.jpa.defer-datasource-initialization=true

# Seguridad - NO exponer detalles internos de errores
server.error.include-message=never
server.error.include-exception=false
server.error.include-binding-errors=never

# Logging
logging.file.name=spring.log
```

### **Variables de Entorno Necesarias**

| Variable | Descripción | Ejemplo |
|----------|-----------|---------|
| `SUPABASE_JDBC_URL` | URL de conexión JDBC a PostgreSQL | `jdbc:postgresql://db.supabasehost.com:5432/postgres` |
| `SUPABASE_PROJECT_REF` | Referencia del proyecto Supabase | `wvqfqkqvxkzqbqxw` |
| `SUPABASE_DB_USER` | Usuario de BD | `postgres` |
| `SUPABASE_DB_PASSWORD` | Contraseña de BD | `your-secure-password` |
| `OPENAI_API_KEY` | API Key de OpenRouter | `sk-or-v1-...` |

---

## 🎯 Cómo Funciona

### **Flujo General de una Solicitud**

```
1. Cliente (app móvil/web) envía HTTP request con JWT token
                    ↓
2. SecurityConfig intercepta y valida JWT con JWKS de Supabase
                    ↓
3. JwtAuthenticationConverter extrae UUID (sub) y roles
                    ↓
4. Controller recibe @PathVariable, @RequestBody, Principal
                    ↓
5. Extrae userId del Principal (SecurityUtils.getCurrentUserId)
                    ↓
6. Service valida acceso (ownership check)
                    ↓
7. Service ejecuta lógica de negocio
                    ↓
8. Repository interactúa con BD
                    ↓
9. Service retorna DTO (nunca la entidad)
                    ↓
10. Controller envía ResponseEntity<DTO>
                    ↓
11. GlobalExceptionHandler maneja excepciones si hay
                    ↓
12. JSON se serializa y envía al cliente
```

---

## 📊 Flujos Principales

### **Flujo 1: Creación de Cultivo**

```
POST /api/crops
  ↓
CropController.createCrop(principal, request)
  ├─ userId = SecurityUtils.getCurrentUserId(principal)
  ├─ CropService.createCrop(userId, request)
  │  ├─ ProfileService.getProfileById(userId)
  │  ├─ Valida que request tiene todos los datos
  │  ├─ Crea entidad Crop
  │  ├─ CropRepository.save(crop)
  │  └─ Log: "create_crop cropId=... profileId=... type=..."
  ├─ CropResponse.from(crop) — conversión a DTO
  └─ Retorna 201 CREATED
```

---

### **Flujo 2: Generar Recomendación de Riego**

```
POST /api/crops/{cropId}/recommendations/irrigation
  ↓
RecommendationController.generateIrrigation(principal, cropId)
  ├─ userId = SecurityUtils.getCurrentUserId(principal)
  ├─ RecommendationService.generateIrrigation(cropId, userId)
  │  ├─ CropService.findCropAndValidateOwnership(cropId, userId)
  │  ├─ CropParameterRepository.findByCropType(crop.cropType)
  │  ├─ ClimateService.getCurrentClimate(crop.municipality)
  │  │  ├─ Consulta Open-Meteo API
  │  │  ├─ Envía: latitude, longitude, current
  │  │  ├─ Recibe: temperature_2m, relative_humidity_2m
  │  │  └─ Retorna ClimateData(temp, humidity)
  │  │
  │  ├─ Comparar temp/humedad vs. rangos óptimos
  │  │  ├─ Si temp > tempMax O humidity < humMin → HIGH
  │  │  ├─ Si temp cerca de tempMax → MEDIUM
  │  │  └─ Caso contrario → LOW
  │  │
  │  ├─ Generar mensaje personalizado
  │  ├─ Crear Recommendation
  │  ├─ RecommendationRepository.save()
  │  ├─ Log: "generate_irrigation cropId=... level=..."
  │  └─ Retorna IrrigationRecommendationResponse
  │
  └─ Retorna 201 CREATED
```

**Datos Enviados a Open-Meteo**:
```
GET https://api.open-meteo.com/v1/forecast
  ?latitude=10.7617
  &longitude=-74.1556
  &current=temperature_2m,relative_humidity_2m
```

---

### **Flujo 3: Generar Recomendación de Fertilización (Con IA)**

```
POST /api/crops/{cropId}/recommendations/fertilizer
  ↓
RecommendationController.generateFertilizer(principal, cropId)
  ├─ userId = SecurityUtils.getCurrentUserId(principal)
  ├─ RecommendationService.generateFertilizer(cropId, userId)
  │  ├─ CropService.findCropAndValidateOwnership(cropId, userId)
  │  ├─ CropParameterRepository.findByCropType(crop.cropType)
  │  ├─ ClimateService.getCurrentClimate(crop.municipality)
  │  ├─ Calcular semanas desde siembra
  │  ├─ AiRecommendationService.generateFertilizerRecommendation()
  │  │  ├─ buildFertilizerPrompt(crop, params, climate, weeks)
  │  │  │  └─ Crea prompt agronómico especializado
  │  │  │
  │  │  ├─ ChatClient.prompt().user(prompt).call().content()
  │  │  │  └─ Envía a OpenRouter (OpenAI gpt-4o-mini)
  │  │  │
  │  │  ├─ Recibe JSON:
  │  │  │  {
  │  │  │    "cropStage": "Floración",
  │  │  │    "recommendedNutrient": "K",
  │  │  │    "recommendedDose": "150 kg/ha",
  │  │  │    "level": "HIGH",
  │  │  │    "message": "..."
  │  │  │  }
  │  │  │
  │  │  ├─ Limpia markdown si hay (```)
  │  │  ├─ Parsea con ObjectMapper (manejo seguro de campos)
  │  │  └─ Retorna FertilizerRecommendationResponse
  │  │
  │  ├─ Crear Recommendation
  │  ├─ RecommendationRepository.save()
  │  ├─ Log: "generate_fertilizer cropId=... level=..."
  │  └─ Retorna FertilizerRecommendationResponse
  │
  └─ Retorna 201 CREATED
```

**Prompt Enviado a OpenAI** (simplificado):
```
Eres agrónomo experto en cultivos tropicales del Magdalena, Colombia.

Datos del cultivo:
- Tipo: BANANO
- Siembra: 2024-12-15
- Semanas: 8
- Área: 5.5 hectáreas
- Municipio: Zona Bananera
- ...parámetros agronómicos...

Condiciones climáticas actuales:
- Temperatura: 34.5°C
- Humedad: 55%

Proporciona recomendación de fertilización en formato JSON...
```

---

### **Flujo 4: Sincronización Offline**

```
POST /api/sync/batch
  ↓
SyncController.syncBatch(principal, request)
  ├─ userId = SecurityUtils.getCurrentUserId(principal)
  ├─ SyncService.processBatch(userId, request)
  │  │
  │  ├─ Log: "sync_batch_start profileId=... crops=... events=..."
  │  │
  │  ├─ 1. SINCRONIZAR CULTIVOS
  │  │  ├─ Batch check: ¿Cuáles IDs ya existen?
  │  │  ├─ cropRepository.findAllById(requestedCropIds)
  │  │  ├─ Para cada cultivo NO existente:
  │  │  │  ├─ CropService.createCrop(userId, cropReq)
  │  │  │  └─ Agrega a syncedCrops
  │  │  │
  │  │  ├─ 2. SINCRONIZAR EVENTOS
  │  │  │  ├─ cropEventRepository.findAllById(requestedEventIds)
  │  │  │  ├─ Para cada evento NO existente:
  │  │  │  │  ├─ CropEventService.createEvent(userId, eventReq)
  │  │  │  │  └─ Agrega a syncedEvents
  │  │  │  │
  │  │  │  ├─ 3. PROCESAR DECISIONES
  │  │  │  │  ├─ Para cada decisión:
  │  │  │  │  │  └─ RecommendationService.markDecision()
  │  │  │  │  │
  │  │  ├─ Log: "sync_batch_done synced_crops=... synced_events=..."
  │  │  │
  │  │  └─ Retorna SyncBatchResponse
  │  │
  │  └─ Retorna 200 OK
```

---

## 🔄 Sincronización Offline

El sistema soporta **funcionamiento sin conexión** con posterior sincronización.

### **¿Cómo Funciona?**

**Fase 1: Offline (Sin conexión)**
```
1. Usuario registra evento local
2. App genera UUID (no espera al servidor)
3. Almacena en storage local (SQLite/localStorage)
4. Marca como PENDING (pendiente de sincronizar)
5. Usuario continúa trabajando
```

**Fase 2: Online (Vuelve la conexión)**
```
1. App detecta conexión a internet
2. Acumula todos los cambios en un JSON batch
3. Envía POST /api/sync/batch
4. Servidor:
   a. Batch check: ¿Cuáles IDs ya existen?
   b. Inserta solo novedades (evita duplicados)
   c. Marca cambios como SYNCED
   d. Retorna confirmación
5. App marca localmente como SYNCED
```

### **Estructura del Batch**

```json
{
  "crops": [
    {
      "id": "uuid-local-generado",
      "cropType": "BANANO",
      "areaHectares": 5.5,
      "municipality": "ZONA_BANANERA",
      "sownDate": "2024-12-15"
    }
  ],
  "events": [
    {
      "id": "uuid-local-generado",
      "cropId": "uuid-del-cultivo",
      "eventType": "IRRIGATION",
      "quantity": 100,
      "unit": "liters",
      "occurredAt": "2025-01-15T14:00:00"
    }
  ],
  "decisions": [
    {
      "recommendationId": "uuid-recomendacion",
      "followed": true
    }
  ]
}
```

### **Ventajas**

✅ Funciona sin conexión a internet  
✅ Evita pérdida de datos  
✅ No hay duplicados (batch check)  
✅ Sincronización automática cuando vuelve conexión  
✅ Experiencia fluida en zonas rurales  

---

## 🔐 Autenticación y Seguridad

### **Flujo de Autenticación JWT**

```
1. Usuario ingresa credenciales en app
                ↓
2. App envía a Supabase Auth
                ↓
3. Supabase valida y emite JWT token con:
   {
     "sub": "550e8400-e29b-41d4-a716-446655440000",  (UUID usuario)
     "email": "productor@example.com",
     "app_metadata": {
       "role": "PRODUCER"
     }
   }
                ↓
4. App incluye en cada request:
   Authorization: Bearer <token>
                ↓
5. SecurityConfig intercepta:
   - Valida firma con JWKS de Supabase
   - Extrae UUID del "sub"
   - Valida algoritmo (ES256)
   - Crea JwtAuthenticationToken
                ↓
6. SecurityUtils.getCurrentUserId(principal)
   - Extrae UUID del token
   - Se usa para validar acceso a datos
```

### **Validaciones de Acceso**

```java
// ✓ Cada usuario solo ve SUS cultivos
CropService.findCropAndValidateOwnership(cropId, userId)
    → Si crop.profile.id != userId → EXCEPCIÓN 403

// ✓ Cada usuario solo ve SUS eventos
CropEventService.getEventById(eventId, userId)
    → Valida que el cultivo pertenece al usuario

// ✓ Roles controlados
SecurityConfig:
    /api/adr/**        → solo ADR_TECHNICIAN
    /api/**            → todos autenticados
    /error, /auth/**   → públicos
```

### **Mejores Prácticas de Seguridad Implementadas**

✅ **CSRF deshabilitado** (API REST, no necesita cookies)  
✅ **Session Stateless** (solo JWT, no sesiones en servidor)  
✅ **OAuth2 Resource Server** (validación centralizada)  
✅ **Role-based access control** (PRODUCER, ADR_TECHNICIAN)  
✅ **Ownership validation** (cada usuario solo accede a sus datos)  
✅ **No exponemos detalles de errores** (sanitizados para cliente)  
✅ **Validación de DTOs** (Jakarta Validation)  
✅ **Encriptación en tránsito** (HTTPS en producción)  

---

## ❌ Manejo de Errores

Todos los errores se centralizan en `GlobalExceptionHandler` y retornan JSON estructurado.

### **Excepciones Mapeadas**

| Excepción | Código HTTP | Descripción |
|-----------|-------------|-----------|
| `ResourceNotFoundException` | 404 | Recurso no encontrado (cultivo, evento, etc.) |
| `UnauthorizedCropAccessException` | 403 | Usuario intenta acceder a cultivo ajeno |
| `SyncConflictException` | 409 | Conflicto en sincronización |
| `ClimateServiceException` | 502 | Falla al consultar Open-Meteo |
| `AiServiceException` | 502 | Falla al consultar OpenAI |
| `MethodArgumentNotValidException` | 400 | DTO inválido (validación Jakarta) |
| `IllegalArgumentException` | 400 | Argumentos inválidos |
| `Exception` (genérica) | 500 | Error interno no mapeado |

### **Formato de Respuesta de Error**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Cultivo con ID 'abc123' no encontrado",
  "timestamp": "2025-01-15T10:45:30"
}
```

### **Error de Validación (DTO inválido)**

```json
{
  "status": 400,
  "error": "Validation Error",
  "fieldErrors": {
    "cropType": "no puede ser nulo",
    "sownDate": "debe ser en el pasado o presente"
  },
  "timestamp": "2025-01-15T10:46:00"
}
```

---

## 📝 Ejemplos de Uso

### **Ejemplo 1: Crear Cultivo**

```bash
curl -X POST http://localhost:8080/api/crops \
  -H "Authorization: Bearer eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyMyJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "cropType": "BANANO",
    "areaHectares": 5.5,
    "municipality": "ZONA_BANANERA",
    "sownDate": "2024-12-15"
  }'
```

**Respuesta**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "cropType": "BANANO",
  "areaHectares": 5.5,
  "municipality": "ZONA_BANANERA",
  "sownDate": "2024-12-15",
  "syncStatus": "SYNCED",
  "createdAt": "2025-01-15T10:30:00"
}
```

---

### **Ejemplo 2: Obtener Recomendación de Riego**

```bash
curl -X POST http://localhost:8080/api/crops/550e8400-e29b-41d4-a716-446655440000/recommendations/irrigation \
  -H "Authorization: Bearer eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyMyJ9..."
```

**Respuesta**:
```json
{
  "id": "770fa611-g40d-63f5-c928-668877662222",
  "cropId": "550e8400-e29b-41d4-a716-446655440000",
  "level": "HIGH",
  "message": "¡Alerta! Temperatura: 34.5°C (máx óptima: 30°C), Humedad: 55% (mín óptima: 65%). Se recomienda riego inmediato. Referencia de riego para BANANO: 2000 ml/day.",
  "temperature": 34.5,
  "humidity": 55.0,
  "generatedAt": "2025-01-15T14:05:00"
}
```

---

### **Ejemplo 3: Obtener Recomendación de Fertilización (IA)**

```bash
curl -X POST http://localhost:8080/api/crops/550e8400-e29b-41d4-a716-446655440000/recommendations/fertilizer \
  -H "Authorization: Bearer eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyMyJ9..."
```

**Respuesta**:
```json
{
  "id": "880gb722-h51e-74g6-d039-779988773333",
  "cropId": "550e8400-e29b-41d4-a716-446655440000",
  "cropStage": "Floración",
  "weeksSinceSowing": 8,
  "recommendedNutrient": "Potasio (K)",
  "recommendedDose": "150 kg/ha",
  "level": "HIGH",
  "message": "En fase de floración, el potasio es crítico para mejorar la calidad del fruto y la robustez de la planta. Se recomienda aplicar 150 kg/ha de K20.",
  "generatedAt": "2025-01-15T14:10:00"
}
```

---

### **Ejemplo 4: Sincronización Batch (Offline)**

```bash
curl -X POST http://localhost:8080/api/sync/batch \
  -H "Authorization: Bearer eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyMyJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "crops": [
      {
        "id": "990hc833-i62f-85h7-e140-880099884444",
        "cropType": "YUCA",
        "areaHectares": 3.0,
        "municipality": "CIENAGA",
        "sownDate": "2025-01-10"
      }
    ],
    "events": [
      {
        "id": "aa1id944-j73g-96i8-f251-991100995555",
        "cropId": "550e8400-e29b-41d4-a716-446655440000",
        "eventType": "OBSERVATION",
        "notes": "Plagas detectadas",
        "occurredAt": "2025-01-15T16:00:00"
      }
    ],
    "decisions": []
  }'
```

**Respuesta**:
```json
{
  "status": "OK",
  "message": "Sincronización completada: 1 cultivo, 1 evento, 0 decisiones procesadas",
  "syncedCrops": [
    {
      "id": "990hc833-i62f-85h7-e140-880099884444",
      "cropType": "YUCA",
      "areaHectares": 3.0,
      "municipality": "CIENAGA",
      "sownDate": "2025-01-10",
      "syncStatus": "SYNCED",
      "createdAt": "2025-01-15T16:05:00"
    }
  ],
  "syncedEvents": [
    {
      "id": "aa1id944-j73g-96i8-f251-991100995555",
      "cropId": "550e8400-e29b-41d4-a716-446655440000",
      "eventType": "OBSERVATION",
      "occurredAt": "2025-01-15T16:00:00"
    }
  ],
  "timestamp": "2025-01-15T16:05:00"
}
```

---

## 🤝 Contribución

### **Directrices**

1. **Fork el proyecto**
2. **Crea una rama** (`git checkout -b feature/AmazingFeature`)
3. **Commit tus cambios** (`git commit -m 'Add some AmazingFeature'`)
4. **Push a la rama** (`git push origin feature/AmazingFeature`)
5. **Abre un Pull Request**

### **Estándares de Código**

- Seguir convenciones de naming de Java
- Añadir logs descriptivos
- Incluir DTOs para nuevos endpoints
- Validar ownership en operaciones sensibles
- Documentar excepciones personalizadas

---

## 📄 Licencia

Este proyecto está bajo licencia **MIT**. Ver el archivo `LICENSE` para más detalles.

---

## 📞 Contacto y Soporte

Para dudas, sugerencias o reportar bugs:

- **Email**: soporte@agromag.com
- **Issues**: [GitHub Issues](https://github.com/tu-usuario/agromag/issues)
- **Documentación**: [Wiki del Proyecto](https://github.com/tu-usuario/agromag/wiki)

---

## 🙏 Agradecimientos

- **Spring Team** por Spring Boot y Spring Data JPA
- **Supabase** por autenticación y base de datos
- **Open-Meteo** por datos climáticos
- **OpenAI** por API de IA
- **Comunidad agrícola** del Magdalena por feedback

---

## 📊 Estadísticas del Proyecto

- **Lenguaje**: Java 21
- **Framework**: Spring Boot 4.0.6
- **Controladores**: 5
- **Servicios**: 8
- **Entidades**: 5
- **Endpoints**: 20+
- **Excepciones Personalizadas**: 6

---

**Agromag - Soluciones Agrícolas Inteligentes** 🌾  
*Empoderando a productores rurales con tecnología y datos*

Versión: 0.0.1-SNAPSHOT  
Última actualización: Enero 2025
