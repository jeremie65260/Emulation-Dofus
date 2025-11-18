# Tests

Ce projet ne contient pas de suite de tests automatisés, mais il est possible de
vérifier la validité de la base de code en compilant toutes les classes Java.

```bash
mkdir -p build
javac -proc:none -cp "Libs/*:src" -d build $(find src -name '*.java')
```

La présence de `lombok` dans le classpath déclenche une erreur d'accès aux
modules internes du JDK lorsque l'annotation processing est activé par défaut.
L'option `-proc:none` désactive ce traitement et permet à la compilation de
réussir. Une fois la compilation terminée, le dossier `build` peut être
supprimé si nécessaire.
