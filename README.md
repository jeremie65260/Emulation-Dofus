Emulateur Game de Rpg-rampage, basé sur casper.

# Game-Rpg-rampage

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