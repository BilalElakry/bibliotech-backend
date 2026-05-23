# 📚 BiblioTech — Plateforme de Bibliothèque Numérique PDF

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![React](https://img.shields.io/badge/React.js-18-61DAFB)
![Spring AI](https://img.shields.io/badge/Spring%20AI-0.8.1-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

> Plateforme web complète de gestion de bibliothèque numérique permettant la centralisation,
> la gestion et la diffusion sécurisée de ressources documentaires au format PDF,
> enrichie d'un assistant conversationnel intelligent basé sur Spring AI, d'un module
> Text-to-Speech (TTS) et d'un système de sous-titres synchronisés (CC Sync / WebVTT).

---

## 👥 Équipe

| Membre | Rôle |
|--------|------|
| **Bilal Elakry** | Back-end, Sécurité JWT, StorageService, Chatbot Spring AI, TTS, CC Sync, Audit, Stats, OpenLibrary |
| **Haitam Khouna** | Diagrammes UML, Module Documents, Interface React.js |

**Encadrante :** Mme Aarich Mounia  
**Établissement :** EMSI Rabat — 4ème année cycle ingénieur (4DDSIR)  
**Année académique :** 2025–2026

---

## 🎯 Fonctionnalités

### 👤 Gestion des utilisateurs
- Inscription publique (`/api/auth/register`) avec email de bienvenue automatique
- Authentification sécurisée par JWT (expiration 24h)
- Deux rôles : `ADMIN` et `UTILISATEUR`
- Gestion du profil et changement de mot de passe (avec notification email)
- Activation / désactivation des comptes par l'admin (avec notification email)

### 📄 Catalogue documentaire
- Ajout, modification, suppression de documents PDF
- Recherche simple (titre, auteur, ISBN) et recherche avancée (filtres par catégorie, année min/max)
- Pagination et tri configurables
- Gestion des catégories thématiques
- Upload sécurisé de fichiers PDF (max 50 Mo)
- Intégration **OpenLibrary API** pour pré-remplir les métadonnées par ISBN ou mot-clé

### 📥 Accès et téléchargement
- Téléchargement sécurisé de PDF (endpoint REST protégé par JWT)
- Lecture en ligne via visionneuse intégrée
- Traçabilité complète des accès (téléchargements + lectures en ligne)
- Compteur de téléchargements par document
- Email de confirmation automatique à chaque téléchargement

### 🤖 Chatbot Spring AI
- Assistant conversationnel en langage naturel (français)
- Contexte dynamique basé sur le catalogue réel au moment de la requête
- Mémoire conversationnelle par session (fenêtre glissante de 20 messages)
- Propulsé par **Ollama + TinyLlama** (100% local, gratuit, sans clé API)

### 🔊 Text-to-Speech (TTS)
- Extraction de texte depuis les PDFs via **Apache PDFBox**
- Génération audio WAV via **espeak-ng** (voix française ou anglaise)
- Endpoint de récupération du texte brut pour la Web Speech API du navigateur
- Limité aux 10 premières pages et 3 000 caractères pour les performances

### 🎬 CC Sync — Sous-titres synchronisés
- Découpage du texte PDF en segments de 12 mots avec horodatage précis
- Timestamps calculés en fonction de la vitesse espeak-ng (150 WPM) pour une synchronisation exacte
- Export **WebVTT** standard, compatible HTML5 `<track>` et tous les lecteurs media
- Export **JSON** des segments pour intégration React personnalisée
- Filtre de bruit intelligent : suppression des en-têtes, URLs, numéros de page, lignes en majuscules
- Paramètres `maxPages` et `skipPages` pour ignorer les pages de couverture
- Numéro de page inclus dans chaque segment pour navigation contextuelle

### 📊 Statistiques & Audit
- Tableau de bord administrateur : total documents, utilisateurs, téléchargements, lectures
- Top 5 des documents les plus téléchargés
- 10 accès les plus récents
- Export CSV des historiques d'accès
- Journal d'audit complet (actions LOGIN, UPLOAD, DELETE, DOWNLOAD, etc.) avec niveaux INFO / WARNING / ERROR
- Journalisation asynchrone (`@Async`) pour ne pas bloquer les requêtes

### 🔒 Rate Limiting
- Limitation à **60 requêtes par minute** par adresse IP (Bucket4j)
- Réponse HTTP 429 avec message explicite en cas de dépassement

---

## 🛠️ Stack technologique

| Couche | Technologie |
|--------|-------------|
| **Back-end** | Java 17, Spring Boot 3.2.5 |
| **Sécurité** | Spring Security, JWT (jjwt 0.12.5), BCrypt |
| **Rate Limiting** | Bucket4j 8.10.1 |
| **Persistance** | Spring Data JPA, Hibernate, MySQL 8 / MariaDB |
| **IA conversationnelle** | Spring AI 0.8.1, Ollama, TinyLlama |
| **Extraction PDF** | Apache PDFBox 3.0.2 |
| **Text-to-Speech** | espeak-ng (local), Web Speech API |
| **CC Sync** | ChunkingService (PDFBox) + VttSerializer (WebVTT) |
| **Métadonnées livres** | OpenLibrary REST API |
| **Notifications email** | Spring Boot Mail (SMTP Gmail) |
| **Documentation API** | SpringDoc OpenAPI / Swagger UI 2.3.0 |
| **Stockage PDF** | Système de fichiers local (StorageService) |
| **Front-end** | React.js 18 (à venir) |
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
│   │   │   ├── chatbot/            # Assistant Spring AI (Ollama/TinyLlama)
│   │   │   ├── stats/              # Statistiques admin + export CSV
│   │   │   ├── audit/              # Journal d'audit asynchrone
│   │   │   ├── notification/       # Service email (SMTP) + EmailProperties
│   │   │   ├── openlibrary/        # Intégration OpenLibrary API (ISBN lookup + search)
│   │   │   ├── tts/                # TTS + CC Sync
│   │   │   │   ├── TtsController.java        # 4 endpoints (audio, text, captions, vtt)
│   │   │   │   ├── TtsService.java           # Extraction texte + génération WAV
│   │   │   │   ├── ChunkingService.java      # Découpage PDF en segments horodatés
│   │   │   │   ├── VttSerializer.java        # Sérialisation WebVTT
│   │   │   │   ├── CaptionSegment.java       # Record: index, startMs, endMs, text, page
│   │   │   │   ├── TtsCaptionResponse.java   # Record: réponse JSON captions
│   │   │   │   └── TtsTextResponse.java      # Record: réponse JSON texte brut
│   │   │   ├── storage/            # Stockage fichiers PDF (StorageService + StorageProperties)
│   │   │   ├── security/           # JwtAuthenticationFilter, JwtUtils, RateLimitFilter, UserDetailsServiceImpl
│   │   │   ├── config/             # SecurityConfig, RestTemplateConfig
│   │   │   └── exception/          # GlobalExceptionHandler, BusinessException, ResourceNotFoundException
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/com/bibliotech/
│           └── BibliotechApplicationTests.java
├── uploads/                        # Fichiers PDF stockés (gitignore)
├── pom.xml
└── README.md
```

---

## ⚙️ Prérequis

- **Java 17** — [Download](https://www.oracle.com/java/technologies/downloads/#java17)
- **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- **MySQL / MariaDB** — via XAMPP ou installation standalone
- **Ollama** — [Download](https://ollama.com)
- **espeak-ng** *(optionnel, pour TTS audio + CC sync)* — [Download](https://github.com/espeak-ng/espeak-ng/releases)
- **Git** — [Download](https://git-scm.com)

---

## 🚀 Installation et démarrage

### 1. Cloner le projet

```bash
git clone https://github.com/votre-username/bibliotech.git
cd bibliotech
```

### 2. Démarrer MySQL

Via XAMPP : démarrer le module **MySQL**

### 3. Démarrer Ollama et charger le modèle

```bash
ollama pull tinyllama
# Ollama démarre automatiquement sur http://localhost:11434
```

### 4. Configurer `application.yaml`

```yaml
spring:
  datasource:
    username: root
    password:           # votre mot de passe MySQL (vide pour XAMPP par défaut)
  mail:
    username: votre-email@gmail.com
    password: votre-app-password   # mot de passe d'application Gmail

bibliotech:
  mail:
    enabled: true        # passer à false pour désactiver les emails
    from: votre-email@gmail.com
```

### 5. Lancer l'application

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080**  
La documentation Swagger est accessible sur **http://localhost:8080/swagger-ui/index.html**

### 6. Créer le premier administrateur

Exécuter dans phpMyAdmin ou MySQL :

```sql
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, role, actif, date_inscription)
VALUES (
    'Admin', 'BiblioTech', 'admin@bibliotech.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ADMIN', true, CURDATE()
);
```
> Mot de passe : **`password`**

---

## 📡 API Endpoints

### 🔐 Authentification

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/api/auth/login` | Public | Connexion — retourne le JWT |
| `POST` | `/api/auth/register` | Public | Inscription (rôle UTILISATEUR) |
| `POST` | `/api/auth/logout` | Authentifié | Déconnexion (stateless, côté client) |

### 📄 Documents

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/documents` | Authentifié | Liste paginée / recherche (`q`, `categorieId`, `sortBy`, `direction`) |
| `GET` | `/api/documents/advanced` | Authentifié | Recherche avancée (`q`, `categorieId`, `anneeMin`, `anneeMax`) |
| `GET` | `/api/documents/{id}` | Authentifié | Détails d'un document |
| `POST` | `/api/documents` | Admin | Ajouter un document + upload PDF (multipart) |
| `PUT` | `/api/documents/{id}` | Admin | Modifier un document (+ remplacement PDF optionnel) |
| `DELETE` | `/api/documents/{id}` | Admin | Supprimer un document (+ suppression du fichier PDF) |
| `GET` | `/api/documents/{id}/pdf` | Authentifié | Télécharger le PDF (attachment) |
| `GET` | `/api/documents/{id}/pdf/inline` | Authentifié | Lire le PDF en ligne (inline) |

### 📁 Catégories

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/categories` | Admin | Liste des catégories |
| `GET` | `/api/categories/{id}` | Admin | Détails d'une catégorie |
| `POST` | `/api/categories` | Admin | Créer une catégorie |
| `PUT` | `/api/categories/{id}` | Admin | Modifier une catégorie |
| `DELETE` | `/api/categories/{id}` | Admin | Supprimer (refus si des documents y sont rattachés) |

### 👥 Utilisateurs

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/utilisateurs` | Admin | Liste de tous les utilisateurs |
| `GET` | `/api/utilisateurs/{id}` | Admin | Détails d'un utilisateur |
| `POST` | `/api/utilisateurs` | Admin | Créer un compte utilisateur |
| `PATCH` | `/api/utilisateurs/{id}/toggle` | Admin | Activer / Désactiver un compte |
| `DELETE` | `/api/utilisateurs/{id}` | Admin | Supprimer un compte |
| `GET` | `/api/utilisateurs/me` | Authentifié | Mon profil |
| `PUT` | `/api/utilisateurs/me/password` | Authentifié | Changer mon mot de passe |

### 📥 Accès & Historique

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/acces/documents/{id}/telecharger` | Authentifié | Télécharger + enregistrer l'accès |
| `GET` | `/api/acces/documents/{id}/lire` | Authentifié | Lire en ligne + enregistrer l'accès |
| `GET` | `/api/acces/mon-historique` | Authentifié | Historique de mes accès |
| `GET` | `/api/acces/utilisateurs/{userId}` | Admin | Historique d'accès d'un utilisateur |

### 📊 Statistiques

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/stats` | Admin | Tableau de bord complet (totaux + top 5 + accès récents) |
| `GET` | `/api/stats/export/csv` | Admin | Export CSV de tous les accès |

### 🗂️ Audit

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/audit` | Admin | Journal paginé de toutes les actions |
| `GET` | `/api/audit/recent` | Admin | 20 actions les plus récentes |
| `GET` | `/api/audit/errors` | Admin | Actions en erreur uniquement |
| `GET` | `/api/audit/utilisateurs/{email}` | Admin | Journal paginé par utilisateur |

### 🤖 Chatbot

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/api/chatbot/message` | Authentifié | Envoyer un message (`message`, `sessionId`) |
| `DELETE` | `/api/chatbot/session/{sessionId}` | Authentifié | Effacer l'historique de session |

### 🔊 Text-to-Speech & CC Sync

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/tts/documents/{id}/audio` | Authentifié | Génère un fichier WAV depuis le PDF (`langue` : `fr`/`en`) |
| `GET` | `/api/tts/documents/{id}/text` | Authentifié | Texte extrait du PDF pour Web Speech API (`maxChars`) |
| `GET` | `/api/tts/documents/{id}/captions` | Authentifié | Segments JSON horodatés (`maxPages`, `skipPages`) |
| `GET` | `/api/tts/documents/{id}/vtt` | Authentifié | Fichier WebVTT téléchargeable (`maxPages`, `skipPages`) |

#### Paramètres CC Sync

| Paramètre | Défaut | Description |
|-----------|--------|-------------|
| `maxPages` | `10` | Nombre maximum de pages à traiter |
| `skipPages` | `0` | Nombre de pages à ignorer depuis le début (ex: `1` pour sauter la couverture) |

#### Exemple de réponse `/captions`

```json
{
  "documentId": 1,
  "titre": "Pride and Prejudice",
  "totalSegments": 148,
  "totalDurationMs": 707600,
  "segments": [
    { "index": 0, "startMs": 0,    "endMs": 4800,  "text": "This book arises from a deep knowledge of domestic life and the", "page": 2 },
    { "index": 1, "startMs": 4800, "endMs": 9600,  "text": "human condition. It is a book full of satire, sharp, profound and", "page": 2 },
    { "index": 2, "startMs": 9600, "endMs": 14400, "text": "anti-romantic at the same time. Pride and Prejudice has captivated several generations", "page": 2 }
  ]
}
```

#### Intégration front-end (pour Haitam)

```jsx
// Lecteur audio avec sous-titres synchronisés
<audio controls src={`/api/tts/documents/${id}/audio`}>
  <track
    kind="captions"
    src={`/api/tts/documents/${id}/vtt?skipPages=1`}
    srcLang="fr"
    label="Français"
    default
  />
</audio>
```

### 📖 OpenLibrary

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/openlibrary/isbn/{isbn}` | Admin | Récupérer les métadonnées d'un livre par ISBN |
| `GET` | `/api/openlibrary/search?q=...&limit=10` | Admin | Rechercher des livres par mot-clé |

---

## 🔒 Sécurité

- Mots de passe hachés avec **BCrypt**
- Authentification sans état via **JWT** (expiration configurable, défaut 24h)
- Contrôle d'accès par rôle **RBAC** (`ADMIN` / `UTILISATEUR`) via `@PreAuthorize`
- Filtrage JWT sur chaque requête (`OncePerRequestFilter`)
- Rate limiting : 60 requêtes/minute/IP (Bucket4j) — HTTP 429 en cas de dépassement
- Fichiers PDF inaccessibles sans token JWT valide
- CORS configuré pour `http://localhost:3000`
- Conformité **Loi 09-08** (protection des données personnelles)

---

## 📧 Notifications email

Les emails suivants sont envoyés de manière **asynchrone** (`@Async`) :

| Événement | Destinataire |
|-----------|-------------|
| Création de compte | Nouvel utilisateur (bienvenue) |
| Téléchargement d'un document | Utilisateur (confirmation) |
| Activation / désactivation du compte | Utilisateur concerné |
| Changement de mot de passe | Utilisateur concerné |
| Ajout d'un nouveau document | Notification admin (optionnel) |

> Les emails peuvent être désactivés globalement via `bibliotech.mail.enabled: false` dans `application.yaml`.

---

## 🧪 Tests API

Importer la collection Postman depuis le dossier `/postman` du projet.

Séquence de test recommandée :
1. `POST /api/auth/login` → récupérer le token JWT
2. `POST /api/categories` → créer une catégorie
3. `POST /api/documents` (multipart) → uploader un document PDF
4. `GET /api/documents` → vérifier le catalogue paginé
5. `GET /api/acces/documents/1/telecharger` → télécharger et tracer l'accès
6. `POST /api/chatbot/message` → tester l'assistant
7. `GET /api/tts/documents/1/audio` → générer l'audio WAV
8. `GET /api/tts/documents/1/captions?skipPages=1` → vérifier les segments JSON
9. `GET /api/tts/documents/1/vtt?skipPages=1` → télécharger le fichier WebVTT
10. `GET /api/stats` → consulter le tableau de bord
11. `GET /api/audit` → consulter le journal d'audit
12. `GET /api/openlibrary/isbn/9782100819775` → lookup ISBN

---

## 📝 Variables de configuration

| Propriété | Valeur par défaut | Description |
|-----------|-------------------|-------------|
| `spring.datasource.password` | *(vide)* | Mot de passe MySQL |
| `bibliotech.jwt.secret` | `bibliotech-jwt-secret-key-...` | Clé secrète JWT (min. 256 bits) |
| `bibliotech.jwt.expiration` | `86400000` | Durée du token en ms (24h) |
| `bibliotech.storage.upload-dir` | `./uploads` | Répertoire de stockage des PDFs |
| `spring.ai.ollama.chat.options.model` | `tinyllama` | Modèle LLM Ollama |
| `bibliotech.mail.enabled` | `false` | Activer/désactiver les emails |
| `bibliotech.mail.from` | — | Adresse expéditeur |
| `bibliotech.tts.max-chars` | `3000` | Limite de caractères pour le TTS audio |
| `bibliotech.tts.max-pages` | `10` | Nombre max de pages extraites |

---

## 📌 Roadmap

- [x] Back-end Spring Boot complet (architecture Package by Feature)
- [x] Authentification JWT + RBAC
- [x] Gestion catalogue + upload PDF
- [x] Recherche simple et avancée (paginée)
- [x] Traçabilité complète des accès
- [x] Chatbot Spring AI (Ollama/TinyLlama)
- [x] Statistiques admin + export CSV
- [x] Journal d'audit asynchrone
- [x] Notifications email (Spring Mail)
- [x] Rate limiting (Bucket4j)
- [x] Intégration OpenLibrary API
- [x] Text-to-Speech (PDFBox + espeak-ng)
- [x] Documentation Swagger/OpenAPI
- [x] CC Sync — sous-titres WebVTT synchronisés
- [ ] Front-end React.js (en cours — Haitam Khouna)
- [ ] Pipeline RAG sur le contenu des PDFs
- [ ] Export rapports PDF

---

## 📄 Licence

Ce projet est développé dans le cadre d'un projet de fin d'année à l'**EMSI Rabat**.  
© 2026 Bilal Elakry & Haitam Khouna — Tous droits réservés.