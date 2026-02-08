AdherentsApp
Présentation

AdherentsApp est une application Android destinée à la gestion complète d’un club (adhérents, familles, cotisations, saisons, assemblées générales, communications et suivi interne).

Le projet a pour objectif de remplacer :

les fichiers Excel

les listes papier

les messages dispersés

le suivi manuel des cotisations

par un système centralisé, structuré et évolutif.

L’application est conçue pour fonctionner dans un contexte réel d’association sportive, avec des contraintes réelles :

familles

mineurs

responsables légaux

remises

licences fédérales

AG et comptes rendus

saisons annuelles

Objectifs du projet
Objectifs fonctionnels

gérer les adhérents

gérer les foyers

calculer les cotisations

suivre les paiements

gérer les saisons

générer des documents officiels

communiquer avec les membres

gérer les rôles et permissions

Objectifs techniques

architecture propre et maintenable

sécurité Firestore stricte

UI fluide et simple

code modulaire et refactorisable

Stack technique

Application Android :

Kotlin

Jetpack Compose

MVVM

Hilt

Navigation Compose

Backend :

Firebase Auth

Firestore

Firebase Storage

Cloud Functions

Architecture générale

L’application suit une séparation claire :

UI (Compose)
ViewModel
Repository
Firestore

Objectifs :

logique métier isolée

UI découplée

règles Firestore strictes

Structure du projet
app/

Application Android principale

Contient :

écrans

viewmodels

modèles

navigation

repositories

functions/

Fonctions backend Firebase

Utilisées pour :

traitements automatiques

sécurité

automatisations futures

scripts/

Scripts internes et outils de maintenance.

Ne contient pas de secrets dans le repo.

Modèle fonctionnel
Adhérent

Un adhérent peut être :

mineur

majeur autonome

majeur non autonome

Peut être lié à :

un foyer

un responsable légal

un rôle

Foyer

Un foyer regroupe :

un responsable

des pratiquants

éventuellement des majeurs non autonomes

Permet :

calcul de cotisation

remises familiales

Saison

Une saison contient :

tarifs

remises

paramètres financiers

AG

comptes rendus

cotisations

Modules de l’application
Gestion des adhérents

Fonctionnalités existantes :

création

modification

rattachement foyer

photo

navigation dans la liste

État :
Stable mais encore en amélioration.

À faire :

optimisation du détail adhérent

amélioration rôles

Gestion des rôles

Rôles existants :

SuperAdmin

Admin

Trésorier

Secrétaire

Jury

Membre

État :
Fonctionnel partiellement.

À faire :

refactor complet des permissions

séparation Staff / Membres

Cotisations

Fonctionnalités existantes :

calcul automatique

remises

exemptions

licence

État :
Fonctionnel.

À faire :

export rapport

historique complet

Saisons

Fonctionnalités existantes :

configuration

paramètres financiers

État :
Partiellement complet.

À faire :

clôture saison

archivage

création nouvelle saison

Assemblée Générale / Compte rendu

Fonctionnalités existantes :

saisie

draft

À faire :

validation officielle

PDF final

verrouillage

Notifications et groupes

Fonctionnalités existantes :

création groupes

À faire :

édition groupe

automatisation envois

État global du projet

Modules stables :

adhérents

foyers

cotisations

configuration saison

Modules en cours :

rôles

AG / CR

notifications

Modules prévus :

espace membre

statistiques

exports

Roadmap technique
Phase actuelle

Refactor rôles et adhérents.

Prochaine étape

Clôture de saison complète.

Étapes futures

rapports PDF

espace membre

statistiques

Sécurité

Le projet utilise :

règles Firestore strictes

contrôle des rôles

validation des champs

Ne jamais commiter :

service accounts

google-services.json réel

clés API

Installation

Cloner le projet :

git clone https://github.com/darkandwhiteangels/AdherentsApp.git


Ouvrir avec Android Studio.

Configurer Firebase :

ajouter google-services.json

Lancer l’application.

Organisation Git recommandée

Branches :

main → stable

dev → développement

Commits par module.

Notes de développement

Projet en évolution continue.
Refactoring régulier.
Architecture améliorée progressivement.

Vision long terme

Faire d’AdherentsApp :

un outil complet de gestion associative

robuste

simple à utiliser

réutilisable par d’autres clubs

Suivi personnel du projet
Ce qui est fait

structure Firebase

gestion adhérents

gestion foyers

cotisations

base saison

groupes notifications base

En cours

refactor rôles

invitations parent

AG et CR

amélioration UI

À faire

clôture saison complète

rapports PDF

dashboard

espace membre

Journal technique (à tenir à jour)

Version actuelle : Dev
Dernier refactor majeur : rôles / adhérents
Priorité actuelle : clôture saison
