# RookiePostBox Dependency Pack

This package contains the reusable server-side plugin jars and BukkitSpring starters needed to run RookiePostBox with the tracked `config-pack`.

## Contents

- `plugins/BukkitSpring-bukkit.jar`
  - BukkitSpring runtime plugin.
- `plugins/OdalitaMenusPlugin-1.0.2.jar`
  - GUI menu runtime plugin required by RookiePostBox.
- `plugins/RookieFonts-1.2.1.jar`
  - Font/template runtime plugin required by RookiePostBox GUI titles.
- `plugins/CraftEngine-0.0.56-community.jar`
  - Resource pack/custom item runtime used by the tracked CraftEngine assets.
- `plugins/BukkitSpring/starters/*.jar`
  - BukkitSpring starter jars copied from the working test server.

## Deploy

Copy this directory's contents into the Paper server root, so the paths line up as:

```text
plugins/BukkitSpring-bukkit.jar
plugins/OdalitaMenusPlugin-1.0.2.jar
plugins/RookieFonts-1.2.1.jar
plugins/CraftEngine-0.0.56-community.jar
plugins/BukkitSpring/starters/bukkitspring-starter-caffeine-1.0.0.jar
plugins/BukkitSpring/starters/bukkitspring-starter-config-1.0.0.jar
plugins/BukkitSpring/starters/bukkitspring-starter-mybatis-1.0.0.jar
plugins/BukkitSpring/starters/bukkitspring-starter-redis-1.0.0.jar
plugins/BukkitSpring/starters/bukkitspring-starter-time-1.0.0.jar
```

Deploy `config-pack` beside this pack when preparing a clean test server.

## Excluded

This pack intentionally does not include generated or local runtime files:

- Paper server jar and remapped cache
- world data, logs, databases, and plugin cache folders
- the locally built `RookiePostBox` plugin jar
- unrelated test-server plugins such as PlayerPoints and RookieFortuneTree
