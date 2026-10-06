#!/usr/bin/env bash
# Mở demo ra Internet qua ngrok. App phải đang chạy với profile `tunnel`:
#   JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw spring-boot:run -Dspring-boot.run.profiles=tunnel
#
# Dùng: scripts/share-ngrok.sh [--khong-mat-khau]
set -euo pipefail

cd "$(dirname "$0")/.."
CONG=8099
NGROK="${NGROK:-$HOME/bin/ngrok}"

[ -x "$NGROK" ] || { echo "Không thấy ngrok tại $NGROK"; exit 1; }

# ngrok-policy.yml chứa mật khẩu nên không commit; tạo từ file ví dụ nếu thiếu.
if [ ! -f scripts/ngrok-policy.yml ]; then
  cp scripts/ngrok-policy.example.yml scripts/ngrok-policy.yml
  echo "Đã tạo scripts/ngrok-policy.yml từ file ví dụ — đặt mật khẩu trong đó rồi chạy lại."
  exit 1
fi

if ! curl -sf "http://localhost:$CONG/api/v1/dot-kham" > /dev/null; then
  echo "App chưa chạy ở cổng $CONG. Chạy trước:"
  echo "  JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw spring-boot:run -Dspring-boot.run.profiles=tunnel"
  exit 1
fi

# Cảnh báo nếu H2 console đang mở — không được để lộ ra Internet.
if curl -sf -o /dev/null "http://localhost:$CONG/h2-console"; then
  echo "!! H2 console đang BẬT — chạy lại app với profile 'tunnel' trước khi mở tunnel."
  echo "   JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw spring-boot:run -Dspring-boot.run.profiles=tunnel"
  exit 1
fi

if [ "${1:-}" = "--khong-mat-khau" ]; then
  echo "Mở tunnel KHÔNG mật khẩu — ai có link đều vào được."
  "$NGROK" http "$CONG" &
else
  echo "Mở tunnel có Basic Auth (xem scripts/ngrok-policy.yml)."
  "$NGROK" http "$CONG" --traffic-policy-file scripts/ngrok-policy.yml &
fi
NGROK_PID=$!
trap 'kill $NGROK_PID 2>/dev/null' EXIT

# Lấy URL public từ API cục bộ của ngrok.
for _ in $(seq 1 30); do
  URL=$(curl -sf http://127.0.0.1:4040/api/tunnels 2>/dev/null \
        | python3 -c 'import json,sys;t=json.load(sys.stdin)["tunnels"];print(t[0]["public_url"] if t else "")' 2>/dev/null || true)
  [ -n "${URL:-}" ] && break
  sleep 1
done

if [ -n "${URL:-}" ]; then
  printf '\n\033[1;32mLink public: %s\033[0m\n' "$URL"
  echo "Bảng theo dõi request: http://127.0.0.1:4040"
else
  echo "Chưa lấy được URL — xem log ngrok ở trên (thường là thiếu authtoken)."
fi

echo "Ctrl+C để đóng tunnel."
wait $NGROK_PID
