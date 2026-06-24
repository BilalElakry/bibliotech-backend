# 📚 BiblioTech — Plateforme de Bibliothèque Numérique PDF

> Plateforme web complète de gestion de bibliothèque numérique permettant la centralisation,
> la gestion et la diffusion sécurisée de ressources documentaires au format PDF,
> enrichie d'un assistant conversationnel intelligent basé sur Spring AI avec pipeline RAG
> (Retrieval-Augmented Generation), d'un module Text-to-Speech (TTS) et d'un système
> de sous-titres synchronisés (CC Sync / WebVTT).

---

## 👥 Équipe

| Membre | Rôle |
| --- | --- |
| **Bilal Elakry** | Back-end, Sécurité JWT, StorageService, Chatbot Spring AI, Pipeline RAG, TTS, CC Sync, Audit, Stats, OpenLibrary |
| **Haitam Khouna** | Diagrammes UML, Module Documents, Interface React.js, Architecture Design System & Thèmes |

**Encadrante :** Mme Aarich Mounia

**Établissement :** EMSI Rabat — 4ème année cycle ingénieur (4DDSIR)

**Année académique :** 2025–2026

---

## 🎯 Fonctionnalités

### 🎨 Interface & Design System "Earth Library"

* **Fidélité Visuelle & Textures Organiques** : Charte graphique haut de gamme inspirée des bibliothèques universitaires classiques (nuances de lin, travertin, terre cuite, mousse de forêt et bois de noyer).
* **Moteur Double Thème Avancé** :
* **Night Mode (Par défaut)** : Ambiance feutrée de salon de lecture au crépuscule, optimisée pour la lecture nocturne prolongée.
* **True Daylight Mode ("Le Reverse-Espresso")** : Zéro blanc pur agressif. Fond mat "Sable de Travertin" avec une luminosité globale capée à 82% pour préserver l'intégrité rétinienne.


* **Isolation CSS & Verrouillage de Spécificité** : Utilisation d'architectures CSS strictes pour immuniser les composants critiques (comme la `.hero-card` et ses overlays d'images sombres) contre les effets de cascade lors du basculement de thème global. Préservation permanente des contrastes textuels en teinte parchemin.

### 👤 Gestion des utilisateurs

* Inscription publique (`/api/auth/register`) avec email de bienvenue automatique
* Authentification sécurisée par JWT (expiration 24h)
* Deux rôles : `ADMIN` et `UTILISATEUR`
* Gestion du profil et changement de mot de passe (avec notification email)
* Activation / désactivation des comptes par l'admin (avec notification email)

### 📄 Catalogue documentaire

* Ajout, modification, suppression de documents PDF
* Recherche simple (titre, auteur, ISBN) et recherche avancée (filtres par catégorie, année min/max)
* Pagination et tri configurables
* Gestion des catégories thématiques
* Upload sécurisé de fichiers PDF (max 50 Mo)
* Intégration **OpenLibrary API** pour pré-remplir les métadonnées par ISBN ou mot-clé

### 📥 Accès et téléchargement

* Téléchargement sécurisé de PDF (endpoint REST protégé par JWT)
* Lecture en ligne via visionneuse intégrée
* Traçabilité complète des accès (téléchargements + lectures en ligne)
* Compteur de téléchargements par document
* Email de confirmation automatique à chaque téléchargement

### 🤖 Chatbot Spring AI + Pipeline RAG

* Assistant conversationnel en langage naturel (français)
* **Pipeline RAG (Retrieval-Augmented Generation)** : le chatbot répond en se basant sur le contenu réel des PDFs, pas uniquement sur les métadonnées du catalogue
* Extraction de texte PDF via **Apache PDFBox**, découpage en chunks via **TokenTextSplitter**
* Embeddings sémantiques générés localement par **nomic-embed-text** (Ollama, 768 dimensions)
* Stockage vectoriel dans **Qdrant Cloud** (gratuit, cosine similarity)
* Recherche sémantique des chunks les plus pertinents (top-k configurable)
* Fallback automatique sur le catalogue si aucun chunk ne dépasse le seuil de similarité
* Mémoire conversationnelle par session (fenêtre glissante de 20 messages)
* **Persistance des conversations** : historique complet sauvegardé en base de données (MySQL)
* Ingestion RAG **asynchrone** (`@Async`) — non-bloquante pour les uploads PDF
* Ré-ingestion automatique à chaque mise à jour de PDF ; suppression des vecteurs à la suppression du document
* Propulsé par **Ollama + qwen2.5:3b** (100% local, gratuit, sans clé API, optimisé pour le français)

### 🔊 Text-to-Speech (TTS)

* Extraction de texte depuis les PDFs via **Apache PDFBox**
* Génération audio WAV via **espeak-ng** (voix française ou anglaise)
* Endpoint de récupération du texte brut pour la Web Speech API du navigateur
* Limité aux 10 premières pages et 3 000 caractères pour les performances

### 🎬 CC Sync — Sous-titres synchronisés

* Découpage du texte PDF en segments de 12 mots avec horodatage précis
* Timestamps calculés en fonction de la vitesse espeak-ng (150 WPM) pour une synchronisation exacte
* Export **WebVTT** standard, compatible HTML5 `<track>` et tous les lecteurs media
* Export **JSON** des segments pour intégration React personnalisée
* Filtre de bruit intelligent : suppression des en-têtes, URLs, numéros de page, lignes en majuscules
* Paramètres `maxPages` et `skipPages` pour ignorer les pages de couverture
* Numéro de page inclus dans chaque segment pour navigation contextuelle

### 📊 Statistiques & Audit

* Tableau de bord administrateur : total documents, utilisateurs, téléchargements, lectures
* Top 5 des documents les plus téléchargés
* 10 accès les plus récents
* Export CSV des historiques d'accès
* Export PDF du rapport statistique (OpenPDF)
* Journal d'audit complet (actions LOGIN, UPLOAD, DELETE, DOWNLOAD, etc.) avec niveaux INFO / WARNING / ERROR
* Journalisation asynchrone (`@Async`) pour ne pas bloquer les requêtes

### 🔒 Rate Limiting

* Limitation à **60 requêtes par minute** par adresse IP (Bucket4j)
* Réponse HTTP 429 avec message explicite en cas de dépassement

---

## 🛠️ Stack technologique

| Couche | Technologie |
| --- | --- |
| **Front-end** | React.js 18, Custom CSS Custom Properties (Design System adaptatif) |
| **Back-end** | Java 17, Spring Boot 3.2.5 |
| **Sécurité** | Spring Security, JWT (jjwt 0.12.5), BCrypt |
| **Rate Limiting** | Bucket4j 8.10.1 |
| **Persistance** | Spring Data JPA, Hibernate, MySQL 8 / MariaDB |
| **IA conversationnelle** | Spring AI 1.0.0-M6, Ollama, qwen2.5:3b |
| **RAG — Embeddings** | nomic-embed-text (Ollama, local, 768 dims) |
| **RAG — Vector Store** | Qdrant Cloud (free cluster, cosine similarity) |
| **RAG — Chunking** | TokenTextSplitter (Spring AI, GPT-2 tokenizer) |
| **Extraction PDF** | Apache PDFBox 3.0.2 |
| **Text-to-Speech** | espeak-ng (local), Web Speech API |
| **CC Sync** | ChunkingService (PDFBox) + VttSerializer (WebVTT) |
| **Métadonnées livres** | OpenLibrary REST API |
| **Notifications email** | Spring Boot Mail (SMTP Gmail) |
| **Documentation API** | SpringDoc OpenAPI / Swagger UI 2.3.0 |
| **Stockage PDF** | Système de fichiers local (StorageService) |
| **Rapport PDF** | OpenPDF 1.3.43 |
| **Tests** | Postman, Spring Boot Test |
| **Outils** | IntelliJ IDEA, Maven, Git, XAMPP |

---

## 📁 Structure du projet

```
bibliotech/
├── src/
│   ├── main/
│   │   ├── java/com/bibliotech/
│   │   │   ├── auth/               # Authentification JWT (login, register, logout)
│   │   │   ├── catalogue/
│   │   │   │   ├── document/       # Gestion des documents PDF (CRUD + upload + recherche)
│   │   │   │   └── categorie/      # Gestion des catégories thématiques
│   │   │   ├── acces/              # Traçabilité des téléchargements et lectures en ligne
│   │   │   ├── utilisateur/        # Gestion des comptes utilisateurs
│   │   │   ├── chatbot/            # Assistant Spring AI (Ollama/qwen2.5:3b) + persistance DB
│   │   │   ├── rag/                # Pipeline RAG (ingestion + retrieval + admin)
│   │   │   │   ├── RagProperties.java          # Configuration externalisée
│   │   │   │   ├── RagIngestionService.java    # PDF → chunks → embeddings → Qdrant
│   │   │   │   ├── RagRetrievalService.java    # Query → Qdrant → context string
│   │   │   │   ├── RagAdminController.java     # POST /api/rag/ingest/{id|all}
│   │   │   │   └── QdrantIndexInitializer.java # Création auto de l'index payload au démarrage
│   │   │   ├── stats/              # Statistiques admin + export CSV + export PDF
│   │   │   ├── audit/              # Journal d'audit asynchrone
│   │   │   ├── notification/       # Service email (SMTP) + EmailProperties
│   │   │   ├── openlibrary/        # Intégration OpenLibrary API (ISBN lookup + search)
│   │   │   ├── tts/                # TTS + CC Sync
│   │   │   ├── storage/            # Stockage fichiers PDF (StorageService + StorageProperties)
│   │   │   ├── security/           # JwtAuthenticationFilter, JwtUtils, RateLimitFilter, UserDetailsServiceImpl
│   │   │   ├── config/             # SecurityConfig, RestTemplateConfig, ChatClientConfig
│   │   │   └── exception/          # GlobalExceptionHandler, BusinessException, ResourceNotFoundException
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/bibliotech/
│           └── BibliotechApplicationTests.java
├── uploads/                        # Fichiers PDF stockés (gitignore)
├── pom.xml
└── README.md

```

---

## ⚙️ Prérequis

* **Java 17** — [Download](https://www.oracle.com/java/technologies/downloads/#java17)
* **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
* **MySQL / MariaDB** — via XAMPP ou installation standalone
* **Ollama** — [Download](https://ollama.com)
* **Qdrant Cloud** — [Free cluster](https://cloud.qdrant.io) (gratuit, sans carte bancaire)
* **espeak-ng** *(optionnel, pour TTS audio + CC sync)* — [Download](https://github.com/espeak-ng/espeak-ng/releases)
* **Git** — [Download](https://git-scm.com)

---

## 🚀 Installation et démarrage

### 1. Cloner le projet

```bash
git clone https://github.com/thelazygenius404/bibliotech-backend.git
cd bibliotech-backend

```

### 2. Démarrer MySQL

Via XAMPP : démarrer le module **MySQL**.

La base de données `bibliotech_db` sera créée automatiquement au premier lancement.

### 3. Démarrer Ollama et charger les modèles

```bash
# Modèle LLM (génération de réponses)
ollama pull qwen2.5:3b

# Modèle d'embedding RAG (274 MB, gratuit, local)
ollama pull nomic-embed-text

# Ollama démarre automatiquement sur http://localhost:11434

```

### 4. Créer le cluster Qdrant Cloud (gratuit)

1. Aller sur [https://cloud.qdrant.io](https://cloud.qdrant.io) → créer un compte
2. **Create Cluster** → Free tier → choisir une région
3. Copier le **Cluster URL** (ex: `abc123.eu-central.aws.cloud.qdrant.io`)
4. Créer une **API Key** dans Settings → API Keys

### 5. Configurer `application.yml`

Créez le fichier `src/main/resources/application.yml` :

```yaml
server:
  port: 8080

spring:
  application:
    name: bibliotech

  datasource:
    url: jdbc:mysql://localhost:3306/bibliotech_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password:           # ← votre mot de passe MySQL

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 55MB

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:3b
          temperature: 0.3
          num-predict: 256
      embedding:
        options:
          model: nomic-embed-text   # modèle d'embedding RAG

    vectorstore:
      qdrant:
        host: your-cluster.eu-central.aws.cloud.qdrant.io  # ← sans https://
        port: 6334
        api-key: your-qdrant-api-key
        collection-name: bibliotech-rag
        use-tls: true
        initialize-schema: true

  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

bibliotech:
  jwt:
    secret: bibliotech-jwt-secret-key-2026-bilal-haitam-emsi-rabat
    expiration: 86400000

  mail:
    enabled: false
    from: your-email@gmail.com
    app-name: BiblioTech

  storage:
    upload-dir: ./uploads

  tts:
    max-chars: 3000
    max-pages: 10
    engine: espeak

  rag:
    enabled: true
    chunk-size: 500       # tokens par chunk
    chunk-overlap: 50     # chevauchement entre chunks
    top-k: 3              # chunks récupérés par requête
    min-score: 0.60       # seuil minimum de similarité cosinus

```

### 6. Lancer l'application

```bash
mvn spring-boot:run

```

L'application démarre sur **http://localhost:8080** Swagger UI : **http://localhost:8080/swagger-ui/index.html**

Au démarrage, l'index payload Qdrant sur le champ `documentId` est créé automatiquement.

### 7. Ingérer les documents existants dans Qdrant

Si vous avez déjà des documents en base, lancez cette requête une seule fois :

```
POST /api/rag/ingest/all
Authorization: Bearer <admin_token>

```

Les nouveaux documents sont ingérés automatiquement à chaque upload.

### 8. Créer le premier administrateur (optionnel)

```sql
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, role, actif, date_inscription)
VALUES ('Admin', 'BiblioTech', 'admin@bibliotech.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
        'ADMIN', true, CURDATE());

```

> Mot de passe : **`password`**

---

## 📡 API Endpoints

### 🔐 Authentification

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Public | Connexion — retourne le JWT |
| `POST` | `/api/auth/register` | Public | Inscription (rôle UTILISATEUR) |
| `POST` | `/api/auth/logout` | Authentifié | Déconnexion (stateless, côté client) |

### 📄 Documents

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/documents` | Authentifié | Liste paginée / recherche |
| `GET` | `/api/documents/advanced` | Authentifié | Recherche avancée |
| `GET` | `/api/documents/{id}` | Authentifié | Détails d'un document |
| `POST` | `/api/documents` | Admin | Ajouter + upload PDF (déclenche ingestion RAG) |
| `PUT` | `/api/documents/{id}` | Admin | Modifier (ré-ingestion RAG si nouveau PDF) |
| `DELETE` | `/api/documents/{id}` | Admin | Supprimer (supprime les vecteurs Qdrant) |
| `GET` | `/api/documents/{id}/pdf` | Authentifié | Télécharger le PDF |
| `GET` | `/api/documents/{id}/pdf/inline` | Authentifié | Lire le PDF en ligne |

### 📁 Catégories

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/categories` | Admin | Liste des catégories |
| `POST` | `/api/categories` | Admin | Créer une catégorie |
| `PUT` | `/api/categories/{id}` | Admin | Modifier une catégorie |
| `DELETE` | `/api/categories/{id}` | Admin | Supprimer (refus si documents liés) |

### 👥 Utilisateurs

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/utilisateurs` | Admin | Liste de tous les utilisateurs |
| `POST` | `/api/utilisateurs` | Admin | Créer un compte |
| `PATCH` | `/api/utilisateurs/{id}/toggle` | Admin | Activer / Désactiver |
| `DELETE` | `/api/utilisateurs/{id}` | Admin | Supprimer |
| `GET` | `/api/utilisateurs/me` | Authentifié | Mon profil |
| `PUT` | `/api/utilisateurs/me/password` | Authentifié | Changer mon mot de passe |

### 📥 Accès & Historique

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/acces/documents/{id}/telecharger` | Authentifié | Télécharger + tracer l'accès |
| `GET` | `/api/acces/documents/{id}/lire` | Authentifié | Lire en ligne + tracer l'accès |
| `GET` | `/api/acces/mon-historique` | Authentifié | Mon historique d'accès |
| `GET` | `/api/acces/utilisateurs/{userId}` | Admin | Historique d'un utilisateur |

### 📊 Statistiques

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/stats` | Admin | Tableau de bord complet |
| `GET` | `/api/stats/export/csv` | Admin | Export CSV des accès |
| `GET` | `/api/stats/export/pdf` | Admin | Export PDF du rapport statistique |

### 🗂️ Audit

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/audit` | Admin | Journal paginé de toutes les actions |
| `GET` | `/api/audit/recent` | Admin | 20 actions les plus récentes |
| `GET` | `/api/audit/errors` | Admin | Actions en erreur uniquement |
| `GET` | `/api/audit/utilisateurs/{email}` | Admin | Journal par utilisateur |

### 🤖 Chatbot RAG

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `POST` | `/api/chatbot/message` | Authentifié | Envoyer un message (RAG + fallback catalogue) |
| `GET` | `/api/chatbot/history/{sessionId}` | Authentifié | Récupérer l'historique d'une session |
| `DELETE` | `/api/chatbot/session/{sessionId}` | Authentifié | Effacer l'historique de session |

### 🧠 RAG — Administration

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `POST` | `/api/rag/ingest/{id}` | Admin | Ingérer un document spécifique dans Qdrant |
| `POST` | `/api/rag/ingest/all` | Admin | Ingérer tous les documents (backfill initial) |

### 🔊 Text-to-Speech & CC Sync

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/tts/documents/{id}/audio` | Authentifié | Génère un fichier WAV |
| `GET` | `/api/tts/documents/{id}/text` | Authentifié | Texte extrait pour Web Speech API |
| `GET` | `/api/tts/documents/{id}/captions` | Authentifié | Segments JSON horodatés |
| `GET` | `/api/tts/documents/{id}/vtt` | Authentifié | Fichier WebVTT téléchargeable |

### 📖 OpenLibrary

| Méthode | Endpoint | Accès | Description |
| --- | --- | --- | --- |
| `GET` | `/api/openlibrary/isbn/{isbn}` | Admin | Métadonnées par ISBN |
| `GET` | `/api/openlibrary/search?q=...` | Admin | Recherche par mot-clé |

---

## 🧠 Architecture RAG

```
┌─────────────────────────────────────────────────────────────┐
│  INGESTION (déclenchée @Async à chaque upload PDF)          │
│                                                             │
│  POST /api/documents  ──►  DocumentService.create()         │
│                                  │                          │
│                                  ▼                          │
│                    RagIngestionService.ingest()             │
│                          │                                  │
│                 PDFBox text extraction (full doc)           │
│                          │                                  │
│                 TokenTextSplitter (500 tokens, overlap 50)  │
│                          │                                  │
│                 nomic-embed-text (Ollama, local, 768 dims)  │
│                          │                                  │
│                 Qdrant Cloud (FREE) ← vecteurs stockés      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  RETRIEVAL (à chaque message chatbot)                       │
│                                                             │
│  POST /api/chatbot/message                                  │
│         │                                                   │
│         ▼                                                   │
│  RagRetrievalService.retrieveContext(userQuery)             │
│         │                                                   │
│  nomic-embed-text embed query → Qdrant similarity search    │
│         │                                                   │
│  top-3 chunks (score ≥ 0.60) → formatted context string    │
│         │                                                   │
│  [RAG context] + [conversation history] → qwen2.5:3b       │
│         │                                                   │
│  Grounded French response → saved to MySQL                  │
└─────────────────────────────────────────────────────────────┘

```

**Fallback automatique :** si aucun chunk ne dépasse le seuil de similarité,
le chatbot bascule sur la liste des documents du catalogue (comportement original).

---

## 🔒 Sécurité

* Mots de passe hachés avec **BCrypt**
* Authentification sans état via **JWT** (expiration configurable, défaut 24h)
* Contrôle d'accès par rôle **RBAC** (`ADMIN` / `UTILISATEUR`) via `@PreAuthorize`
* Filtrage JWT sur chaque requête (`OncePerRequestFilter`)
* Rate limiting : 60 requêtes/minute/IP (Bucket4j) — HTTP 429 en cas de dépassement
* Fichiers PDF inaccessibles sans token JWT valide
* CORS configuré pour `http://localhost:5173`

---

## 📧 Notifications email

| Événement | Destinataire |
| --- | --- |
| Création de compte | Nouvel utilisateur (bienvenue) |
| Téléchargement d'un document | Utilisateur (confirmation) |
| Activation / désactivation du compte | Utilisateur concerné |
| Changement de mot de passe | Utilisateur concerné |

> Les emails peuvent être désactivés via `bibliotech.mail.enabled: false`.

---

## 📝 Variables de configuration

| Propriété | Description |
| --- | --- |
| `spring.datasource.password` | Mot de passe MySQL |
| `bibliotech.jwt.secret` | Clé secrète JWT (min. 256 bits) |
| `bibliotech.jwt.expiration` | Durée du token en ms (défaut 86400000 = 24h) |
| `bibliotech.storage.upload-dir` | Répertoire de stockage des PDFs |
| `spring.ai.ollama.chat.options.model` | Modèle LLM Ollama |
| `spring.ai.ollama.embedding.options.model` | Modèle d'embedding (nomic-embed-text) |
| `spring.ai.vectorstore.qdrant.host` | Host Qdrant Cloud (sans https://) |
| `spring.ai.vectorstore.qdrant.api-key` | Clé API Qdrant Cloud |
| `spring.ai.vectorstore.qdrant.collection-name` | Nom de la collection (défaut: bibliotech-rag) |
| `bibliotech.rag.enabled` | Activer/désactiver le pipeline RAG |
| `bibliotech.rag.chunk-size` | Tokens par chunk (défaut: 500) |
| `bibliotech.rag.chunk-overlap` | Chevauchement entre chunks (défaut: 50) |
| `bibliotech.rag.top-k` | Chunks récupérés par requête (défaut: 3) |
| `bibliotech.rag.min-score` | Seuil minimum similarité cosinus (défaut: 0.60) |
| `bibliotech.mail.enabled` | Activer/désactiver les emails |

---

## 📌 Roadmap

* [x] Back-end Spring Boot complet (architecture Package by Feature)
* [x] Authentification JWT + RBAC
* [x] Gestion catalogue + upload PDF
* [x] Recherche simple et avancée (paginée)
* [x] Traçabilité complète des accès
* [x] Chatbot Spring AI (Ollama/qwen2.5:3b) avec persistance DB
* [x] Statistiques admin + export CSV + export PDF
* [x] Journal d'audit asynchrone
* [x] Notifications email (Spring Mail)
* [x] Rate limiting (Bucket4j)
* [x] Intégration OpenLibrary API
* [x] Text-to-Speech (PDFBox + espeak-ng)
* [x] Documentation Swagger/OpenAPI
* [x] CC Sync — sous-titres WebVTT synchronisés
* [x] **Pipeline RAG (nomic-embed-text + Qdrant Cloud) — réponses ancrées dans le contenu des PDFs**
* [x] **Front-end React.js & Intégration du Design System "Earth Library" (Haitam Khouna)**
* [x] **Moteur Double Thème Avancé avec isolation de spécificité CSS (Haitam Khouna)**

---

## 📄 Licence

Ce projet est développé dans le cadre d'un projet de fin d'année à l'**EMSI Rabat**.

© 2026 Bilal Elakry & Haitam Khouna — Tous droits réservés.

---

