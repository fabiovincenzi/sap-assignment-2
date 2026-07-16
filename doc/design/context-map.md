# Context Map

```
+------------------+                    +--------------------+                    +------------------+
|                  |   OrderConfirmed   |                    |   AssignDrone      |                  |
|  Order Service   | ----------------→ |  Delivery Service  | ----------------→ |  Drone Service   |
|                  |                    |                    |                    |                  |
|  (Supporting)    | ←---------------- |  (Core)            | ←-----------------| (Supporting)     |
|                  | DeliveryCompleted  |                    | DroneLocationUpdated                  |
+------------------+                    +--------------------+  DroneStatusChanged+------------------+
```

## Pattern di integrazione

| Relazione | Pattern | Tipo | Comunicazione |
|-----------|---------|------|---------------|
| Order → Delivery | Customer-Supplier | Event (notify) | Order pubblica OrderConfirmed, Delivery reagisce |
| Delivery → Order | Customer-Supplier | Event (notify) | Delivery pubblica DeliveryCompleted, Order reagisce |
| Delivery → Drone | Partnership | Command (request/response) | Delivery richiede assegnazione drone via REST |
| Drone → Delivery | Partnership | Event (notify) | Drone pubblica DroneLocationUpdated, Delivery aggiorna tracking |

## Nota implementativa (Assignment 1)

In A1 non esiste un event broker (Kafka arriva in A3). Tutte le interazioni sono implementate come **chiamate REST sincrone** tramite **proxy adapter** (pattern Lab 6):
- Gli **eventi** sono simulati con chiamate REST: quando Order conferma un ordine, chiama l'endpoint di Delivery via proxy adapter
- I **comandi** tra servizi (es. AssignDrone) sono chiamate REST request/response dirette

## User Management

Gestito internamente all'Order Service come modulo semplificato (registrazione e login). Non serve un servizio separato per il prototipo.
