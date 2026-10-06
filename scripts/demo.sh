#!/usr/bin/env bash
# Chạy xuyên suốt một đợt khám qua API, theo đúng các giai đoạn trong tài liệu nghiệp vụ.
# Dùng: scripts/demo.sh [base-url]   — app phải đang chạy (mvnw spring-boot:run)
set -euo pipefail

cd "$(dirname "$0")/.."
B="${1:-http://localhost:8099}/api/v1"
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT

show() { python3 -I scripts/in_ket_qua.py "$1" "$T/r.json"; }
buoc() { printf '\n\033[1;36m== %s\033[0m\n' "$1"; }

curl -s "$B/dot-kham" -o "$T/r.json"
DOT=$(show ma-dot-kham)
echo "Đợt khám: $DOT"

buoc "Giai đoạn 1 — nhận danh sách nhà trường (chế độ kiểm tra, chưa ghi DB)"
curl -s -X POST "$B/import/hoc-sinh?truongId=1&cheDo=KIEM_TRA" -H 'Content-Type: text/plain' \
  --data-binary @src/main/resources/data/hoc-sinh-mau-co-loi.csv -o "$T/r.json"
show import

buoc "Giai đoạn 3 — kiểm kê trước khi xuất phát"
curl -s "$B/dot-kham/$DOT/kiem-ke" -o "$T/r.json"; show kiem-ke
echo "-- thử xuất phát khi còn thiếu:"
curl -s -X POST "$B/dot-kham/$DOT/xuat-phat" -o "$T/r.json"; show trang-thai

buoc "Bổ sung cho đủ rồi xuất phát, setup khu khám"
curl -s "$B/dot-kham/$DOT/kiem-ke" -o "$T/r.json"; show bu-vat-tu > "$T/fix.json"
curl -s -X PUT "$B/dot-kham/$DOT/checklist" -H 'Content-Type: application/json' -d @"$T/fix.json" -o /dev/null
curl -s -X POST "$B/dot-kham/$DOT/xuat-phat" -o "$T/r.json"; show trang-thai
curl -s -X POST "$B/dot-kham/$DOT/bat-dau-kham" -o "$T/r.json"; show trang-thai

ghi() { # $1 mã nhận diện, $2 hạng mục, $3 kết luận chuyên môn
  curl -s -X POST "$B/dot-kham/$DOT/ket-qua" -H 'Content-Type: application/json' \
    -d "{\"maNhanDien\":\"$1\",\"hangMuc\":\"$2\",\"bacSi\":\"BS. Demo\",\"ketLuanChuyenMon\":\"$3\"}" \
    -o "$T/r.json"
  show ghi-ket-qua
}

buoc "Giai đoạn 4 — khám TỰ DO: hai học sinh, hai thứ tự khác nhau"
echo "HS00001: Mắt → Nội/Nhi → RHM → TMH → Thể lực"
for hm in MAT NOI_NHI RHM TMH THE_LUC; do ghi HS00001 "$hm" BINH_THUONG; done
echo "HS00002: TMH → Thể lực → Mắt (cố ý bỏ 2 bàn)"
for hm in TMH THE_LUC MAT; do ghi HS00002 "$hm" CAN_THEO_DOI; done

buoc "Giai đoạn 5 — bàn kết luận chặn học sinh còn thiếu hạng mục"
curl -s -X POST "$B/dot-kham/$DOT/ket-luan" -H 'Content-Type: application/json' \
  -d '{"maNhanDien":"HS00002","nguoiKetLuan":"BS. Trưởng đoàn"}' -o "$T/r.json"; show ket-luan
echo "-- HS00001 đã đủ hạng mục:"
curl -s -X POST "$B/dot-kham/$DOT/ket-luan" -H 'Content-Type: application/json' \
  -d '{"maNhanDien":"HS00001","ketLuan":"Sức khỏe bình thường","nguoiKetLuan":"BS. Trưởng đoàn"}' \
  -o "$T/r.json"; show ket-luan

buoc "Không đóng được đợt khi còn phiếu chưa kết luận"
curl -s -X POST "$B/dot-kham/$DOT/hoan-thanh" -o "$T/r.json"; show trang-thai

buoc "Báo cáo đợt khám"
curl -s "$B/bao-cao/$DOT" -o "$T/r.json"; show bao-cao
printf '\nCSV kết quả: curl -s %s/bao-cao/%s/csv\n' "$B" "$DOT"
