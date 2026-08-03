# Event Sourcing — riferimenti e ricetta di implementazione

## Fonti

- **[Lab Notes] Microservice Patterns: Event Sourcing** — documento del corso su Google Docs:
  <https://docs.google.com/document/d/1WD1sYV8SbFlX3WtW3S4fBIz9VIqCsRXD57Mf6LusWFA/edit>
  Riassume il cap. 6 di Richardson, *Microservices Patterns* [MP]. **Non è tra i PDF distribuiti
  a lezione.** Per scaricarne il testo:
  `curl -sL "https://docs.google.com/document/d/1WD1sYV8SbFlX3WtW3S4fBIz9VIqCsRXD57Mf6LusWFA/export?format=txt"`
- Slide: `[module-2.1] Domain-Driven Design` p. 48; `[module-3.1] Microservices Patterns Overview` p. 10.
- Domande d'esame: D38 (cos'è) e D38b (procedimento) in `Domande_Esame_SAP.md`.

> Verificato il 2026-08-03 clonando tutti i 20 repo dell'org `sap-2025-2026`: **nessun lab
> implementa l'event sourcing**. Il `lab-activity-10` lo elenca come "[TODO] implementing event
> sourcing" e la sua persistenza resta `InMemoryGameRepository` (una `HashMap` a stato corrente).
> Da citare nel report: l'implementazione è lavoro originale, non adattamento di codice del lab.

## La ricetta (dalle Lab Notes)

1. **I comandi non mutano più.** Ogni metodo di comando si spezza in due:
   - `process(command)` → valida, **non tocca lo stato**, restituisce la lista di eventi; **può
     lanciare eccezione**;
   - `apply(event)` → aggiorna lo stato; **non può fallire**, perché l'evento è un fatto avvenuto.
2. **Ogni transizione di stato è un evento**, creazione compresa. Gli eventi devono contenere tutti
   i dati che servono ad `apply()` per ricostruire lo stato: non si possono più usare eventi magri
   con il solo ID.
3. **Caricare** = costruttore di default + replay di tutti gli eventi.
   **Creare** = istanzia → `process()` → `apply()` sui nuovi → salva i nuovi eventi.
   **Aggiornare** = carica gli eventi → istanzia → replay → `process()` → `apply()` → salva **solo i
   nuovi** eventi.
4. **Snapshot** per gli aggregate long-lived: si persiste lo stato alla versione N e il caricamento
   applica solo gli eventi successivi. Lo snapshot è stato derivato, mai fonte di verità.
5. **Event store** = *"hybrid of a database and a message broker"*: API per inserire e recuperare
   gli eventi di un aggregate **per chiave primaria**, più API per **sottoscriversi**.
6. **Evoluzione dello schema**: aggiornare gli eventi all'ultima versione al caricamento, tenendo il
   codice di upgrade fuori dall'aggregate.
7. **Cancellazione**: soft delete; per il GDPR cifratura con chiave per utente (si butta la chiave)
   ed eventuale pseudonimizzazione se il dato personale è usato come aggregate ID.
8. **Query**: l'event store serve solo lookup per chiave → serve **CQRS** per ogni altra query.

## Applicazione al Delivery Service

Stato di partenza: `Delivery` ha già `pendingEvents` e due eventi (`DeliveryCompleted`,
`DeliveryFailed`), ma i comandi (`assignDrone`, `startTransit`, `complete`, `fail`) mutano
direttamente e 3 transizioni su 5 non emettono nulla.

**Vincolo che guida il design:** `DeliveryRepository` espone `findByOrderId` e `findByDroneId`,
cioè query su campi non-chiave, che un event store non può servire. È la ragione **tecnica** per cui
CQRS diventa necessario — non una scelta di comodo (vedi 1.5).

### Fasi

1. **Eventi completi**, con payload sufficiente al replay:
   `DeliveryScheduled(deliveryId, orderId, route, weightKg, createdAt)` — evento "grasso" che ricrea
   l'aggregate — `DroneAssigned`, `TransitStarted`, `DronePositionUpdated`, più i due esistenti.
2. **Refactor dell'aggregate**: costruttore di default, metodi `processXxx()` che restituiscono
   eventi (dove migrano le guardie di stato attuali) e `apply(evento)` per ogni tipo.
3. **Event store**: `@OutBoundPort DeliveryEventStore` con
   `append(id, expectedVersion, events)` e `load(id)`; adapter `InMemoryDeliveryEventStore`
   append-only. `expectedVersion` dà il controllo di concorrenza ottimistico — è ciò che distingue un
   event store da una coda di messaggi.
4. **`EventSourcedDeliveryRepository`** che implementa l'interfaccia esistente: `findById` carica gli
   eventi e li riapplica. Grazie all'esagonale, `DeliveryService` resta pressoché invariato.
5. **Snapshot ogni N eventi.** Caso d'uso reale: `updateDronePosition` è ad alta frequenza, quindi lo
   stream cresce senza limiti. Misurare `findById` con ~1000 eventi prima e dopo, e riportare il
   numero (si lega al QAS di performance, sez. 5).
6. **Proiezione CQRS**: indici `orderId → deliveryId` e `droneId → deliveryId` aggiornati dagli
   eventi, per servire `findByOrderId` e `findByDroneId`.

**Bonus:** endpoint `GET /api/deliveries/:id/history` e `?at=<timestamp>` per lo stato a un istante
passato — dimostrazione concreta delle **temporal queries**, il vantaggio sottolineato da module-2.1
p. 48.

**Criterio di correttezza:** dopo la fase 4 i test Cucumber esistenti devono passare **senza
modifiche**. Se serve toccarli, il comportamento è cambiato.
