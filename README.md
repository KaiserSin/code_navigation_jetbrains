# Code Navigator

Code Navigator is an IntelliJ IDEA plugin that provides fast, case-insensitive text search across any directory in your project. Results stream in as they are found, showing the full file path, line number, and column for each match.

## Features

Case-insensitive search across regular files under the chosen directory.

Background execution powered by Kotlin coroutines so the IDE stays responsive.

Live result list that keeps the view on the newest matches when you stay at the bottom.

Clear status updates and popup errors if something goes wrong.

A cancel button that stops a long running search right away.

## Getting Started

### Prerequisites

JDK 21  
IntelliJ IDEA Community or Ultimate (2025.2 platform)  
Gradle (wrapper included)

### Run the Plugin in IDE

```bash
./gradlew runIde
```

This launches a sandboxed IntelliJ instance with the plugin installed.

### Build

```bash
./gradlew buildPlugin
```

The packaged plugin will be placed under `build/distributions`.

### Test

```bash
./gradlew test
```

## Usage

1. Open the **Code Navigator** tool window (left side by default).
2. Enter the absolute path to the directory you want to search.
3. Enter the text you want to find.
4. Click **Start search** to begin or **Cancel search** to stop.
5. Watch results populate in real time; each entry shows `path:line:column`.

## Project Structure

`src/main/kotlin` contains the Kotlin sources for the plugin UI and search logic.  
`src/main/resources/META-INF/plugin.xml` holds the plugin registration and metadata.  
`build.gradle.kts` stores the Gradle build configuration.

## License

This project is provided for educational purposes. Adapt as needed for your own use.
