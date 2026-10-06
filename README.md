# MediLink Checkup — demo quản lý đợt khám sức khỏe học đường

Demo hiện thực nghiệp vụ trong tài liệu *"Quy trình khám sức khỏe định kỳ tại trường học"*.
Xương sống dữ liệu là **Đợt khám**, không phải một phiếu khám điện tử rời:

```
Đợt khám → Trường → Khối/Lớp → Học sinh → Gói/hạng mục khám → Bàn khám
         → Nhân sự/Vật tư/Thiết bị → Kết quả từng bàn → Tiến độ → Kết luận → Báo cáo
```

Nguyên tắc thiết kế lấy nguyên từ tài liệu: **lỗi phải được phát hiện tại nơi và thời điểm còn sửa được.**

## Chạy

```bash
JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw spring-boot:run     # http://localhost:8099
JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw test                # 12 test nghiệp vụ, H2 in-memory
scripts/demo.sh                                          # chạy xuyên suốt 1 đợt khám qua API
```

Máy dev mặc định Java 8 — luôn đặt `JAVA_HOME` tới JDK 21.
H2 in-memory, `ddl-auto: create-drop`, dữ liệu mẫu nạp lại mỗi lần khởi động
(1 trường · 3 lớp · 75 học sinh · 1 đợt khám đã chuẩn bị, checklist cố ý thiếu 2 dòng).
Console H2: `/h2-console` (`jdbc:h2:mem:medilink`, user `sa`, không mật khẩu).


## Chạy bằng Docker

```bash
docker compose up -d --build          # build + chạy, http://localhost:8099
docker compose logs -f app            # xem log
docker compose down                   # dừng (giữ dữ liệu)
docker compose down -v                # dừng + xoá dữ liệu, lần sau seed lại từ đầu
PORT=9000 docker compose up -d        # đổi cổng host
```

Image chạy profile `docker`: H2 lưu ra file trên volume `/data` nên **dữ liệu sống qua restart**,
và H2 console bị tắt vì container thường mở ra ngoài mạng.

Chi tiết image: build 2 stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`),
chạy bằng user `app` không phải root, healthcheck qua `/actuator/health`, ~399 MB.

### Dùng image đã build sẵn trên GHCR

Mỗi lần push lên `main`, CI chạy 12 test rồi build và push image. Người khác chỉ cần:

```bash
docker run -d -p 8099:8099 -v medilink-data:/data ghcr.io/<user>/<repo>:latest
```

### Mở ra Internet

```bash
# 1. cài authtoken ngrok một lần
~/bin/ngrok config add-authtoken <TOKEN>
# 2. đặt mật khẩu Basic Auth
cp scripts/ngrok-policy.example.yml scripts/ngrok-policy.yml   # rồi sửa mật khẩu
# 3. mở tunnel (app phải đang chạy ở cổng 8099)
scripts/share-ngrok.sh
```

`scripts/ngrok-policy.yml` chứa mật khẩu nên **đã được .gitignore**; bản commit lên git là
`ngrok-policy.example.yml`. Script từ chối mở tunnel nếu H2 console còn bật.

Chạy bằng `./mvnw` thay vì Docker thì nhớ profile tắt H2 console:

```bash
JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw spring-boot:run -Dspring-boot.run.profiles=tunnel
```

## UI

`http://localhost:8099` — 6 tab đi theo đúng trình tự nghiệp vụ:

| Tab | Vai trò thực tế | Nội dung |
|---|---|---|
| 1. Đợt khám | Điều phối viên | Vòng đời đợt khám, bàn khám đã setup, gói khám |
| 2. Danh sách học sinh | Cán bộ nhận danh sách | Dán CSV, soi lỗi trước ngày khám, lưu dòng hợp lệ |
| 3. Chuẩn bị & kiểm kê | Người chuẩn bị vật tư | Checklist nhân sự/vật tư/thiết bị, kiểm kê trước xuất phát |
| 4. Bàn khám | Bác sĩ tại bàn | Nhận diện học sinh, ghi kết quả chuyên môn |
| 5. Bàn kết luận | Người phụ trách kết luận | Chặn thiếu hạng mục, phân loại sức khỏe |
| 6. Theo dõi & báo cáo | Trưởng đoàn | Tiến độ theo lớp/bàn, danh sách còn thiếu, xuất CSV |

## Nguyên tắc nghiệp vụ được giữ nguyên

**Khám tự do giữa các bàn.** Phần mềm không áp thứ tự khám. Học sinh vào bàn nào trước cũng được;
thứ tự chỉ là thời điểm của từng bản ghi kết quả. Điều kiện duy nhất là **đủ hạng mục bắt buộc**
trước khi tới bàn kết luận.

**Phiếu đi cùng học sinh.** `PhieuKham` là bản số của tờ phiếu giấy: số phiếu in trên giấy, encode vào QR,
và nhận diện tại bàn bằng số phiếu *hoặc* mã định danh — bác sĩ không phải tìm tờ giấy.

## 7 điểm đau → chốt kiểm soát trong code

| # | Công đoạn | Điểm đau | Chốt trong hệ thống |
|---|---|---|---|
| 1 | Nhận danh sách | Sai, thiếu, trùng thông tin | `ImportHocSinhService` — chế độ `KIEM_TRA` soi lỗi mà không ghi DB; tách **lỗi** (chặn) và **cảnh báo** (vẫn nhập được) |
| 2 | Chuẩn bị | Khó kiểm soát số lượng vật tư | `MauChecklist` — định mức/100 học sinh, số lượng cần tính từ danh sách thực tế + dự phòng |
| 3 | Trước xuất phát | Đến trường mới biết thiếu đồ | `GET /kiem-ke` + `POST /xuat-phat` trả **409 THIEU_VAT_TU** kèm đúng danh sách còn thiếu |
| 4 | Nhận diện học sinh | Nhầm hoặc mất phiếu | Tra cứu bằng mã định danh hoặc số phiếu/QR |
| 5 | Khám tự do | Không biết đã khám bàn nào | `TienDoPhieu` — đã khám / còn thiếu theo thời gian thực; ghi lại cùng hạng mục là **sửa**, không tạo dòng mới |
| 6 | Trước kết luận | Học sinh bỏ sót hạng mục | `KetLuanService` trả **409 THIEU_HANG_MUC** kèm danh sách bàn phải quay lại |
| 7 | Sau khám | Khó tổng hợp hàng nghìn phiếu | `BaoCaoService` — tiến độ theo lớp/bàn, phân loại sức khỏe, xuất CSV; **409 CON_PHIEU_CHUA_KET_LUAN** khi đóng đợt sớm |

## Vòng đời đợt khám

```
NHAP ──chuẩn bị (sinh phiếu, dựng bàn, tính checklist)──▶ DANG_CHUAN_BI
     ──kiểm kê đủ──▶ SAN_SANG ──▶ DANG_KHAM ──mọi phiếu đã kết luận──▶ HOAN_THANH
```

Mỗi mũi tên là một chốt: chuyển sai trạng thái trả 409 với `code` nghiệp vụ, không im lặng bỏ qua.

## API

Lỗi nghiệp vụ trả `ProblemDetail` + `code` + dữ liệu để người dùng sửa ngay
(ví dụ `conThieu`, `dongThieu`, `loiTheoTruong`).

```
GET    /api/v1/truong                            danh sách trường + số học sinh + các lớp
GET    /api/v1/truong/{id}/hoc-sinh
POST   /api/v1/import/hoc-sinh?truongId=&cheDo=KIEM_TRA|LUU     body: CSV (text/plain) hoặc multipart

GET    /api/v1/dot-kham | POST /api/v1/dot-kham  | GET /api/v1/dot-kham/{ma}
GET    /api/v1/dot-kham/hang-muc                 hạng mục + chỉ số gợi ý (UI dựng form)
POST   /api/v1/dot-kham/{ma}/chuan-bi
GET    /api/v1/dot-kham/{ma}/ban-kham
GET    /api/v1/dot-kham/{ma}/checklist | PUT     cập nhật số đã chuẩn bị / số kiểm kê
GET    /api/v1/dot-kham/{ma}/kiem-ke
POST   /api/v1/dot-kham/{ma}/xuat-phat | /bat-dau-kham | /hoan-thanh

GET    /api/v1/dot-kham/{ma}/tra-cuu?q=          mã định danh hoặc số phiếu
POST   /api/v1/dot-kham/{ma}/ket-qua             ghi kết quả một bàn (thứ tự bất kỳ)
GET    /api/v1/dot-kham/{ma}/tien-do?lop=&conThieu=
POST   /api/v1/dot-kham/{ma}/ket-luan

GET    /api/v1/bao-cao/{ma} | /api/v1/bao-cao/{ma}/csv
```

## Cấu trúc

```
domain/   DotKham · HocSinh · PhieuKham · BanKham · KetQuaKham · TaiNguyenDotKham + enum
repo/     Spring Data JPA
service/  ImportHocSinhService · DotKhamService · KhamService · KetLuanService · BaoCaoService
          MauChecklist (định mức) · PhanLoaiDeXuat (gợi ý phân loại I–V)
web/      controller + dto (record) + ApiException + GlobalExceptionHandler
static/   UI vanilla JS, không build step
```

## Giới hạn của bản demo

Đây là demo nghiệp vụ, chưa phải bản chạy thật:

- Chưa có xác thực/phân quyền — thật thì mỗi bàn khám là một người dùng có quyền riêng.
- H2 in-memory, mất dữ liệu khi tắt app. Lên thật cần MySQL/Postgres + migration (Flyway).
- Import nhận CSV; tài liệu nói nhà trường gửi Excel → cần thêm Apache POI cho `.xlsx`.
- Chưa in phiếu/QR thật (số phiếu đã sẵn sàng để encode).
- Một hạng mục mặc định một bàn; trường đông cần nhiều bàn cùng hạng mục để giảm thời gian chờ.
- Phân loại sức khỏe I–V hiện là quy tắc đơn giản từ kết luận các bàn, chưa theo thông tư chuẩn
  (chưa dùng BMI theo tuổi/giới). Người kết luận vẫn là người quyết định cuối.
