# RookiePostBox Config Pack

This package was extracted from `test-server/paper-1.21.4` and keeps only the reusable plugin configuration needed by RookiePostBox.

## Contents

- `plugins/RookiePostBox/config.yml`
  - Main RookiePostBox config.
  - Uses H2 by default: `plugins/RookiePostBox/data/rookiepostbox`.
  - Uses the `letter` and `letter_detail` RookieFonts templates for GUI titles.
- `plugins/BukkitSpring/config.yml`
  - Shared starter config used by RookiePostBox.
  - Includes the `rookiepostbox-mailbox-page-state` Caffeine cache.
- `plugins/CraftEngine/resources/rookiepostbox/`
  - CraftEngine source pack for mailbox GUI icons.
  - Provides CustomModelData `21001` to `21004`.
- `plugins/RookieFonts/`
  - Mailbox GUI title templates: `letter` and `letter_detail`.
  - Includes only the generated font/resource files referenced by those templates.

## Deploy

Copy this directory's contents into the Paper server root, so the paths line up as:

```text
plugins/RookiePostBox/config.yml
plugins/BukkitSpring/config.yml
plugins/CraftEngine/resources/rookiepostbox/
plugins/RookieFonts/Template/letter.yml
plugins/RookieFonts/Template/letter_detail.yml
```

Required plugin jars are not included in this config pack:

- `RookiePostBox`
- `BukkitSpring`
- `OdalitaMenus`
- `RookieFonts`
- `CraftEngine`

After deploying CraftEngine or RookieFonts assets, restart the server or run the relevant plugin reload/build commands for the resource pack to be regenerated and sent to clients.

## Excluded From Test Server

The following test-server runtime files are intentionally not included:

- world data
- logs
- `.paper-remapped`
- plugin jars
- H2 database files under `plugins/RookiePostBox/data`
- generated CraftEngine resource pack zip
- unrelated plugin configs such as PlayerPoints, RoseGarden, spark, bStats, and RookieFortuneTree

## Notes

`plugins/CraftEngine/resources/rookiepostbox/configuration/items.yml` defines both `rookiepostbox:unopened_box` with CustomModelData `21003` and `rookiepostbox:ready_box` with CustomModelData `21004`.

The current RookiePostBox config has one `rookiepostbox.menu.mail-item.custom-model-data` value, currently `21004`, so the same mailbox item model is used for both read and unread mail until the plugin supports separate model settings.
