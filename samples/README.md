# Synthetic backup payload examples

These JSON files show decrypted logical payloads for documentation. They are synthetic and are not directly importable `.steadfast` files; actual backups are authenticated encrypted envelopes described in [Security and backup](../docs/SECURITY_BACKUP.md).

- `backup_payload_example.json`: legacy schema 1, with omitted settings exercising historical defaults.
- `backup_payload_v2_example.json`: current schema 2, including all portable settings, a sample session, linked journal entry and Bible completion. `shownMilestones` is the exported comma-separated string of IDs, not a JSON array.

Current exports use app version 1.0 and schema 2/STDF_V2. Legacy examples retain their historical app-version string. Never place real journal entries, device backups, keys or passphrases in this folder.
