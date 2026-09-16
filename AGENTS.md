# AGENTS.md

Minecraft mod "Immersive Vehicles" (formerly MTS). Gradle multi-loader build: one version-independent core plus per-Minecraft-version adapters. No tests or linters exist; compiling is the only verification.

## Build / verify

- Run `.\gradlew.bat <task>` from the repo root (Gradle 7.5.1 wrapper; daemon disabled in `gradle.properties`).
- Build one MC version: `buildForge1122`, `buildForge1165`, `buildForge1182`, `buildForge1192`, `buildForge1201`; `buildForgeAll` builds all of them. `buildCore` builds `mccore` alone.
- These tasks **move** the finished jar out of `<module>/build/libs` into `out/` (gitignored), named `Immersive Vehicles-<mc_version>-<mod_version>.jar`.
- Focused compile check: `.\gradlew.bat :mcinterfaceforge1201:compileJava` or `:mccore:build`. First build downloads/decompiles Minecraft and is slow.
- Run the game per module: `.\gradlew.bat :mcinterfaceforge1201:runClient` / `runServer` (game dirs `run`/`runServer` inside the module).
- `buildForge1211` (NeoForge 1.21.1) is special: it shells out to the separate Gradle 8.8 wrapper in `gradle/neoforge-wrapper/` and builds `mcinterfaceneoforge1211`, which is **not** in the root `settings.gradle.kts`. It needs JDK 21; don't build that module with the root wrapper.
- `buildForge262` (NeoForge 26.2) works the same way via `gradle/neoforge-wrapper-262/` (Gradle 9.2.1, Java 25 toolchain) and additionally `dependsOn :mccore:shadowJar`. `mcinterfaceneoforge262` cannot include `:mccore` as a project because mccore's shadow plugin 7.1.2 crashes on Gradle 9 (`convention` removed); it embeds the prebuilt `mccore/build/libs/Immersive Vehicles-Core-relocated.jar`. Build that jar with the root or 1211 wrapper before compiling the 262 module standalone.
- Compile-check the 262 module directly: `java -classpath gradle/neoforge-wrapper-262/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon -p mcinterfaceneoforge262 compileJava`, with `CI=true` to skip Minecraft decompilation. Its decompiled 26.2 sources are generated under `mcinterfaceneoforge262/build/moddev/artifacts/minecraft-patched-26.2.0.88-sources.jar` when built without `CI`.

## Versioning gotcha

- Root `gradle.properties` `global_version` is the only source of truth. Every `buildForge*` task runs `preBuild()`, which walks the whole repo and rewrites `mod_version=` in `gradle.properties`, `"version":` in `mcmod.info`, and `MODVER =` in `InterfaceLoader.java`. Don't hand-edit those values; expect builds to modify tracked files.

## Architecture

- `mccore`: version-agnostic game logic, data models (`jsondefs`, `packloading`, `systems`, ...) and shared assets (`src/main/resources/assets/mts`). Compiled with `--release 8` (as are 1122/1165) — no Java 9+ APIs or language features in shared code.
- `mcinterfaceforge<ver>` / `mcinterfaceneoforge1211` / `mcinterfaceneoforge262`: loader + MC-version adapters (packages `mcinterface1122` ... `mcinterface262`). Each implements all `minecrafttransportsimulator.mcinterface.IInterface*` (Core/Packet/Client/Input/Sound/Render) and wires them in `InterfaceLoader` via `InterfaceManager`. Changing an interface requires implementing it in all seven modules.
- 1.18.2+ modules embed mccore's `shadowJar` through the `relocated` configuration (javazoom relocation avoids conflicts with other mods). NeoForge additionally excludes `com/jcraft/**` from its fat jar because the game provides jorbis on the JPMS module path.
- 26.2 changed several cross-cutting APIs versus 1.21.1: `ResourceLocation`→`Identifier`, `ResourceKey.location()`→`identifier()`, `Capabilities.{FluidHandler,ItemHandler,EnergyStorage}`→`Capabilities.{Fluid,Item,Energy}` backed by the transactional transfer API (`ResourceHandler`/`EnergyHandler`), `CompoundTag` getters return `Optional` (use `getXOr`), entity/TE save-load uses `ValueInput`/`ValueOutput` (MTS data is nested under the `"mts"` key), `InteractionResultHolder`/`UseAnim`/`GuiGraphics` are gone, and the whole rendering stack (`RenderType`/`RenderStateShard`/`VertexBuffer`/`MultiBufferSource`/`ShaderInstance`) was replaced by the `RenderSetup`/`RenderPipeline`/`SubmitNodeCollector` system.

## Toolchain / CI

- JDK 17 for the root wrapper and modern modules; JDK 21 for the NeoForge module. `README.md` is stale (documents only 1.12.2/1.16.5 and JDK 8) — trust `build.gradle.kts` and the module `gradle.properties`.
- `.github/workflows/build.yml`: on master pushes and PRs, builds Forge modules only (not 1.21.1) with JDK 17; on tags it creates a GitHub release from `out/` jars.
