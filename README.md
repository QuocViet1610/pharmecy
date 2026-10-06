# MediLink Checkup

Phần mềm quản lý **một đợt khám sức khỏe định kỳ tại trường học**, từ lúc nhà trường bàn giao danh
sách đến lúc thu phiếu và xuất báo cáo.

Hiện thực theo tài liệu nghiệp vụ *"Quy trình khám sức khỏe định kỳ tại trường học"*. Điểm khác biệt
so với một "phiếu khám điện tử" thông thường: xương sống dữ liệu là **Đợt khám**, không phải tờ phiếu.

```
Đợt khám → Trường → Khối/Lớp → Học sinh → Gói/hạng mục khám → Bàn khám
         → Nhân sự/Vật tư/Thiết bị → Kết quả từng bàn → Tiến độ → Kết luận → Báo cáo
```

Nguyên tắc thiết kế lấy nguyên từ tài liệu: **lỗi phải được phát hiện tại nơi và thời điểm còn sửa được.**

| | |
|---|---|
| Stack | Java 21 · Spring Boot 4.0.1 · Spring Data JPA · H2 |
| Giao diện | HTML + CSS + vanilla JS, **không build step**, không framework |
| Kiểm thử | 14 test nghiệp vụ, chạy bằng H2 in-memory, không cần Docker |
| Đóng gói | Dockerfile 2 stage, non-root, ~399 MB · CI đẩy image lên GHCR |

---

## Chạy nhanh

Cần **JDK 21** (hoặc chỉ cần Docker nếu dùng cách 2). Maven không cần cài sẵn — đã có wrapper.

### Cách 1 — chạy trực tiếp

```bash
git clone https://github.com/QuocViet1610/pharmecy.git
cd pharmecy
./mvnw spring-boot:run
```

Mở **http://localhost:8099**. Dữ liệu mẫu được nạp sẵn: 1 trường, 3 lớp, 75 học sinh, 1 đợt khám
đã chuẩn bị xong.

> **Máy có nhiều JDK / mặc định không phải 21?** Chỉ định rõ:
> ```bash
> JAVA_HOME=/duong/dan/den/jdk-21 ./mvnw spring-boot:run
> ```
> Trên máy dev của tác giả: `JAVA_HOME=~/.jdks/ms-21.0.11`

### Cách 2 — Docker

```bash
docker compose up -d --build     # http://localhost:8099
docker compose logs -f app
docker compose down              # dừng, giữ dữ liệu
docker compose down -v           # dừng và xoá dữ liệu, lần sau seed lại từ đầu
PORT=9000 docker compose up -d   # đổi cổng host
```

Chạy Docker thì dữ liệu **sống qua restart** (H2 lưu ra file trên volume `/data`).

### Cách 3 — image có sẵn trên GHCR

Mỗi lần push lên `main`, CI chạy 14 test rồi build và đẩy image lên GitHub Container Registry.
Sau khi workflow đầu tiên chạy xong và package được **đổi sang public**
(Settings của package trên GitHub → Change visibility), người khác chỉ cần:

```bash
docker run -d -p 8099:8099 -v medilink-data:/data ghcr.io/quocviet1610/pharmecy:latest
```

Package còn private thì phải `docker login ghcr.io` bằng Personal Access Token có quyền
`read:packages` trước khi pull.

### Chạy test

```bash
./mvnw test              # 14 test, ~7 giây
scripts/demo.sh          # chạy xuyên suốt 1 đợt khám qua API (app phải đang chạy)
```

---

## Đi một vòng demo

Dữ liệu mẫu **cố ý để thiếu 2 dòng vật tư**, để thấy ngay chốt chặn "chưa đủ thì không được xuất phát".
Thứ tự nên thử:

**① Tab 2 — Danh sách học sinh.** Bấm *Nạp file mẫu có lỗi* → *Kiểm tra*. Hệ thống chặn 6 dòng
(thiếu họ tên, ngày `31/02/2014` không tồn tại, trùng mã trong file, trùng mã đã có trong hệ thống,
giới tính sai, thiếu lớp) và cảnh báo 2 dòng khác mà vẫn cho nhập. Chế độ *Kiểm tra* **không ghi
vào DB** — mục đích là sửa với nhà trường trước ngày khám.

**② Tab 3 — Chuẩn bị & kiểm kê.** Hai dòng tô đỏ: *Máy đo huyết áp* (cần 2, có 1) và *Que đè lưỡi*
(cần 95, có 72). Điền cột **Kiểm kê** cho đủ → *Lưu checklist* → *Kiểm kê trước khi xuất phát*.

**③ Tab 1 — Đợt khám.** *Xuất phát* → *Bắt đầu khám*. Thử bấm *Xuất phát* khi còn thiếu: bị chặn
kèm đúng danh sách còn thiếu.

**④ Tab 4 — Bàn khám.** Chọn một bàn trực → gõ `duc long` (không cần dấu) hoặc `nguyen` → Enter.
Ghi kết quả bằng nút *Bình thường*. Cứ đổi bàn theo thứ tự tùy ý — đây là nguyên tắc **khám tự do**.

**⑤ Tab 5 — Bàn kết luận.** Tra một em chưa khám đủ: bị chặn kèm danh sách bàn phải quay lại.
Khám đủ 5 bàn rồi thì nút *Kết luận* mới bật, và hệ thống đề xuất phân loại sức khỏe I–V.

**⑥ Tab 6 — Theo dõi & báo cáo.** Tiến độ theo lớp và theo bàn, danh sách học sinh còn thiếu hạng mục,
nút tải CSV.

---

## Nguyên tắc nghiệp vụ

**Khám tự do giữa các bàn.** Phần mềm **không** áp thứ tự khám. Học sinh vào bàn nào trước cũng được;
thứ tự chỉ là thời điểm của từng bản ghi kết quả. Điều kiện duy nhất là **đủ hạng mục bắt buộc**
trước khi tới bàn kết luận.

**Phiếu đi cùng học sinh.** `PhieuKham` là bản số của tờ phiếu giấy: số phiếu in trên giấy, encode
vào QR, và nhận diện tại bàn bằng số phiếu, mã định danh **hoặc tên** — bác sĩ không phải tìm tờ giấy.

### 7 điểm đau → 7 chốt kiểm soát

| # | Công đoạn | Điểm đau | Chốt trong hệ thống |
|---|---|---|---|
| 1 | Nhận danh sách | Sai, thiếu, trùng thông tin | `ImportHocSinhService` — chế độ `KIEM_TRA` soi lỗi mà không ghi DB; tách **lỗi** (chặn) và **cảnh báo** (vẫn nhập được) |
| 2 | Chuẩn bị | Khó kiểm soát số lượng vật tư | `MauChecklist` — định mức/100 học sinh, số cần tính từ danh sách thực tế + dự phòng |
| 3 | Trước xuất phát | Đến trường mới biết thiếu đồ | `POST /xuat-phat` trả **409 `THIEU_VAT_TU`** kèm đúng danh sách còn thiếu |
| 4 | Nhận diện học sinh | Nhầm hoặc mất phiếu | Tìm bằng mã định danh, số phiếu/QR, hoặc tên **không cần gõ dấu** |
| 5 | Khám tự do | Không biết đã khám bàn nào | Tiến độ từng phiếu theo thời gian thực; ghi lại cùng hạng mục là **sửa**, không tạo dòng mới |
| 6 | Trước kết luận | Học sinh bỏ sót hạng mục | `KetLuanService` trả **409 `THIEU_HANG_MUC`** kèm danh sách bàn phải quay lại |
| 7 | Sau khám | Khó tổng hợp hàng nghìn phiếu | `BaoCaoService` — tiến độ, phân loại sức khỏe, xuất CSV; **409 `CON_PHIEU_CHUA_KET_LUAN`** khi đóng đợt sớm |

### Vòng đời đợt khám

```
NHAP ──chuẩn bị (sinh phiếu, dựng bàn, tính checklist)──▶ DANG_CHUAN_BI
     ──kiểm kê đủ──▶ SAN_SANG ──▶ DANG_KHAM ──mọi phiếu đã kết luận──▶ HOAN_THANH
```

Mỗi mũi tên là một chốt. Chuyển sai trạng thái trả `409` kèm `code` nghiệp vụ, không im lặng bỏ qua.

---

## Giao diện

6 tab theo đúng trình tự nghiệp vụ:

| Tab | Vai trò thực tế | Nội dung |
|---|---|---|
| 1. Đợt khám | Điều phối viên | Vòng đời đợt khám, bàn khám đã setup, gói khám |
| 2. Danh sách học sinh | Cán bộ nhận danh sách | Dán CSV, soi lỗi trước ngày khám, lưu dòng hợp lệ |
| 3. Chuẩn bị & kiểm kê | Người chuẩn bị vật tư | Checklist nhân sự/vật tư/thiết bị, kiểm kê trước xuất phát |
| 4. Bàn khám | Bác sĩ tại bàn | Chọn bàn trực 1 lần, tìm học sinh, ghi kết quả 1 click |
| 5. Bàn kết luận | Người phụ trách kết luận | Chặn thiếu hạng mục, phân loại sức khỏe |
| 6. Theo dõi & báo cáo | Trưởng đoàn | Tiến độ theo lớp/bàn, danh sách còn thiếu, xuất CSV |

### Tab 4 được tối ưu riêng

Một đợt 75 học sinh × 5 bàn = **~375 lượt nhập**, giữa sân trường, học sinh xếp hàng chờ. Mỗi động tác
dư ở đây bị nhân lên 375 lần, nên luồng gói lại thành: **quét/gõ → Enter → một nút → tự sang em kế tiếp.**

- **Chọn bàn trực một lần cho cả buổi**, nhớ trong `localStorage`. Màn hình sau đó chỉ phục vụ bàn đó
  nên không thể ghi nhầm hạng mục. Thanh màu đậm trên cùng luôn hiển thị đang trực bàn nào.
- **Tìm không cần gõ dấu**: `duc long` → `Nguyễn Đức Long`. Em nào quên mã hoặc mất phiếu không còn
  làm nghẽn hàng chờ. Nhiều kết quả thì bấm `1..9` để chọn. Lọc thêm theo lớp.
- **Một nút lớn "✓ Bình thường — ghi & sang HS tiếp"** cho phần lớn ca. Ca cần theo dõi / bất thường
  mới mở ô mô tả, và *bắt buộc* mô tả khi chọn bất thường — bàn kết luận cần thông tin đó.
- **Cảnh báo khi ghi lại**: bàn đã ghi cho em này thì hiện giờ ghi, kết luận cũ, điền sẵn giá trị cũ
  và nói rõ "ghi tiếp là sửa kết quả" — không ghi đè âm thầm.
- **Nhật ký 8 lượt vừa ghi** để bác sĩ tự đối chiếu khi bị ngắt giữa buổi.
- **Phím tắt**: `Enter` tìm / ghi bình thường · `Alt+1/2/3` ba mức kết luận · `Esc` sang em mới.
- **Chạy tốt cả tablet và laptop**: vùng chạm ≥44px, `inputmode` số cho chỉ số số, một cột khi màn hẹp,
  bỏ hiệu ứng hover trên thiết bị cảm ứng.

Form chỉ số **do backend mô tả**, UI không hardcode: `GET /api/v1/dot-kham/hang-muc` trả nhãn, kiểu
(`so`/`text`) và giá trị ví dụ cho từng chỉ số. Thêm hạng mục khám mới chỉ cần sửa enum `HangMuc`.

---

## API

Lỗi nghiệp vụ trả `ProblemDetail` + `code` + dữ liệu để người dùng sửa ngay (`conThieu`, `dongThieu`,
`loiTheoTruong`…).

```
# Trường và học sinh
GET    /api/v1/truong                             danh sách trường + số học sinh + các lớp
GET    /api/v1/truong/{id}/hoc-sinh
POST   /api/v1/import/hoc-sinh?truongId=&cheDo=KIEM_TRA|LUU
                                                  body: CSV (text/plain) hoặc multipart

# Đợt khám
GET    /api/v1/dot-kham                           | POST /api/v1/dot-kham
GET    /api/v1/dot-kham/{ma}
GET    /api/v1/dot-kham/hang-muc                  hạng mục + chỉ số (UI dựng form từ đây)
POST   /api/v1/dot-kham/{ma}/chuan-bi             sinh phiếu, dựng bàn, tính checklist
GET    /api/v1/dot-kham/{ma}/ban-kham
GET    /api/v1/dot-kham/{ma}/checklist            | PUT  cập nhật số đã chuẩn bị / số kiểm kê
GET    /api/v1/dot-kham/{ma}/kiem-ke
POST   /api/v1/dot-kham/{ma}/xuat-phat            | /bat-dau-kham | /hoan-thanh

# Tại khu khám
GET    /api/v1/dot-kham/{ma}/tra-cuu?q=           khớp chính xác mã định danh hoặc số phiếu
GET    /api/v1/dot-kham/{ma}/tim?q=&lop=&gioiHan= tìm theo mã, số phiếu hoặc TÊN (không cần dấu)
POST   /api/v1/dot-kham/{ma}/ket-qua              ghi kết quả một bàn (thứ tự bất kỳ)
GET    /api/v1/dot-kham/{ma}/tien-do?lop=&conThieu=
POST   /api/v1/dot-kham/{ma}/ket-luan

# Báo cáo
GET    /api/v1/bao-cao/{ma}                       | /api/v1/bao-cao/{ma}/csv
GET    /actuator/health                           dùng cho Docker healthcheck
```

### Định dạng CSV nhập học sinh

```csv
ma_dinh_danh,ho_ten,ngay_sinh,gioi_tinh,lop,khoi,ho_ten_phu_huynh,dien_thoai_phu_huynh
HS09001,Nguyễn Văn Khoa,12/05/2014,Nam,8A1,8,Nguyễn Văn Bình,0912000001
```

Ngày `dd/MM/yyyy`. Giới tính `Nam` / `Nữ` / `Khác`. Hai cột cuối không bắt buộc. Khối trống thì suy
ra từ tên lớp. File mẫu có lỗi sẵn: `src/main/resources/data/hoc-sinh-mau-co-loi.csv`.

---

## Cấu trúc

```
src/main/java/com/medilink/checkup/
  domain/   DotKham · HocSinh · PhieuKham · BanKham · KetQuaKham · TaiNguyenDotKham · ChiSo + enum
  repo/     Spring Data JPA
  service/  ImportHocSinhService · DotKhamService · KhamService · KetLuanService · BaoCaoService
            MauChecklist (định mức) · PhanLoaiDeXuat (gợi ý I–V) · ChuanHoaText (bỏ dấu để tìm)
  web/      controller + dto (record) + ApiException + GlobalExceptionHandler
  config/   DuLieuMau (seed dữ liệu demo)
src/main/resources/
  static/   index.html · app.js · styles.css   (UI, không build step)
  data/     CSV mẫu
  application.yml + application-tunnel.yml + application-docker.yml
scripts/    demo.sh (smoke test API) · share-ngrok.sh (mở ra Internet)
```

Ba profile:

| Profile | Dùng khi | H2 | H2 console |
|---|---|---|---|
| (mặc định) | dev trên máy | in-memory, reset mỗi lần chạy | **bật** tại `/h2-console` |
| `tunnel` | mở ra Internet qua ngrok | in-memory | tắt |
| `docker` | chạy trong container | file trên volume `/data` | tắt |

---

## Mở ra Internet (demo cho người khác xem)

```bash
# 1. cài authtoken ngrok (một lần)
ngrok config add-authtoken <TOKEN>
# 2. đặt mật khẩu Basic Auth
cp scripts/ngrok-policy.example.yml scripts/ngrok-policy.yml   # rồi sửa mật khẩu trong đó
# 3. chạy app với profile tắt H2 console
./mvnw spring-boot:run -Dspring-boot.run.profiles=tunnel
# 4. mở tunnel (terminal khác)
scripts/share-ngrok.sh
```

`scripts/ngrok-policy.yml` chứa mật khẩu nên **đã được `.gitignore`**; bản commit lên git là
`ngrok-policy.example.yml`. Script tự chối mở tunnel nếu H2 console còn bật.

> **Bắt buộc dùng profile `tunnel` hoặc `docker` khi mở ra ngoài.** Cấu hình mặc định bật H2 console
> với user `sa` không mật khẩu — để lộ ra Internet là cho người lạ chạy SQL tùy ý.

---

## Giới hạn

Đây là demo nghiệp vụ, **chưa phải bản chạy thật**:

- **Chưa có xác thực/phân quyền.** Ai vào được là dùng được mọi API, kể cả ghi kết quả và kết luận
  phiếu. Thật thì mỗi bàn khám phải là một người dùng có quyền riêng.
- **H2.** Lên thật cần MySQL/PostgreSQL + migration (Flyway/Liquibase).
- **Import chỉ nhận CSV.** Tài liệu nói nhà trường gửi Excel → cần thêm Apache POI cho `.xlsx`.
- **Chưa in phiếu/QR thật** (số phiếu đã sẵn sàng để encode).
- **Một hạng mục mặc định một bàn.** Trường đông cần nhiều bàn cùng hạng mục để giảm thời gian chờ.
- **Phân loại sức khỏe I–V** hiện là quy tắc đơn giản từ kết luận các bàn, chưa theo thông tư chuẩn
  (chưa dùng BMI theo tuổi/giới). Người kết luận vẫn là người quyết định cuối.
- **Mất mạng giữa buổi** làm mọi thao tác đứng. Wifi sân trường hay yếu — xử lý đúng cần lưu tạm
  offline rồi đồng bộ lại.
