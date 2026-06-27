# Project-14
Flood Guard# 🛡️ FloodGuard — Crowdsourced Disaster Management Portal

A hyper-local, real-time web and mobile platform for flood and monsoon disaster response. Built with **Java 17 + Spring Boot 3** on the backend and **React 18** on the frontend. Supports geospatial tracking, volunteer coordination, SOS routing, and resource allocation mapping.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    React Frontend                       │
│  LiveMap (Leaflet) · Incidents · Volunteers · Resources │
│  WebSocket (STOMP/SockJS) for real-time updates         │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP/REST + WebSocket
┌───────────────────────▼─────────────────────────────────┐
│              Spring Boot Backend (Java 17)               │
│                                                         │
│  IncidentService  VolunteerService  ResourceService      │
│  JwtService       DashboardService  ScheduledChecks      │
│                                                         │
│  REST Controllers · WebSocket Handler (STOMP)           │
│  Spring Security (JWT) · Spring Data JPA                │
└───────┬───────────────────────────┬─────────────────────┘
        │                           │
┌───────▼───────┐         ┌─────────▼────────┐
│  PostgreSQL   │         │      Kafka        │
│  + PostGIS    │         │  sos-alerts       │
│  (spatial)    │         │  resource-updates │
└───────────────┘         │  volunteer-updates│
                          └──────────────────┘
```

---

## 🚀 Features

| Feature | Details |
|---|---|
| 🗺️ Live Geospatial Map | Leaflet-powered map with flood zones, SOS markers, volunteer positions, shelter icons |
| 🚨 SOS Alert System | Civilians submit SOS at `/report` (no login needed). Auto-broadcast via Kafka + WebSocket |
| 🤝 Volunteer Coordination | Register, assign zones, track live GPS, skill-based dispatch |
| 📦 Resource Allocation | Track food, medical, boats, power with threshold alerts |
| 🔁 Real-time Updates | STOMP WebSocket pushes incident, volunteer, and resource changes to all dashboards |
| 🔐 JWT Auth | Role-based: ADMIN / COORDINATOR / FIELD_OFFICER |
| 📊 Dashboard Summary | Live counts: SOS, volunteers deployed, resources critical, zones affected |
| 🏥 Auto-assignment | Nearest qualified volunteer auto-dispatched to SOS based on skill and location |

---

## 🗂️ Project Structure

```
floodguard/
├── src/
│   ├── main/
│   │   ├── java/com/floodguard/
│   │   │   ├── config/          # Security, WebSocket, Kafka, GlobalExceptionHandler
│   │   │   ├── controller/      # REST endpoints (Incident, Volunteer, Resource, Auth, Dashboard)
│   │   │   ├── dto/             # Request/Response DTOs
│   │   │   ├── entity/          # JPA entities (Incident, Volunteer, Resource, FloodZone, User)
│   │   │   ├── enums/           # Severity, IncidentStatus, VolunteerStatus, ResourceCategory
│   │   │   ├── repository/      # Spring Data JPA repositories (with spatial queries)
│   │   │   ├── service/         # Business logic services
│   │   │   └── websocket/       # STOMP message handler
│   │   └── resources/
│   │       ├── application.yml  # Configuration
│   │       └── data.sql         # Seed data
│   └── test/java/com/floodguard/
│       ├── IncidentServiceTest.java
│       └── ResourceServiceTest.java
├── frontend/
│   ├── src/
│   │   ├── hooks/               # useAuth, useWebSocket
│   │   ├── pages/               # Dashboard, LiveMap, Incidents, Volunteers, Resources, Login, SosReport
│   │   └── services/            # api.js (Axios)
│   ├── Dockerfile
│   └── nginx.conf
├── Dockerfile                   # Backend Docker build
├── docker-compose.yml           # Full stack: Postgres + Kafka + Backend + Frontend
└── pom.xml
```

---

## ⚡ Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (for frontend dev)

### 1. Clone the repo
```bash
git clone https://github.com/YOUR_USERNAME/floodguard.git
cd floodguard
```

### 2. Run the full stack with Docker Compose
```bash
docker-compose up --build
```

| Service | URL |
|---|---|
| Frontend (React) | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Civilian SOS Form | http://localhost:3000/report |

### 3. Default credentials
| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `coordinator1` | `coord123` | COORDINATOR |

---

## 🔧 Local Development

### Backend only
```bash
# Start infrastructure
docker-compose up postgres kafka -d

# Run Spring Boot
./mvnw spring-boot:run
```

### Frontend only
```bash
cd frontend
npm install
npm start     # http://localhost:3000
```

### Run tests
```bash
./mvnw test
```

---

## 📡 REST API Reference

### Authentication
```
POST /api/auth/login       { username, password }  → { token, roles }
POST /api/auth/register    { username, password, email, role }
```

### Incidents
```
GET    /api/incidents               List active incidents
GET    /api/incidents/sos           Active SOS alerts
GET    /api/incidents/zone/{zone}   By zone
GET    /api/incidents/nearby        ?lat=&lng=&radius=
POST   /api/incidents               Create (public — civilian SOS)
PATCH  /api/incidents/{id}/status   Update status
POST   /api/incidents/{id}/assign/{volunteerId}
```

### Volunteers
```
GET    /api/volunteers              All volunteers
GET    /api/volunteers/status/{s}   By status
GET    /api/volunteers/nearby-boats ?lat=&lng=&radius=
POST   /api/volunteers              Self-register
PATCH  /api/volunteers/{id}/location  { latitude, longitude }
PATCH  /api/volunteers/{id}/status    { status }
PATCH  /api/volunteers/{id}/zone      { zone }
```

### Resources
```
GET    /api/resources               All resources
GET    /api/resources/critical      Critically low stock
GET    /api/resources/category/{c}  By category
PATCH  /api/resources/{id}/quantity { availableQuantity, notes }
PATCH  /api/resources/{id}/zone     { zone }
```

### Dashboard
```
GET    /api/dashboard/summary       Aggregated live stats
GET    /api/dashboard/zones         All flood zones
GET    /api/dashboard/zones/evacuation  Evacuation-required zones
```

---

## 📡 WebSocket Topics (STOMP)

Connect to: `ws://localhost:8080/api/ws`

| Topic | Description |
|---|---|
| `/topic/incidents` | New and updated incidents |
| `/topic/volunteers/locations` | Live volunteer GPS pings |
| `/topic/resources` | Resource quantity updates |
| `/topic/alerts/resources` | Critical/low stock alerts |

**Send SOS from client:**
```javascript
client.publish({ destination: '/app/sos', body: JSON.stringify(incident) });
```

---

## 🔐 Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `floodguard` | PostgreSQL username |
| `DB_PASSWORD` | `floodguard123` | PostgreSQL password |
| `KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `JWT_SECRET` | *(see yml)* | **Change in production!** |

---

## 🧪 Tech Stack

| Layer | Technology |
|---|---|
| Backend language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt) |
| Database | PostgreSQL 15 + PostGIS |
| ORM | Spring Data JPA + Hibernate Spatial |
| Messaging | Apache Kafka |
| Real-time | WebSocket (STOMP + SockJS) |
| Frontend | React 18, React Router 6 |
| Maps | Leaflet + React-Leaflet |
| HTTP client | Axios |
| Containerisation | Docker + Docker Compose |

---

## 🌊 Use Case

Designed for Kerala's monsoon flood response (Palakkad district), this portal enables:
- Civilians to submit geotagged SOS reports from any device without an account
- District coordinators to view all incidents in real time on a live map
- Field officers to update volunteer positions every few minutes
- Shelter managers to log resource consumption and trigger automatic alerts
- Command centre to dispatch the nearest qualified volunteer to each SOS automatically

---

## 📄 License

MIT — free to use, modify, and deploy for humanitarian purposes.

[README.md](https://github.com/user-attachments/files/29410710/README.md)
