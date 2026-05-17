# Documentación Técnica de Cambios - Agromag Backend

Este documento detalla las implementaciones y mejoras realizadas en el sistema Agromag para asegurar la integración robusta de la Inteligencia Artificial, el manejo de datos climáticos y la persistencia de la API.

---

## 1. Controladores (Capa API)
Se implementó una suite completa de controladores REST para exponer las funcionalidades del negocio:

- **`CropController`**: 
    - Endpoints CRUD para la gestión de cultivos.
    - Operación de eliminación lógica y física.
    - Listado de cultivos filtrados por perfil de usuario.
- **`CropEventController`**:
    - Listado de eventos por cultivo.
    - Consulta de detalles de un evento específico por ID.
- **`RecommendationController`**:
    - **Fertilización (IA)**: Endpoint POST que dispara la consulta a OpenRouter.
    - **Riego (Lógica de Negocio)**: Endpoint POST que calcula necesidades basándose en umbrales de temperatura/humedad.
- **`ProfileController`**:
    - Gestión del perfil del agricultor vinculado a la autenticación de Supabase.
- **`SyncController`**:
    - Endpoints para verificar el estado de sincronización entre el cliente móvil y el servidor.

---

## 2. Servicios (Lógica de Negocio)
Se refactorizaron y crearon servicios clave para el procesamiento de datos:

- **`AiRecommendationService`**:
    - Integración con **Spring AI** para conectar con OpenRouter.
    - Implementación de **Prompt Engineering** especializado en agronomía del Magdalena, Colombia.
    - Lógica de limpieza de respuestas para manejar formatos Markdown inesperados.
- **`ClimateService`**:
    - Integración con la API de **Open-Meteo**.
    - Implementación de un cliente reactivo (`WebClient`) con manejo manual de JSON para evitar errores de deserialización.
- **`RecommendationService`**:
    - Orquestador que une datos climáticos, parámetros del cultivo y delegación a la IA.
    - Persistencia automática de cada recomendación generada para consulta histórica.

---

## 3. Integración de IA (OpenRouter)
Se configuró el soporte para modelos de lenguaje a través de un proxy compatible con OpenAI:

- **Configuración**: Uso de `spring-ai-openai` redirigido a `https://openrouter.ai/api/v1`.
- **Modelo**: `openai/gpt-4o-mini` por su balance entre costo, velocidad y precisión técnica.
- **Seguridad**: Gestión de API Keys mediante variables de entorno y soporte para archivos `.env` vía `spring-dotenv`.

---

## 4. Correcciones Técnicas (Fixes)
Se resolvieron cuellos de botella críticos identificados durante las pruebas:

- **PgBouncer / Supabase**: Se añadió el parámetro `prepareThreshold=0` en la cadena de conexión JDBC. Esto resuelve el error `prepared statement already exists` causado por el pooler de transacciones de Supabase.
- **Deserialización JSON**: Se corrigió el error `InvalidDefinitionException` al parsear la respuesta de Open-Meteo, cambiando la recepción a `String` y usando un `ObjectMapper` manual para mayor control.
- **Security JWT**: Se ajustó el `SecurityConfig` para validar los tokens JWT emitidos por Supabase, permitiendo el acceso protegido a los recursos del agricultor.
- **URL de IA**: Se corrigió el `base-url` de OpenRouter de `/api` a `/api/v1`, asegurando que el cliente de Spring AI pueda construir las rutas de chat completions correctamente.

---

## 5. Persistencia y Semillas (Seed Data)
- **`AgromagApplication`**: Se añadió un `CommandLineRunner` que inserta automáticamente los parámetros agronómicos base (`CropParameter`) para cultivos de Banano, Café, Mango y Cacao si la base de datos se detecta vacía al iniciar.
- **`data.sql`**: Se preparó el archivo de carga inicial para asegurar consistencia en entornos de desarrollo y producción.

---
**Agromag - Soluciones Agrícolas Inteligentes**
*Mayo 2026*
