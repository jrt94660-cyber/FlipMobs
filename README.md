# FlipMobs

Paper plugin: random mob bounties, direct and explosion kill payouts, and action-bar rewards.

Default bounty range: $100,000-$30,000,000. Bounties persist on the mob so the name display and payout stay in sync.

## Requirements
Paper 1.21.11+, Java 21+, Vault and a Vault-compatible economy.

## Economy
Set `economy.provider` to `VAULT`, `COINSENGINE`, or `EXCELLENTEconomy`. For CoinsEngine and ExcellentEconomy, enable their Vault integration. `economy.symbol` changes the displayed symbol.

## Commands
- `/flipmobs reload`
- `/flipmobs info`

Permission: `flipmobs.admin` (OP by default).

Build with `mvn clean package`; jar: `target/FlipMobs-1.0.0.jar`.