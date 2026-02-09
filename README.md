![le logo de AnteStorm Labs](http://www.image-heberg.fr/files/thumbs/17705699754186274869.png) 

<center><h1> Architecture fonctionnelle globale </h1></center>

<h3>Flux principal du système vision globale de l'application</h3>

```mermaid
---
config:
  layout: dagre
  theme: base
---
flowchart LR

    Tache1["Fonction terminée"]:::done
    Tache2["Fonction en cours"]:::wip
    Tache3["Fonction à faire"]:::todo

    classDef done fill:#C8E6C9,stroke:#2E7D32,stroke-width:2px,color:#1B5E20;
    classDef wip fill:#FFE0B2,stroke:#EF6C00,stroke-width:2px,color:#E65100;
    classDef todo fill:#ECEFF1,stroke:#546E7A,stroke-width:2px,color:#263238;

    n4["Staff"] --> M["Main"]
    M === bNb["Bottom NavBar"] & mTb["Menu TopBar"]

    subgraph BottomNav
      bNb --> A["Adhérents"] & P1["Présence"] & n2["Cotisations"] & n5["Bureau"]

      subgraph Module_Adherents["Module Adhérents"]
        A --> F["Fiche détail"] & S["Swipe del/archi."] & Ad["Ajouter un adhérent"]
      end

      subgraph Module_Presences["Module Présences"]
        P1 --> C["Baby"] & n1["Enfants -u14"] & n3["Adulte"]
      end

      subgraph Module_Cotisations["Module Cotisations"]
        n2 === n2mTb["TopBar Cotisations"]
        n2mTb --> n12(["Générer Dossiers"]) & n13(["Recalculer Dossiers"]) & n14(["Liste Responsables"]) & n15(["Params Saison"]) & n16(["Rapport cotisations"]) & n17(["Rapport Lic. FFK"]) & n27(["Clôture Saison"]) & n42["Clic foyer"]

        n14 --- n18["Listing adults only RGPD"]
        n18 --> n19["Slide R"] & n20["Slide L"] & n21["Clic"] & n22["Long Press"]
        n19 --> n23["Edit Resp."]
        n20 --> n24["Supprime Resp."]
        n21 --> n25["Select invitation accès App"]
        n22 --> n26["Invitation accès App"]

        n42 --> n43["Système de remise"] & n44["Saisie Aide"] & n45["Saisie Règlement"] & n46["Statut foyer"] & n47["Attestation"]
      end

      subgraph Bureau["Bureau"]
        n5 --> n28["Params Club"] & n33["AG / CR"] & n34["Stats total"]
        n28 --> n29["Sign Président"] & n30["Sign Secrétaire"] & n31["Logo Club"] & n32["Nom et ville Club"]
      end
    end

    subgraph TopNav["TopNav"]
      mTb --> n6["Notifications Push"] & n35["Listing SMS/Email"] & n38["Déconnexion"] & n39["Kihon Board"]
      n6 --> n7(("Nouveau Msg")) & n8["Gestion Gr Perso"]
      n8 --> n9["Ajouter Gr"] & n10["Supprimer Gr"] & n11["Editer Gr"]
      n35 --> n36["Envoie de SMS"] & n37["Envoie de Email"]
      n39 --> n40["Catalogue Kihon"] & n41["En cours..."]
    end

    %% ============
    %% COLOR LEGEND
    %% ============
    classDef done fill:#C8E6C9,stroke:#2E7D32,stroke-width:2px,color:#1B5E20;
    classDef wip  fill:#FFE0B2,stroke:#EF6C00,stroke-width:2px,color:#E65100;
    classDef todo fill:#ECEFF1,stroke:#546E7A,stroke-width:2px,color:#263238;

    %% ============
    %% DONE (stable)
    %% ============
    class n4,M,bNb,mTb,n38 done;
    class A,F,S,Ad done;
    class P1,C,n1,n3 done;
    class n2,n2mTb,n12,n13,n15,n42,n43,n44,n45,n46,n47 done;
    class n14,n18,n19,n20,n21,n22,n23,n24,n25,n26 done;
    class n6,n7,n8,n9,n10,n11,n35,n36,n37 done;


    %% ============
    %% WIP (en cours / partiel)
    %% ============
    class n5,n28,n29,n30,n31,n32,n33 wip;
    class n39,n40,n41 wip;
    class n16,n17 wip;


    %% ============
    %% TODO (à faire)
    %% ============
    class n27,n34 todo;
```

Ce flux représente le fonctionnement réel du club au fil de l’année.

# Cycle de vie d’un adhérent

```mermaid
---
config:
  theme: base
---
flowchart TB
    A("Arrivée d’un pratiquant au club") --> B("Prise d’informations<br>Identité / contact / âge") & B1(["Adhérent déjà<br>existant"])
    B --> C{"Mineur ou adulte ?"}
    C -- Mineur --> D1("Mineur : infos responsable légal<br>+ contact + autorisations")
    D1 --> E("Création / mise à jour fiche adhérent")
    E --> F{"Foyer existant à rattacher ?"} & F
    C -- Adulte --> D2("Adulte : infos personnelles<br>(pas de responsable légal)")
    D2 --> E
    F -- Oui --> G("Rattachement à un foyer existant")
    G --> I("Calcul cotisation (foyer / saison)<br>remises / licence / exemptions")
    F -- Non --> H("Création d’un nouveau foyer")
    H --> I
    I --> J("Paiement / Statut cotisation<br>(à jour / partiel / en attente)")
    J --> K("Participation aux cours<br>présences / suivi")
    K --> L{"Fin de saison ?"}
    L -- Non --> K
    L -- Oui --> M("Clôture saison<br>verrouillage + snapshot")
    M --> N("Archivage historique adhérent / foyer")
    N --> O("Réinscription nouvelle saison")
    O --> B
    B1 --> E

     A:::green
     B:::orange
     C:::yellow
     D1:::blue
     E:::pink
     F:::yellow
     D2:::blue
     G:::blue
     I:::purple
     H:::blue
     J:::orange
     K:::blue
     L:::yellow
     M:::green
     N:::orange
     O:::blue
     B1:::brown
    classDef green fill:#B2DFDB,stroke:#00897B,stroke-width:2px
    classDef orange fill:#FFE0B2,stroke:#FB8C00,stroke-width:2px
    classDef blue fill:#BBDEFB,stroke:#1976D2,stroke-width:2px
    classDef yellow fill:#FFF9C4,stroke:#FBC02D,stroke-width:2px
    classDef pink fill:#F8BBD0,stroke:#C2185B,stroke-width:2px
    classDef purple fill:#E1BEE7,stroke:#8E24AA,stroke-width:2px
    classDef brown fill:#E08543,stroke:#3D2412,stroke-width:2px
```

# Modèle relationnel simplifié
```mermaid
erDiagram
  ADHERENT ||--o{ ENROLLMENT : "a des inscriptions"
  SEASON   ||--o{ ENROLLMENT : "contient"
  HOUSEHOLD ||--o{ ENROLLMENT : "regroupe (saison)"

  HOUSEHOLD ||--o{ GUARDIANSHIP : "a des responsables"
  ADHERENT  ||--o{ GUARDIANSHIP : "peut etre responsable"
  ADHERENT  ||--o{ GUARDIANSHIP : "peut etre mineur"

  SEASON ||--o{ HOUSEHOLD_COTISATION : "cotisation foyer"
  HOUSEHOLD ||--o{ HOUSEHOLD_COTISATION : "recoit"
```
# Architecture logique de l’application
```mermaid
flowchart TD

UI[Compose UI] --> VM[ViewModel]
VM --> REPO[Repository]
REPO --> FIRESTORE[Firestore]
REPO --> AUTH[Firebase Auth]
REPO --> STORAGE[Firebase Storage]
```
# Organisation des données Firestore (conceptuelle)

Structure logique :

users/
adherents/
households/
seasons/
cotisations/
season_admin/
notifications/
groups/

États d’une saison
stateDiagram-v2
[*] --> Active
Active --> AG_PREPARED
AG_PREPARED --> MINUTES_READY
MINUTES_READY --> CLOSED
CLOSED --> ARCHIVED

États d’un document AG / CR
stateDiagram-v2
[*] --> Draft
Draft --> Editable
Editable --> Validated
Validated --> Locked

Organisation interne recommandée du code

Structure logique idéale :

ui/
viewmodel/
repository/
model/
domain/
firebase/
navigation/


Objectif :

UI simple

logique métier isolée

accès données centralisé

Roadmap technique détaillée
Phase actuelle

Refactor des rôles et des écrans adhérents.

Phase suivante

Clôture saison complète :

Objectifs :

verrouiller cotisations

générer snapshot

archiver

ouvrir nouvelle saison

Phase suivante

Rapports et documents :

PDF cotisations

PDF AG

export CSV

Phase suivante

Espace membres :

vidéos

progression

présence

auto-évaluation

Modules et état réel
Module	État	Remarques
Authentification	Stable	Fonctionne
Adhérents	Stable	Améliorations UI prévues
Foyers	Stable	Logique OK
Cotisations	Stable	Rapport à faire
Saisons	Partiel	Clôture à implémenter
AG / CR	En cours	Workflow à finaliser
Notifications	Partiel	Edition groupe à améliorer
Rôles	Refactor en cours	Priorité actuelle
TODO priorisé
Priorité haute

clôture saison

refactor rôles

invitations parent

Priorité moyenne

rapports PDF

amélioration UI

Priorité basse

stats membres

espace vidéos

Journal de développement

Ajouter chaque jour :

Date :
Travail effectué :
Problèmes rencontrés :
Prochaine étape :


Ça devient extrêmement utile dans les projets longs.

Vision cible de l’application

Objectif final :

Une application capable de gérer :

plusieurs saisons

plusieurs clubs (à long terme)

documents officiels

communication centralisée

Conseil important (retour d’expérience)

Le moment où un projet devient difficile, ce n’est pas quand il devient gros…
c’est quand on ne voit plus clairement son architecture.

Un README comme celui-ci sert justement à garder la vision claire.
