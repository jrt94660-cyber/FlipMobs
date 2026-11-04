# FlipMobs

Paper plugin: random mob bounties, direct and explosion kill payouts, action-bar rewards, and WorldGuard region mob-spawn control.

## Supported versions

- Paper 1.21.11
- Paper 26.3

The plugin is compiled for Java 21 bytecode. Run Paper 1.21.11 with Java 21; Paper 26.3 requires Java 25 and can run Java 21-targeted plugins.

## Requirements

Paper, Vault, WorldGuard, and a Vault-compatible economy provider.

## WorldGuard mob region

By default, the WorldGuard region named `spawn` only allows bats to spawn. Configure it in `config.yml`:

```yaml
spawn-control:
  enabled: true
  region: spawn
  override-mob-spawning-deny: true
  allowed-mobs:
    - BAT
```

Change `region` to your WorldGuard region ID and add allowed entity types to `allowed-mobs`. Any other living entity spawn inside that region is cancelled. With `override-mob-spawning-deny: true`, whitelisted mobs are allowed to spawn even when that region has WorldGuard's `mob-spawning: deny` flag. Set it to `false` to respect WorldGuard's deny flag for every mob, or set `enabled: false` to disable this rule.

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

Jar: `target/FlipMobs-1.3.2.jar`


### Managed spawning in the WorldGuard region

When `spawn-control.managed-spawning.enabled` is true, FlipMobs periodically spawns only the mob types listed in `spawn-control.allowed-mobs` inside the configured region. `interval-ticks` controls the spawn check interval, `max-mobs` sets the regional cap, and `attempts-per-cycle` controls how many spawn locations are tried per check. This allows the configured mobs to populate the region even when WorldGuard's `mob-spawning` flag is set to `deny`.
