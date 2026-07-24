# Appunti per il report — Assignment 2 "Shipping on the Air with Patterns"

> Documento vivo: raccoglie decisioni di design, pattern applicati e razionale mentre implementiamo.
> Struttura pensata per diventare lo scheletro del report finale (`report/report.tex`).
> Legenda stato: ✅ fatto e verificato · 🟡 parziale · ⬜ da fare

---

## 0. Punto di partenza

- Assignment 2 = **raffinamento** dell'Assignment 1, non progetto nuovo (lo dice la consegna: *"we want to refine the design, implementation and deployment by applying a subset of the microservices patterns"*).
- Repo separato `github.com/fabiovincenzi/sap-assignment-2`, partito da import del codice finale di A1. La storia git di A2 mostra pulita l'aggiunta dei pattern, commit per commit.
- Base: 3 microservizi Vert.x (Order:8090, Delivery:8091, Drone:8092) + modulo `common`, architettura esagonale + DDD marker, repository in-memory, comunicazione via proxy REST sincroni.

---

## 1. Pattern richiesti dalla consegna

### 1.1 API Gateway ✅

> Verificato end-to-end: flusso completo dal gateway (register, drone, order, confirm, tracking). Il tracking aggregato restituisce in una risposta `orderStatus` (da order) + `deliveryStatus`/`droneId`/posizione/ETA (da delivery); i log del gateway mostrano le due GET verso order e delivery via DNS interno. Ordine inesistente → 404, upstream giù → 502.

**Cos'è (D35 / modulo 3.1):** singolo punto d'ingresso per i client verso i microservizi. Responsabilità: routing, composition/aggregation, auth, rate limiting, caching/logging, protocol translation.

**Cosa abbiamo implementato:**
- Nuovo sottoprogetto Gradle `api-gateway` (porta 8080, quarto container), architettura **esagonale** come i servizi.
- **Gateway sottile**: solo routing + aggregazione, nessuna logica di business. Coerentemente, i 3 port dell'application layer sono tutti `@OutBoundPort`, **nessun `@InBoundPort`** (il gateway non espone logica propria). Modello preso dal Lab 7 (`ttt-api-gateway`).
- **Dominio proprio ridotto**: record "vista" piatti (`OrderView`, `DeliveryView`, `DroneView`, `UserView`, `TrackingInfo`, `NewOrder`) = la vista che il gateway ha dei modelli dei servizi, non una loro copia.
- **Versionamento del contratto pubblico**: il gateway espone `/api/v1/...` mentre i servizi interni usano `/api/...`. Il contratto esterno è versionato e distinto da quello interno.

**Endpoint esposti (scelta: client-facing + aggregazione):**
| Gateway (8080) | inoltra a |
|---|---|
| `POST /api/v1/users/register` · `login` | order |
| `POST /api/v1/orders` · `GET /orders/:id` · `confirm` · `cancel` | order |
| `POST /api/v1/drones` | drone |
| `GET /api/v1/tracking/:orderId` | **order + delivery (aggregazione)** |

Gli endpoint di **coreografia interna** (scheduleDelivery, drone-position, assign, release, available, complete) **non** sono esposti: restano chiamate dirette servizio-a-servizio.

**Il pezzo forte — API Composition (D-query / modulo 3.1):** `GET /api/v1/tracking/:orderId` chiama **due** servizi (stato ordine da order + posizione drone/ETA da delivery) e **fonde** le risposte in un unico `TrackingInfo`. È la responsabilità "Composition/Aggregation" di D35 che il Lab 7 **non** mostra → valore aggiunto rispetto al materiale del corso.

**Dettaglio tecnico — non bloccare l'event loop:** i proxy usano `HttpClient` sincrono; Vert.x gira su event loop che non va mai bloccato. Quindi ogni handler avvolge la chiamata in `vertx.executeBlocking(...).onSuccess(...).onFailure(...)`. Sul gateway il problema è amplificato perché riceve *tutto* il traffico. **Miglioria rispetto al Lab 7:** passiamo `ordered=false` a `executeBlocking`, così le richieste non vengono serializzate una alla volta sul worker (col default `ordered=true` il gateway diventerebbe un collo di bottiglia).

**Traduzione uniforme degli errori:** i proxy mappano `2xx` → record, `404` → `Optional.empty` (non è un errore), il resto → `ServiceNotAvailableException`. Il controller traduce SNAE in **502 Bad Gateway** (status semanticamente corretto per un gateway che non raggiunge l'upstream; miglioria rispetto al Lab 7 che restava 200 con `result:error`); ordine/consegna inesistenti → 404.

**Limiti consapevoli (da citare nel report):**
- Come nel Lab 7, ogni risposta non-2xx delle scritture collassa su `ServiceNotAvailableException`: un `409 username già preso` diventa indistinguibile da "servizio giù".
- Nessuna auth (non richiesta dall'assignment). Evoluzione naturale: autenticazione al gateway con propagazione di un JWT ai servizi (D35 + D73, lab notes Production-Ready).
- Nessun timeout/circuit breaker sulle chiamate → aggancio naturale col Circuit Breaker (vedi 1.4).

### 1.2 Observability — Health Check API ✅

**Cos'è (D40 / lab notes Production-Ready, slide 28-32):** endpoint `/health` che dice se il servizio è *pronto a lavorare*, non solo *acceso*. Un servizio può girare ma essere inutilizzabile (dipendenza giù, ancora in avvio). L'infrastruttura interroga `/health` e smette di mandare traffico / riavvia se non sano.

**Cosa abbiamo implementato:**
- Endpoint `GET /health` su **tutti e 4** i componenti (order, delivery, drone, gateway), che risponde `{"status":"UP","checks":[]}` — formato **MicroProfile Health** come il Lab 8 (`status` + array `checks`).
- Blocco `healthcheck:` nel `docker-compose.yml` per ciascun servizio (`curl --fail http://localhost:PORT/health`) + `restart: unless-stopped`.
- Nei Dockerfile: installato `curl` nell'immagine di runtime (`eclipse-temurin:17-jre` non lo include, a differenza dell'immagine Maven del lab).
- **Verificato:** i 4 endpoint rispondono UP; `docker compose ps` marca tutti i container `(healthy)`.

**Nota concettuale (slide 31):** un health check "serio" verifica le dipendenze (es. query di prova sul DB). I nostri servizi hanno **repository in-memory, nessuna dipendenza esterna** → `UP` fisso è *onesto*, non pigro: con un DB reale l'handler eseguirebbe una query di test e ne metterebbe l'esito in `checks`.

**Differenze consapevoli rispetto al Lab 8 (da citare nel report):**
- Health check su **tutti e 4** i servizi, non solo sul gateway (il lab lo mette solo su `ttt-api-gateway` come esempio; la consegna chiede di instrumentare i microservizi).
- `test` in forma *exec* `["CMD","curl",...]` invece della forma *shell* del lab (non passa da una shell, più robusto).
- Host `localhost` invece del nome del servizio: è un **self-check**, non dipende dal DNS della rete.
- Tempi più corti (`interval 15s`, `start_period 20s` vs `1m30s`/`40s` del lab) per rendere lo stato `(healthy)` visibile in fretta durante la demo.
- **`restart: unless-stopped`** (restart policy nativa di Docker) invece del container `autoheal` del lab: il README del Lab 8 stesso ammette che autoheal *"is not working properly"*; la restart policy nativa è più semplice e affidabile. Autoheal citabile come alternativa.

### 1.3 Observability — Application Metrics 🟡

**Cos'è (D40 / lab notes, slide 43-47):** contatori e gauge esposti a un server di metriche. Modello **pull**: Prometheus interroga un endpoint `/metrics`; (Grafana visualizza — non containerizzato, vedi sotto).

**Cosa abbiamo implementato finora:**
- **Infrastruttura (compose)** ✅: `prometheus.yml` alla root (stile Lab 8, un job `static_configs` per servizio) + container `prometheus-01` (UI su `:9090`). Metriche su **porta dedicata** per servizio (order :9490, delivery :9491, gateway :9492, drone :9493), non su una rotta della porta applicativa.
- **Order Service** ✅ (validato end-to-end in locale): counter `orders_placed_total` che sale a ogni ordine creato. Client `io.prometheus:prometheus-metrics-{core,instrumentation-jvm,exporter-httpserver}:1.3.3`.
- **Delivery Service** ✅ (inc validato in locale: gauge 0→2 dopo due schedule): gauge `deliveries_in_progress`, `inc()` in `scheduleDelivery`, `dec()` in `completeDelivery`. Observer con **due metodi** (`notifyDeliveryScheduled`/`notifyDeliveryCompleted`) perché un gauge ha due transizioni, a differenza del counter monotòno di order. Nota: il `dec` non è stato esercitato end-to-end perché senza drone-service la consegna resta `SCHEDULED` e non può passare a `IN_TRANSIT`/`DELIVERED` (vincolo del ciclo di vita, non delle metriche).
- **Caveat gauge da citare:** una consegna che **fallisce** (`DeliveryFailed`) oggi non decrementa il gauge (gestiamo solo lo schedule→complete); con più tempo si aggancerebbe anche l'evento di fallimento per evitare che il gauge sovrastimi le consegne attive.
- Ancora da instrumentare: drone (`drones_available` gauge), gateway (`gateway_requests_total` counter).

**Counter vs Gauge (differenza di design osservata implementando):** il counter di order ha **un solo** metodo observer (`notifyOrderCreated` → `inc()`), è monotòno. Il gauge di delivery ne ha **due** (`inc` allo schedule, `dec` al complete) perché rappresenta una quantità istantanea che sale e scende. Stesso schema esagonale (port `@OutBoundPort` + adapter `@Adapter`), semantica della metrica diversa.

**Integrazione nell'esagono (il punto di design, diagrammi in `doc/diagrams/`):** le metriche sono una preoccupazione tecnica, quindi vivono su un **adapter al bordo** e il core non conosce Prometheus. Concretamente:
- nuovo **port** `OrderServiceObserver` (`@OutBoundPort`) nell'application layer: il core *notifica* verso l'esterno (driven/secondary port, come il repository);
- `OrderService` tiene una lista di observer + `addObserver(...)` e chiama `notifyOrderCreated(...)` nel caso d'uso di creazione;
- **adapter** `PrometheusOrderServiceObserver` (`@Adapter`, infrastructure) implementa il port: possiede il `Counter` e un `HTTPServer` Prometheus sulla porta dedicata. `import io.prometheus...` compare **solo** qui.
- Sostituire Prometheus (es. con OpenTelemetry) = nuovo adapter, **zero righe nel core**. Stessa dependency inversion di repository e proxy.
- Rispetto al **Lab 8**: là il `service.addObserver(obs)` c'era già; qui il meccanismo observer è stato **aggiunto** (le classi evento di dominio esistevano, ma non un port di pubblicazione). Questo stesso port è la base per l'**Event Sourcing** (1.4) → lavoro non sprecato.

**Gotcha da citare nel report (naming Prometheus):** la metrica **non** può chiamarsi `orders_created_total`: il client rimuove il suffisso convenzionale `_total` dei counter, resta `orders_created`, e `_created` è un **suffisso riservato** (Prometheus genera in automatico la serie `<name>_created` col timestamp di creazione del counter) → `IllegalArgumentException`. Rinominata in `orders_placed_total`. Regola generale: per un counter dare il nome-base (con o senza `_total`), evitando i suffissi riservati `_created`, `_total`, `_sum`, `_count`, `_bucket`, `_gsum`, `_gcount`.

### 1.4 Event Sourcing ⬜ (applicato a UN servizio)

**Cos'è (D38 / modulo 3.1):** modellare la persistenza come sequenza di eventi invece che come stato corrente; lo stato si ricostruisce riapplicando gli eventi.

**Candidato: Delivery Service** — ha già gli eventi di dominio (`DeliveryCompleted`, `DeliveryFailed`) e un ciclo di vita a stati chiaro (SCHEDULED → DRONE_ASSIGNED → IN_TRANSIT → DELIVERED/FAILED).

### 1.5 Due pattern a scelta ⬜

Proposta:
- **Circuit Breaker** (D / *Design for failure*): sui proxy sincroni, con `vertx-circuit-breaker`. Evita fallimenti a cascata (fail fast + fallback). Si aggancia bene al gateway e ai QAS di availability.
- **CQRS** (D37) oppure **Log Aggregation** (D40). CQRS è naturale complemento dell'Event Sourcing sullo stesso servizio.

---

## 2. Deployment strategy basata su container ✅

- **Un `Dockerfile` multi-stage per servizio**: stage `build` con `gradle`/JDK che fa `installDist`, stage runtime con solo JRE + distribuzione installata (immagine finale senza JDK né Gradle). Migliora l'immagine single-stage del Lab 7 (che tiene Maven dentro l'immagine finale) → citabile nel report.
- **Build context = root del monorepo** (serve il modulo `common`), quindi `-f order-service/Dockerfile .`. Differenza rispetto al Lab 7, che ha una repo per servizio.
- **`docker-compose.yml`** alla root: rete logica `shipping_network`, `container_name` espliciti (order-01, delivery-01, drone-01), `ports` + `expose`.
- **Externalized Configuration** (vedi 3.1) via blocchi `environment:` che iniettano i nomi dei container come hostname.
- **Verificato end-to-end**: catena order-01 → delivery-01 → drone-01 via DNS interno Docker; log mostrano `http://drone-service:8092/...` (nomi di servizio, non localhost).

---

## 3. Pattern di supporto (non obbligatori ma applicati)

### 3.1 Externalized Configuration ✅

**Cos'è (lab notes Production-Ready, slide 18-23):** la config non può stare nel codice; va fornita da fuori a runtime, perché dipende dall'ambiente. Due modelli: **push** (l'infrastruttura spinge la config, es. env var) e **pull** (il servizio la legge da un config server).

**Cosa abbiamo fatto:** i 3 launcher leggono host/porta dei servizi da variabili d'ambiente (`DELIVERY_HOST`, `DELIVERY_PORT`, ...) con fallback a `localhost`. È il **push model via environment variables** (slide 21). Vantaggio: lo stesso artefatto gira in locale (fallback localhost, test Cucumber invariati) e in Docker (il compose inietta i nomi dei container). Nessun blocco di codice da commentare/scommentare come nel Lab 7.

### 3.2 Logging — parte developer di Log Aggregation ✅

**Cos'è (D40 / lab notes, slide 33-36):** ogni servizio logga; una pipeline aggrega su un server centrale. Regole per il developer (slide 35): usare una libreria di logging e **scrivere su stdout** (nei container non c'è filesystem permanente).

**Cosa abbiamo fatto:** logging `java.util.logging` (JUL) in stile lab (logger per componente `[Nome]`, `logger.log(Level.INFO, ...)`) su infrastructure + application dei 3 servizi. Output su stdout → `docker compose logs` lo raccoglie già. È la **parte developer** del pattern Log Aggregation; l'aggregatore centrale (ELK) non è implementato ma i log sono strutturati e pronti.

**Nota tecnica onesta:** il nome del logger `[Nome]` non compare nell'output col `SimpleFormatter` di default di JUL (stampa `classe metodo`); l'informazione c'è comunque. Scelta: i `printStackTrace()` nei catch restano come sono (aderenza al materiale del lab).

### 3.3 Fix funzionali ereditati da A1 ✅

Due bug pre-esistenti sistemati (in A1 e portati in A2 con cherry-pick):
- **Endpoint mancante**: `POST /api/deliveries/drone-position` non era registrato nel `DeliveryController` → il proxy del drone prendeva 404 e il tracking in tempo reale non funzionava. Aggiunto handler + doc OpenAPI. Rilevante perché il tracking è una user story della consegna.
- **Fallimento silenzioso all'avvio**: i launcher non avevano `.onFailure()` sul `listen()` → fallivano in silenzio se la porta era occupata. Aggiunto `.onFailure()` con log `SEVERE` + `System.exit(1)` (exit code non-zero, importante per le restart policy di Docker).

---

## 4. Testing strategy ⬜

Consegna: un esempio per ogni livello della **test pyramid**. Riferimento D41 (5 livelli) + Lab 9.
- **Unit** (dominio + application, in isolamento)
- **Integration** (servizio + dipendenze esterne)
- **Component** (un microservizio intero, mockando gli altri)
- **Contract** (consumer-driven, es. Pact) — il gateway è un consumer naturale dei 3 servizi
- **End-to-End** (sistema intero, con parsimonia)

Base già presente da A1: ArchUnit (fitness functions) + Cucumber BDD.

---

## 5. Quality Attribute Scenarios + Observability ⬜

Consegna: discutere come usare gli observability pattern per implementare QAS, con **due esempi concreti**. Riferimento D8/D9. Base: `doc/analysis/quality-attributes.md` da A1.

Idee (da raffinare):
- **Availability**: *"se il delivery-service è irraggiungibile, l'order-service risponde entro 1s con un errore gestito invece di bloccarsi"* → misurabile con Application Metrics (error rate) + dimostrabile spegnendo un container. Si lega al Circuit Breaker.
- **Performance/latency**: *"il 95% delle richieste di tracking risponde entro N ms"* → misurabile con Application Metrics (latency histogram sul gateway).

---

## 6. Collegamenti a slide e domande d'esame

| Argomento | Domanda esame | Materiale corso |
|---|---|---|
| API Gateway | D35 | modulo 3.1; Lab 7 (`ttt-api-gateway`) |
| API Composition / aggregazione | D (query patterns) | modulo 3.1 slide 11 |
| Observability (Health, Metrics) | D40 | lab notes Production-Ready; Lab 8 |
| Event Sourcing | D38 | modulo 3.1; modulo 4.x |
| CQRS | D37 | modulo 3.1 |
| Circuit Breaker | D (riga 422, 1138) | modulo 3.1 (Reliability) |
| Service Discovery | D36 | modulo 3.1 |
| Externalized Configuration | — | lab notes Production-Ready slide 18-23 |
| Testing pyramid | D41, D89 | Lab 9 |
| Deployment container | D72 | Lab 7 |
| QAS | D8, D9 | modulo 1.2/1.3 |
| Microservice Chassis / Service Mesh | — | lab notes Production-Ready slide 52-54 |

Guida di studio dettagliata: `../Guida_Service-Discovery_Circuit-Breaker_Production-Ready.md`
