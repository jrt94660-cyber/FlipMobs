# FlipMobs

Paper plugin: random mob bounties, direct and explosion kill payouts, and action-bar rewards.

Default bounty range: $100,000-$30,000,000. Bounties persist on the mob so the name display and payout stay in sync.

## Supported versions

- Paper 1.21.11
- Paper 26.3

This is a single compatibility build compiled against the Paper 1.21.11 API and targeting Java 21 bytecode. Use Java 21 to run Paper 1.21.11. Paper 26.3 requires Java 25 to run the server; the same Java 21-targeted plugin JAR can run on that newer runtime.

The plugin intentionally uses the older shared Bukkit/Paper API surface rather than compiling against 26.x-only APIs.

## Requirements

Paper, Vault, and a Vault-compatible economy provider.

## Economy

Set `economy.provider` to `VAULT`, `COINSENGINE`, or `EXCELLENTECONOMY`. For CoinsEngine and ExcellentEconomy, enable their Vault integration. `economy.symbol` changes the displayed symbol.

## Commands

- `/flipmobs reload`
- `/flipmobs info`

Permission: `flipmobs.admin` (OP by default).

## Build

Build with JDK 21 and Maven:

```bash
mvn clean package
```

Jar: `target/FlipMobs-1.1.0.jar`