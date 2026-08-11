# Zihou

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/okocraft/Zihou)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/okocraft/Zihou/maven.yml?branch=main)
![GitHub](https://img.shields.io/github/license/okocraft/Zihou)

A Velocity plugin that announces the time to the server every hour.

## Requirements

- Velocity 4.0.0 or later
- Java 21 or later

## Installation

1. Download the latest JAR from [GitHub Releases](https://github.com/okocraft/Zihou/releases).
2. Place the JAR in the Velocity proxy's `plugins` directory.
3. Restart the proxy.

Zihou creates `plugins/zihou/config.yml` on its first startup.

## Configuration

```yaml
message: <dark_gray>[<blue>時報<dark_gray>] <gray><hour>時<minute>分<second>秒になりました
timezone-id: Asia/Tokyo
```

- `message`: The announcement in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format.
- `timezone-id`: An IANA time-zone ID such as `Asia/Tokyo`. Use `default` to use the proxy host's default time zone.

The following placeholders are available in `message`:

- `<year>`
- `<month>`
- `<day>`
- `<hour>`
- `<minute>`
- `<second>`

After editing the configuration, run `/zihou reload`.

## Commands and permission

| Command | Description |
| --- | --- |
| `/zihou reload` | Reloads `config.yml`. |
| `/zihou test` | Sends the current announcement to the command source. |

Both commands require the `zihou.command` permission.

## Building

Building requires JDK 21 or later and Maven.

```shell
mvn clean package
```

The built plugin is written to `target/Zihou-1.0.jar`.

## License

This project is under the Apache License version 2.0. Please see [LICENSE](LICENSE) for more info.

Copyright © 2025-2026, OKOCRAFT and Siroshun09
