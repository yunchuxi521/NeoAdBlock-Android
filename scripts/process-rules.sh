#!/bin/bash
set -euo pipefail

RAW_DIR="raw"
DIST_DIR="dist/rules"
mkdir -p "$RAW_DIR" "$DIST_DIR"

# Fetch upstream EasyList sources
echo "==> Fetching EasyList China..."
curl -sL -o "$RAW_DIR/easylist_china.txt" \
    "https://easylist-downloads.adblockplus.org/easylistchina+easylist.txt"

echo "==> Fetching EasyList Global..."
curl -sL -o "$RAW_DIR/easylist_global.txt" \
    "https://easylist-downloads.adblockplus.org/easylist.txt"

# Extract and deduplicate block domains (||domain^)
echo "==> Extracting block domains..."
grep -hoP '^\|\|([a-zA-Z0-9.-]+)\^' "$RAW_DIR"/*.txt \
    | sed 's/||//;s/\^$//' \
    | sort -u \
    > "$DIST_DIR/blocklist.txt"

# Extract and deduplicate allow domains (@@||domain^)
echo "==> Extracting allow domains..."
grep -hoP '^@@\|\|([a-zA-Z0-9.-]+)\^' "$RAW_DIR"/*.txt \
    | sed 's/^@@||//;s/\^$//' \
    | sort -u \
    > "$DIST_DIR/allowlist.txt"

# Generate metadata
BLOCK_COUNT=$(wc -l < "$DIST_DIR/blocklist.txt")
ALLOW_COUNT=$(wc -l < "$DIST_DIR/allowlist.txt")

echo "{\"blockCount\":$BLOCK_COUNT,\"allowCount\":$ALLOW_COUNT,\"updated\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}" \
    > "$DIST_DIR/meta.json"

echo "==> Done: $BLOCK_COUNT block + $ALLOW_COUNT allow rules"
