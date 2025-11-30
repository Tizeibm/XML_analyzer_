# ![Node.js](https://img.shields.io/badge/Node.js-339933?style=flat&logo=node.js&logoColor=white) ![Java](https://img.shields.io/badge/Java-007396?style=flat&logo=java&logoColor=white) ![VSCode](https://img.shields.io/badge/Visual%20Studio%20Code-007ACC?style=flat&logo=visual-studio-code&logoColor=white)

# XML_analyzer_Njangihost

## Description du projet
XML_analyzer_Njangihost est un projet conçu pour analyser et valider des fichiers XML en utilisant une architecture de serveur de langage. Il intègre des fonctionnalités avancées de validation XML et d'extraction de données, facilitant ainsi le travail avec des fichiers XML complexes. Ce projet est idéal pour les développeurs souhaitant intégrer des fonctionnalités de traitement XML dans leurs applications.

### Fonctionnalités clés
- Validation de fichiers XML selon des schémas XSD.
- Extraction de zones spécifiques à partir de fichiers XML.
- Interface de serveur de langage pour une intégration fluide avec des éditeurs de code comme Visual Studio Code.

## Stack Technologique
| Technologie       | Description                     |
|-------------------|---------------------------------|
| ![Node.js](https://img.shields.io/badge/Node.js-339933?style=flat&logo=node.js&logoColor=white) | Environnement d'exécution JavaScript côté serveur. |
| ![Java](https://img.shields.io/badge/Java-007396?style=flat&logo=java&logoColor=white) | Langage de programmation utilisé pour le serveur de langage XML. |
| ![Visual Studio Code](https://img.shields.io/badge/Visual%20Studio%20Code-007ACC?style=flat&logo=visual-studio-code&logoColor=white) | Éditeur de code utilisé pour le développement. |

## Instructions d'installation

### Prérequis
- Node.js (version 14 ou supérieure)
- Java (version 11 ou supérieure)
- Maven (pour le projet Java)

### Guide d'installation
1. **Clonez le dépôt** :
   ```bash
   git clone https://github.com/Tizeibm/XML_analyzer_Njangihost.git
   cd XML_analyzer_Njangihost
   ```

2. **Installez les dépendances pour le projet Node.js** :
   ```bash
   cd xml-analyzer
   npm install
   ```

3. **Construisez le projet Java** :
   ```bash
   cd xml-lsp-server
   mvn install
   ```

### Configuration de l'environnement
Aucune variable d'environnement spécifique n'a été détectée dans le code. Assurez-vous que votre environnement est configuré avec les versions requises de Node.js et Java.

## Utilisation

### Lancer le projet
Pour démarrer le serveur de langage XML, exécutez la commande suivante dans le répertoire `xml-lsp-server` :
```bash
mvn exec:java -Dexec.mainClass="com.xml.lspserver.XmlLanguageServer"
```

### Exemples d'utilisation
Une fois le serveur en cours d'exécution, vous pouvez utiliser un éditeur comme Visual Studio Code pour ouvrir des fichiers XML et bénéficier des fonctionnalités de validation et d'extraction offertes par l'extension.

## Structure du projet
```
XML_analyzer_Njangihost/
├── xml-analyzer/
│   ├── src/
│   │   └── extension.ts          # Code de l'extension Visual Studio Code
│   ├── package.json               # Dépendances et scripts du projet Node.js
│   ├── CHANGELOG.md               # Journal des modifications
│   └── tsconfig.json              # Configuration TypeScript
└── xml-lsp-server/
    ├── src/
    │   └── main/
    │       └── java/             # Code source du serveur de langage XML
    ├── pom.xml                   # Configuration Maven pour le projet Java
    ├── books.xml                 # Exemple de fichier XML
    └── books.xsd                 # Schéma XSD pour validation
```

### Explication des répertoires principaux
- **xml-analyzer/** : Contient le code de l'extension Visual Studio Code pour l'analyse XML.
- **xml-lsp-server/** : Contient le code du serveur de langage XML, géré par Maven, avec les fichiers sources Java et les exemples de fichiers XML.

## Contribuer
Les contributions sont les bienvenues ! Pour contribuer, veuillez suivre ces étapes :
1. Forkez le projet.
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/YourFeature`).
3. Commitez vos changements (`git commit -m 'Ajout d'une nouvelle fonctionnalité'`).
4. Poussez vers la branche (`git push origin feature/YourFeature`).
5. Ouvrez une Pull Request.

Nous vous remercions de votre intérêt pour le projet XML_analyzer_Njangihost !
