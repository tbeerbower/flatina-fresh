#!/usr/bin/env bash
# Provisions a Google Cloud project for Flatina Fresh and wires GitHub Actions to it.
# Re-runnable: every step checks before it creates. Answers are saved to deploy-config.env
# (git-ignored, it holds secrets).
#
# Needs: gcloud (logged in as a project owner) and a billing account.
# Optional: gh (logged in) to set the GitHub environment variables and secrets for you.
set -euo pipefail

cd "$(dirname "$0")"
CONFIG_FILE="./deploy-config.env"
GITHUB_REPO_DEFAULT="tbeerbower/flatina-fresh"

command -v gcloud >/dev/null || { echo "gcloud CLI is required: https://cloud.google.com/sdk/docs/install"; exit 1; }

# Google Cloud APIs sometimes answer 429 (rate limit), counted against your gcloud quota project. Every gcloud
# call below goes through this wrapper, which waits and retries on rate limits and fails on anything else.
gcloud() {
  local attempt=1 delay=10 err rc
  err="$(mktemp)"
  while :; do
    if command gcloud "$@" 2>"$err"; then
      grep -vE '^(Updates are available|To take a quick anonymous survey|  \$ gcloud|please run:)' "$err" >&2 || true
      rm -f "$err"
      return 0
    else
      rc=$?
    fi
    if grep -qE 'RESOURCE_EXHAUSTED|RATE_LIMIT_EXCEEDED|rateLimitExceeded|Quota exceeded|status.: 429' "$err" \
        && [ "$attempt" -lt 6 ]; then
      echo "  Google Cloud rate limit hit (gcloud $1 $2); retrying in ${delay}s..." >&2
      sleep "$delay"
      delay=$((delay * 2))
      attempt=$((attempt + 1))
      continue
    fi
    cat "$err" >&2
    rm -f "$err"
    return "$rc"
  done
}

# shellcheck disable=SC1090
[ -f "$CONFIG_FILE" ] && source "$CONFIG_FILE"

ask() { # ask VAR "Prompt" default
  local var="$1" prompt="$2" default="${!1:-$3}" input
  read -r -p "$prompt [$default]: " input
  printf -v "$var" '%s' "${input:-$default}"
}
random_secret() { openssl rand -base64 36 | tr -d '/+=' | cut -c1-40; }

ask PROJECT_ID "GCP project ID" "flatina-fresh-$RANDOM"
ask REGION "Region" "us-central1"
ask ENVIRONMENT "GitHub environment" "development"
ask GITHUB_REPO "GitHub repository" "$GITHUB_REPO_DEFAULT"
ask SQL_INSTANCE "Cloud SQL instance name" "flatina-db"
ask ADMIN_USERNAME "App admin username" "tom"
DB_NAME="${DB_NAME:-flatina}"
DB_USERNAME="${DB_USERNAME:-flatina}"
DB_PASSWORD="${DB_PASSWORD:-$(random_secret)}"
JWT_SECRET="${JWT_SECRET:-$(random_secret)}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-$(random_secret | cut -c1-16)}"
REPO="flatina-repo"

save_config() {
  umask 077
  cat > "$CONFIG_FILE" <<CFG
PROJECT_ID=$PROJECT_ID
REGION=$REGION
ENVIRONMENT=$ENVIRONMENT
GITHUB_REPO=$GITHUB_REPO
SQL_INSTANCE=$SQL_INSTANCE
ADMIN_USERNAME=$ADMIN_USERNAME
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
ADMIN_PASSWORD=$ADMIN_PASSWORD
BILLING_ACCOUNT=${BILLING_ACCOUNT:-}
CFG
}
save_config

echo "== Project"
if ! gcloud projects describe "$PROJECT_ID" >/dev/null 2>&1; then
  gcloud projects create "$PROJECT_ID" --name="Flatina Fresh"
fi
gcloud config set project "$PROJECT_ID" >/dev/null
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"

billing_enabled() {
  local out
  if ! out="$(gcloud billing projects describe "$PROJECT_ID" --format='value(billingEnabled)')"; then
    echo "Couldn't check billing for $PROJECT_ID (see the error above). Fix that and re-run this script." >&2
    exit 1
  fi
  [ "$out" = "True" ]
}

if billing_enabled; then
  echo "Billing is enabled."
else
  echo "Billing isn't linked to $PROJECT_ID yet. Your open billing accounts:"
  ACCOUNTS="$(gcloud billing accounts list --filter='open=true' --format='value(name.basename(),displayName)')"
  if [ -z "$ACCOUNTS" ]; then
    echo "  (none found) Create one at https://console.cloud.google.com/billing, then re-run."
    exit 1
  fi
  echo "$ACCOUNTS" | sed 's/^/  /'
  BILLING_ACCOUNT="${BILLING_ACCOUNT:-$(echo "$ACCOUNTS" | head -n1 | cut -f1)}"
  ask BILLING_ACCOUNT "Billing account to link" "$BILLING_ACCOUNT"
  save_config
  gcloud billing projects link "$PROJECT_ID" --billing-account="$BILLING_ACCOUNT" >/dev/null
  billing_enabled || { echo "Billing still isn't enabled for $PROJECT_ID."; exit 1; }
  echo "Billing linked."
fi

echo "== APIs"
gcloud services enable run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com cloudresourcemanager.googleapis.com

echo "== Artifact Registry"
gcloud artifacts repositories describe "$REPO" --location="$REGION" >/dev/null 2>&1 || \
  gcloud artifacts repositories create "$REPO" --repository-format=docker --location="$REGION" \
    --description="Flatina Fresh images"

echo "== Cloud SQL (this takes several minutes the first time)"
if ! gcloud sql instances describe "$SQL_INSTANCE" >/dev/null 2>&1; then
  gcloud sql instances create "$SQL_INSTANCE" --database-version=POSTGRES_16 --edition=ENTERPRISE \
    --tier=db-f1-micro --region="$REGION" --storage-size=10 --storage-auto-increase
fi
gcloud sql databases describe "$DB_NAME" --instance="$SQL_INSTANCE" >/dev/null 2>&1 || \
  gcloud sql databases create "$DB_NAME" --instance="$SQL_INSTANCE"
if gcloud sql users list --instance="$SQL_INSTANCE" --format='value(name)' | grep -qx "$DB_USERNAME"; then
  gcloud sql users set-password "$DB_USERNAME" --instance="$SQL_INSTANCE" --password="$DB_PASSWORD"
else
  gcloud sql users create "$DB_USERNAME" --instance="$SQL_INSTANCE" --password="$DB_PASSWORD"
fi
SQL_CONNECTION_NAME="$(gcloud sql instances describe "$SQL_INSTANCE" --format='value(connectionName)')"

echo "== Service accounts"
ensure_sa() {
  gcloud iam service-accounts describe "$1@$PROJECT_ID.iam.gserviceaccount.com" >/dev/null 2>&1 || \
    gcloud iam service-accounts create "$1" --display-name="$2"
}
bind() {
  gcloud projects add-iam-policy-binding "$PROJECT_ID" --member="serviceAccount:$1" --role="$2" \
    --condition=None --quiet >/dev/null
}
ensure_sa flatina-runtime "Flatina Fresh runtime"
ensure_sa github-deployer "GitHub Actions deployer"
RUNTIME_SA="flatina-runtime@$PROJECT_ID.iam.gserviceaccount.com"
DEPLOYER_SA="github-deployer@$PROJECT_ID.iam.gserviceaccount.com"
bind "$RUNTIME_SA" roles/cloudsql.client
for role in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser; do
  bind "$DEPLOYER_SA" "$role"
done

echo "== Workload Identity Federation for $GITHUB_REPO"
gcloud iam workload-identity-pools describe github-pool --location=global >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools create github-pool --location=global --display-name="GitHub"
gcloud iam workload-identity-pools providers describe github-provider --location=global \
  --workload-identity-pool=github-pool >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools providers create-oidc github-provider --location=global \
    --workload-identity-pool=github-pool --display-name="GitHub provider" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
    --attribute-condition="assertion.repository == '$GITHUB_REPO'"
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER_SA" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/attribute.repository/$GITHUB_REPO" \
  --quiet >/dev/null

echo "== GitHub environment '$ENVIRONMENT'"
RUNTIME_SERVICE_ACCOUNT="$RUNTIME_SA"
VAR_NAMES="PROJECT_ID PROJECT_NUMBER REGION SQL_CONNECTION_NAME DB_NAME DB_USERNAME RUNTIME_SERVICE_ACCOUNT ADMIN_USERNAME"
SECRET_NAMES="DB_PASSWORD JWT_SECRET ADMIN_PASSWORD"

if command -v gh >/dev/null && gh auth status >/dev/null 2>&1; then
  gh api -X PUT "repos/$GITHUB_REPO/environments/$ENVIRONMENT" >/dev/null
  for k in $VAR_NAMES; do gh variable set "$k" --repo "$GITHUB_REPO" --env "$ENVIRONMENT" --body "${!k}"; done
  for k in $SECRET_NAMES; do gh secret set "$k" --repo "$GITHUB_REPO" --env "$ENVIRONMENT" --body "${!k}"; done
  echo "GitHub variables and secrets are set."
else
  echo "gh isn't available; set these on the '$ENVIRONMENT' environment in GitHub (Settings > Environments):"
  echo "Variables:"; for k in $VAR_NAMES; do echo "  $k=${!k}"; done
  echo "Secrets: DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD (values are in gcp-deployment/deploy-config.env)"
fi

echo
echo "Done. Card scanning also needs an ANTHROPIC_API_KEY secret on the environment (optional)."
echo "Your first login is '$ADMIN_USERNAME' with the ADMIN_PASSWORD in gcp-deployment/deploy-config.env."
echo "Push to main (or run the 'Build and Deploy' workflow) to deploy."
