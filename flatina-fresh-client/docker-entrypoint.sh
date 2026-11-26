#!/bin/sh
# Write the runtime config from environment variables, then start nginx.
export API_URL="${API_URL:-http://localhost:8080}"
export VERSION="${VERSION:-dev}"
envsubst '${API_URL} ${VERSION}' < /usr/share/nginx/html/config.js.template > /usr/share/nginx/html/config.js
exec nginx -g 'daemon off;'
