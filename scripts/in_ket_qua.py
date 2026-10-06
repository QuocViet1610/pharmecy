"""In gọn phản hồi API cho scripts/demo.sh. Dùng: in_ket_qua.py <mode> <file.json>"""
import json
import sys

mode, duong_dan = sys.argv[1], sys.argv[2]
with open(duong_dan, encoding='utf-8') as f:
    d = json.load(f)


def loi(d):
    return d.get('status') is not None


if mode == 'ma-dot-kham':
    print(d[0]['ma'])

elif mode == 'import':
    print(f"tổng {d['tongDong']} dòng · hợp lệ {d['soDongHopLe']} · bị chặn {d['soDongLoi']}")
    for r in d['dongLoi']:
        print(f"  [LỖI]      dòng {r['dong']}: " + '; '.join(r['loi']))
    for r in d['dongCanhBao']:
        print(f"  [CẢNH BÁO] dòng {r['dong']}: " + '; '.join(r['canhBao']))

elif mode == 'kiem-ke':
    print(f"cho phép xuất phát: {d['choPhepXuatPhat']}")
    for t in d['dongThieu']:
        print(f"  thiếu {t['soConThieu']} {t['donVi']} {t['ten']} (phải có {t['tongPhaiCo']})")
    for c in d['canhBao']:
        print(f"  cảnh báo: {c}")

elif mode == 'bu-vat-tu':
    print(json.dumps([{'id': t['id'], 'soLuongKiemKe': t['tongPhaiCo']} for t in d['dongThieu']]))

elif mode == 'trang-thai':
    print(f"  {d['code']} — {d['detail']}" if loi(d) else f"  trạng thái: {d['trangThai']}")

elif mode == 'ghi-ket-qua':
    if loi(d):
        print(f"  bị chặn: {d['code']} — {d['detail']}")
    else:
        print(f"  {d['hoTen']}: đã khám {len(d['daKham'])} bàn · còn thiếu "
              + (', '.join(h['ten'] for h in d['conThieu']) or 'không'))

elif mode == 'ket-luan':
    if loi(d):
        print(f"  bị chặn: {d['code']} — {d['detail']}")
    else:
        print(f"  phiếu {d['soPhieu']} → {d['trangThai']} · phân loại {d['phanLoaiSucKhoe']}")

elif mode == 'bao-cao':
    print(f"  {d['tenTruong']}: {d['daKetLuan']}/{d['tongHocSinh']} đã kết luận "
          f"({d['phanTramHoanThanh']}%)")
    print("  theo lớp: " + ' | '.join(f"{l['lop']} {l['daKetLuan']}/{l['tongSo']}" for l in d['theoLop']))
    print("  tải theo bàn: " + ' | '.join(f"{t['tenHangMuc']} {t['daKham']}" for t in d['theoBan']))
    print(f"  phân loại sức khỏe: {d['phanLoaiSucKhoe']} · cần theo dõi: {d['canTheoDoiTheoHangMuc']}")
    print(f"  số HS còn thiếu hạng mục: {len(d['hocSinhConThieu'])}")

else:
    sys.exit('mode không hợp lệ: ' + mode)
