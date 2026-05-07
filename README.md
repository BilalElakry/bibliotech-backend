# 📚 BiblioTech — Plateforme de Bibliothèque Numérique PDF

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![React](https://img.shields.io/badge/React.js-18-61DAFB)
![Spring AI](https://img.shields.io/badge/Spring%20AI-0.8.1-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

> Plateforme web complète de gestion de bibliothèque numérique permettant la centralisation,
> la gestion et la diffusion sécurisée de ressources documentaires au format PDF,
> enrichie d'un assistant conversationnel intelligent basé sur Spring AI.

---

## 👥 Équipe

| Membre | Rôle |
|--------|------|
| **Bilal Elakry** | Back-end, Sécurité JWT, StorageService, Chatbot Spring AI |
| **Haitam Khouna** | Diagrammes UML, Module Documents, Interface React.js |

**Encadrante :** Mme Aarich Mounia  
**Établissement :** EMSI Rabat — 4ème année cycle ingénieur (4DDSIR)  
**Année académique :** 2025–2026

---

## 🎯 Fonctionnalités

### 👤 Gestion des utilisateurs
- Inscription publique (`/api/auth/register`)
- Authentification sécurisée par JWT
- Deux rôles : `ADMIN` et `UTILISATEUR`
- Gestion du profil et changement de mot de passe
- Activation / désactivation des comptes (Admin)

### 📄 Catalogue documentaire
- Ajout, modification, suppression de documents PDF
- Recherche par titre, auteur, catégorie ou ISBN
- Gestion des catégories thématiques
- Upload sécurisé de fichiers PDF (max 50 Mo)

### 📥 Accès et téléchargement
- Téléchargement sécurisé de PDF (endpoint REST protégé par JWT)
- Lecture en ligne via visionneuse intégrée
- Traçabilité complète des accès (téléchargements + lectures)
- Compteur de téléchargements par document

### 🤖 Chatbot Spring AI
- Assistant conversationnel en langage naturel
- Recherche documentaire intelligente
- FAQ interactive
- Mémoire conversationnelle par session (`ChatMemory`)
- Propulsé par Ollama + TinyLlama (100% local, gratuit)

### 📊 Statistiques
- Tableau de bord administrateur
- Documents les plus téléchargés
- Historique d'accès par utilisateur

---

## 🛠️ Stack technologique

| Couche | Technologie |
|--------|-------------|
| **Back-end** | Java 17, Spring Boot 3.2.5 |
| **Sécurité** | Spring Security, JWT (jjwt 0.12.5), BCrypt |
| **Persistance** | Spring Data JPA, Hibernate, MySQL 8 / MariaDB |
| **IA conversationnelle** | Spring AI 0.8.1, Ollama, TinyLlama |
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
│   │   │   ├── auth/               # Authentification JWT
│   │   │   ├── catalogue/
│   │   │   │   ├── document/       # Gestion des documents PDF
│   │   │   │   └── categorie/      # Gestion des catégories
│   │   │   ├── acces/              # Téléchargements et accès PDF
│   │   │   ├── utilisateur/        # Gestion des utilisateurs
│   │   │   ├── chatbot/            # Assistant Spring AI
│   │   │   ├── storage/            # Stockage fichiers PDF
│   │   │   ├── security/           # JWT Filter, UserDetailsService
│   │   │   ├── config/             # SecurityConfig
│   │   │   └── exception/          # GlobalExceptionHandler
│   │   └── resources/
│   │       └── application.yml
│   └── test/
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

### 4. Configurer `application.yml`

```yaml
spring:
  datasource:
    username: root
    password:           # votre mot de passe MySQL
  ai:
    ollama:
      chat:
        options:
          model: tinyllama
```

### 5. Lancer l'application

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080**

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
| `POST` | `/api/auth/login` | Public | Connexion |
| `POST` | `/api/auth/register` | Public | Inscription |
| `POST` | `/api/auth/logout` | Authentifié | Déconnexion |

### 📄 Documents

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/documents` | Authentifié | Liste / recherche |
| `GET` | `/api/documents/{id}` | Authentifié | Détails |
| `POST` | `/api/documents` | Admin | Ajouter + upload PDF |
| `PUT` | `/api/documents/{id}` | Admin | Modifier |
| `DELETE` | `/api/documents/{id}` | Admin | Supprimer |
| `GET` | `/api/documents/{id}/pdf` | Authentifié | Télécharger PDF |
| `GET` | `/api/documents/{id}/pdf/inline` | Authentifié | Lire en ligne |

### 📁 Catégories

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/categories` | Admin | Liste |
| `POST` | `/api/categories` | Admin | Créer |
| `PUT` | `/api/categories/{id}` | Admin | Modifier |
| `DELETE` | `/api/categories/{id}` | Admin | Supprimer |

### 👥 Utilisateurs

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/utilisateurs` | Admin | Liste |
| `POST` | `/api/utilisateurs` | Admin | Créer |
| `PATCH` | `/api/utilisateurs/{id}/toggle` | Admin | Activer/Désactiver |
| `DELETE` | `/api/utilisateurs/{id}` | Admin | Supprimer |
| `GET` | `/api/utilisateurs/me` | Authentifié | Mon profil |
| `PUT` | `/api/utilisateurs/me/password` | Authentifié | Changer mot de passe |

### 📥 Accès PDF

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/api/acces/documents/{id}/telecharger` | Authentifié | Télécharger |
| `GET` | `/api/acces/documents/{id}/lire` | Authentifié | Lire en ligne |
| `GET` | `/api/acces/mon-historique` | Authentifié | Mon historique |
| `GET` | `/api/acces/utilisateurs/{id}` | Admin | Historique d'un user |

### 🤖 Chatbot

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/api/chatbot/message` | Authentifié | Envoyer un message |
| `DELETE` | `/api/chatbot/session/{id}` | Authentifié | Effacer la session |

---

## 🔒 Sécurité

- Mots de passe hachés avec **BCrypt**
- Authentification sans état via **JWT** (expiration : 24h)
- Contrôle d'accès par rôle **RBAC** (ADMIN / UTILISATEUR)
- Fichiers PDF inaccessibles sans token JWT valide
- Conformité **Loi 09-08** (protection des données personnelles)

---

## 🧪 Tests API

Importer la collection Postman depuis le dossier `/postman` du projet.

Séquence de test recommandée :
1. `POST /api/auth/login` → récupérer le token
2. `POST /api/categories` → créer une catégorie
3. `POST /api/documents` → uploader un document PDF
4. `GET /api/documents` → vérifier le catalogue
5. `GET /api/acces/documents/1/telecharger` → télécharger le PDF
6. `POST /api/chatbot/message` → tester l'assistant

---

## 📝 Variables d'environnement

| Variable | Valeur par défaut | Description |
|----------|-------------------|-------------|
| `spring.datasource.password` | *(vide)* | Mot de passe MySQL |
| `bibliotech.jwt.secret` | `bibliotech-256-bit-secret...` | Clé secrète JWT |
| `bibliotech.storage.upload-dir` | `./uploads` | Répertoire PDF |
| `spring.ai.ollama.chat.options.model` | `tinyllama` | Modèle LLM |

---

## 📌 Roadmap

- [x] Back-end Spring Boot complet
- [x] Authentification JWT + RBAC
- [x] Gestion catalogue + upload PDF
- [x] Traçabilité des accès
- [x] Chatbot Spring AI (Ollama)
- [ ] Front-end React.js
- [ ] Tableau de bord statistiques
- [ ] Export rapports PDF/CSV
- [ ] Pipeline RAG sur le catalogue

---

## 📄 Licence

Ce projet est développé dans le cadre d'un projet de fin d'année à l'**EMSI Rabat**.  
© 2026 Bilal Elakry & Haitam Khouna — Tous droits réservés.