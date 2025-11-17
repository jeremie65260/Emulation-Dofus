Emulateur Game de Rpg-rampage, basé sur casper.
# Game-Rpg-rampage 0.9.4:
    - Le contôle des invocations est fonctionnel
    - Correction de de la récupération des sorts lié aux potions de changement de classe ( on efface maintenant les sorts de la classe d'avant)
    - Mode cinvoc à 90 % 
# Game-Rpg-rampage 0.9.3:
    - Correction d'un bug de l'etat pesanteur qui faisait pass tour les monstres
    - Correction de la logique avec codex dans function.java : le monstre lance désormais les pièges sur cellule adjacente au player
    - Ajout de la logique ifTrapSpell dans function.java de Velzog + récupération de l'IA92 piège  de velzog
    - Correctif 1 : Reussi ! , désormais le monstre le rentrera plus dans ses propres pièges (test multiple avec piège surnois)
# Game-Rpg-rampage 0.9.3:
    - Intégration de la commande spellmax
    - sauvegarde des sorts après l'utilisation d'une potion de classe
## Version 0.9.2:
    - Création de l'IA 206 ( mix de l'IA 39 pour attirer , taper, invoquer, fuite) + injection de la méthode attackBondIfPossible() de l'IA 36
    - COrrection du Wa : il passait son tour si ne pouvait pas appliquer attackBondIfPossible()
## Version 0.9.1:
    - Création de l'Etat Invulnerable 301 pour le Wa : si son equipe meurt , il devient vulnérable
    - Création d'une IA 205 qui Buff self , alliés et invocations , plusieurs fois, pas d'attaque puis fuite
## Version 0.9.0:
    - Implémentation du système "ramener une capture d'ames à un pnj (type 12) dans capture.java
    - Codage de la logique pour ramner la pierre dans quest.java -> fonctionnel
## Version 0.8.3:
    - Correction du sort Laisse spirituelle : le monstre réssucite sur une case valide. (spellEffect.java effect 780)
    - Création d'une IA 204 ( calquée sur IA 203)  afin que le monstre wabbit squelette puisse vérifier si quelqu'un est mort dans son équipe avant de lancer laisse spirituelle.
## Version 0.8.2:
    - Fix de la génération du jet aleatoire familier objectTemplate.java
## Version 0.8.1:
    - Prise en compte des faiblesses si c'est une invocation dans formulas.java
## Version 0.8.0:
    - Ajout de la logique pour le mob Brumaire, modifications dans Fighter.java , Fight.java , Formulas.java
## Version 0.7.2:
    - Création d'IA 203:
        - Basée sur la 72 avec deux sorts de Cac utilisable ( le reste est copié/collé)
## Version 0.7.1:
    - Corredtion IA 202:
        - Inclusion de la fallback au cas ou il n'y a pas de sort Cac
        - correction du passe tour CAC.
## Version 0.7.0:
    - Création d'IA 202:
        - Basée sur la 37 avec le buff de soi même + comportement fuyard 
        + modification du palier de soin sur soi à 80%.
## Version 0.6.0:
    - Amélioration : 
        - IA 55 : Retrait de la fonction moveFarIfpossible() contre moveAutourIfPossible().
## Version 0.5.0:
    - Fix: 
        - FullMorph en donjon : 
            - On ne perd plus la fullMorph après la perte ou l'abandon d'un combat.
## Version 0.4.0:
    - Fix:
        - Fight:
            - Drop item quête avec stats. On vérifie le coffre mais aussi le joueur si il ne possède pas déjà l'item.
    - Refactoring:
        - Quest:
            - Envoie une seule fois le packet de stats une fois la récompense reçu.
    - Commande Player :
        - Correction de la position de la commande .start, utilise dorénavant celle de la Config.

## Version 0.3.0:
    - Fix:
        - Fight:
            - Drop d'items de quête, pas plus de un par joueur. Si le joueur possède déjà l'item, même son coffre ne peut le droper.
            - Si on gagne ou perd un combat, le joueur récupère toute sa vie.
    - Nouveauté:
        - Commande Joueur:
            - Commande .vie
        - Config:
            - Ajout de start_map et start_cell en config.
    - Commande Player :
        - Correction de la position de la commande .start à Incarnam en 10295,237
            
## Version 0.2.1:
    - Fix:
        - CommandAdmin:
            - item | !getitem: Envoie les stats au target.

## Version 0.2.0:
    - Fix:
        - Fight: Les items de quêtes (action -1 drop) ne peuvent se drop une seconde fois.
        - Fight: Si un joueur drop un item de quête, on lui envoie le packet de Stats.
        - CommandAdmin:
            - itemtype: envoie le packet de stats si c'est le type de quête.
            - item | !getitem: envoie le packet de stats si c'est un item de type quête.
        - Quete: Lorsque l'on donne le butin, si il y a un item de quête, on envoie les stats au joueur.
    - Refactoring:
         - Fight: Suppresion message inutiles.

## Version 0.1.0:
    - Nouveauté:
        - Player: Les items de quêtes possèdant des stats influent les statistiques du joueur.
        - Sql/Monster: La taille de base est définissable en base de donnée (base_size).

## Version 0.0.1:
    - Nouveauté:
        - Player: Ajout de la récupération de vie a 100 %
        - Monster: désactivation étoiles.
        - SQL: Les mobs officiel ne donnent plus d'xp.