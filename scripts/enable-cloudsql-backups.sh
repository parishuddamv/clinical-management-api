#!/usr/bin/env bash
# Turns on daily automatic backups and point-in-time recovery for the CliniSys One database.
# Run once from Cloud Shell (or any machine with gcloud signed in as a project owner).
#
#   bash scripts/enable-cloudsql-backups.sh
#
# Then set these in Vercel (Project → Settings → Environment Variables) and redeploy the web app,
# so Settings → Data & Privacy shows the schedule to clinics:
#   REACT_APP_BACKUP_SCHEDULE="Every day at 2:00 AM IST"
#   REACT_APP_BACKUP_RETENTION_DAYS=30
set -euo pipefail

PROJECT="${PROJECT:-clinicos-prod-2026}"
INSTANCE="${INSTANCE:-clinicos-db}"
# Cloud SQL takes the start time in UTC. 20:30 UTC = 2:00 AM IST.
START_UTC="${START_UTC:-20:30}"
RETAINED_BACKUPS="${RETAINED_BACKUPS:-30}"
LOG_DAYS="${LOG_DAYS:-7}"   # point-in-time recovery window (max 7 for PostgreSQL on Enterprise)

echo "Current backup settings for $PROJECT:$INSTANCE"
gcloud sql instances describe "$INSTANCE" --project "$PROJECT" --format="yaml(settings.backupConfiguration)"

gcloud sql instances patch "$INSTANCE" --project "$PROJECT" \
  --backup-start-time="$START_UTC" \
  --retained-backups-count="$RETAINED_BACKUPS" \
  --enable-point-in-time-recovery \
  --retained-transaction-log-days="$LOG_DAYS"

echo
echo "New backup settings:"
gcloud sql instances describe "$INSTANCE" --project "$PROJECT" --format="yaml(settings.backupConfiguration)"
echo
echo "Recent backups:"
gcloud sql backups list --instance "$INSTANCE" --project "$PROJECT" --limit 5
cat <<'NOTE'

Restore drill (do this once, then every quarter):
  1. Create a scratch instance from the latest backup:
       gcloud sql backups list --instance clinicos-db --project clinicos-prod-2026
       gcloud sql instances clone clinicos-db clinicos-db-restore-test --project clinicos-prod-2026
  2. Check that patient counts match production, then delete the scratch instance:
       gcloud sql instances delete clinicos-db-restore-test --project clinicos-prod-2026
NOTE
