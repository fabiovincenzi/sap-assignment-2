# Quality Attribute Scenarios

## QAS-1: Performance — Real-time Tracking

| Source | Stimulus | Environment | Artifact | Response | Measure |
|--------|----------|-------------|----------|----------|---------|
| Customer | Richiede posizione drone | Operazioni normali, 50 consegne attive | Delivery Service | Posizione GPS e ETA aggiornati | < 1s latenza, update ogni 5s |

## QAS-2: Availability — Order Placement

| Source | Stimulus | Environment | Artifact | Response | Measure |
|--------|----------|-------------|----------|----------|---------|
| Customer | Piazza un ordine | Operazioni normali | Order Service | Ordine accettato e confermato | Disponibilita 99.9% |

## QAS-3: Modifiability — Nuovo tipo di drone

| Source | Stimulus | Environment | Artifact | Response | Measure |
|--------|----------|-------------|----------|----------|---------|
| Developer | Supportare nuovo modello drone | Design time | Drone Service | Integrato con modifiche locali | Cambiamenti in un solo servizio |

## QAS-4: Scalability — Aumento carico

| Source | Stimulus | Environment | Artifact | Response | Measure |
|--------|----------|-------------|----------|----------|---------|
| Business growth | Ordini giornalieri raddoppiano | Operazioni normali | Tutti i servizi | Nessun degrado performance | Scaling orizzontale indipendente per servizio |
