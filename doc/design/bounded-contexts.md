# Bounded Contexts

Ogni bounded context corrisponde a un microservizio con proprio domain model, database e deploy indipendente.

## Order Context → Order Service

| Concetto | Tipo |
|----------|------|
| Order | Aggregate Root |
| PackageInfo | Value Object |
| Address | Value Object |
| OrderStatus | Value Object (PENDING, CONFIRMED, CANCELLED, COMPLETED) |

- **Commands**: createOrder, confirmOrder, cancelOrder
- **Queries**: getOrder
- **Eventi pubblicati**: OrderConfirmed, OrderCancelled
- **Eventi consumati**: DeliveryCompleted → ordine COMPLETED

### Modulo User Management (Generic subdomain)

Gestito internamente come modulo semplificato. Non giustifica un servizio separato per il prototipo.

| Concetto | Tipo |
|----------|------|
| User | Entity |
| UserId | Value Object |

- **Commands**: register, login
- **Queries**: getUser

## Delivery Context → Delivery Service

| Concetto | Tipo |
|----------|------|
| Delivery | Aggregate Root |
| Route | Value Object |
| DeliveryStatus | Value Object (SCHEDULED, DRONE_ASSIGNED, IN_TRANSIT, DELIVERED, FAILED) |

- **Commands**: scheduleDelivery, assignDrone, startDelivery, completeDelivery
- **Queries**: trackDelivery
- **Eventi pubblicati**: DeliveryCompleted, DeliveryFailed
- **Eventi consumati**: OrderConfirmed → crea delivery, DroneLocationUpdated → aggiorna tracking

## Drone Context → Drone Service

| Concetto | Tipo |
|----------|------|
| Drone | Aggregate Root |
| Location | Value Object (lat, lng) |
| DroneStatus | Value Object (AVAILABLE, IN_FLIGHT, CHARGING, MAINTENANCE) |

- **Commands**: registerDrone, updateLocation, releaseDrone
- **Queries**: findAvailableDrone, getDrone
- **Eventi pubblicati**: DroneLocationUpdated, DroneStatusChanged
- **Eventi consumati**: DroneAssigned → drone IN_FLIGHT

## God Classes evitate

Lo stesso concetto ha rappresentazioni diverse nei diversi BC:
- **Order Context**: conosce customer, package, prezzo, stato ordine — non sa nulla del drone o del percorso
- **Delivery Context**: conosce pickup, delivery, drone assegnato (solo ID), route, ETA — non sa nulla del customer o del prezzo
- **Drone Context**: conosce posizione, stato, capacita — non sa nulla di ordini o delivery
