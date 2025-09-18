Emulateur Game de Rpg-rampage, basé sur casper.

# Game-Rpg-rampage
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