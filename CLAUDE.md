# CLAUDE.md

Hướng dẫn cho Claude Code trong `medilink-checkup`. Tổng quan nghiệp vụ và API: xem `README.md`.

## Lệnh

```bash
JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw test            # 12 test, H2, không cần Docker
JAVA_HOME=~/.jdks/ms-21.0.11 ./mvnw spring-boot:run # cổng 8099
scripts/demo.sh                                      # smoke test xuyên suốt qua API
docker compose up -d --build                         # chạy trong container, cổng 8099
scripts/share-ngrok.sh                               # mở ra Internet (cần authtoken ngrok)
```

Ba profile: mặc định (dev, H2 in-memory + console), `tunnel` (tắt H2 console khi mở ra Internet),
`docker` (H2 file trên volume `/data`, `ddl-auto: update`, console tắt).

Máy dev mặc định Java 8 — luôn đặt `JAVA_HOME` tới JDK 21. Cổng 8080–8090 đã bị các project khác
chiếm nên project này dùng 8099.

## Quy tắc

- **Không áp thứ tự khám.** Nghiệp vụ là khám tự do giữa các bàn; mọi thay đổi phải giữ được việc
  học sinh vào bàn nào trước cũng được. Chỉ `KetLuanService` kiểm tra tính đủ hạng mục.
- Lỗi nghiệp vụ: `throw ApiException.badRequest|notFound|conflict(CODE, "message tiếng Việt")`.
  Controller trả thẳng DTO (record), không try/catch.
- Chốt chặn phải kèm dữ liệu để sửa được: `conflict(code, msg, Map.of("conThieu", ...))` →
  `GlobalExceptionHandler` đẩy vào `ProblemDetail` làm property.
- Mỗi `(phiếu, hạng mục)` chỉ một `KetQuaKham`: ghi lần hai gọi `ghiLai(...)`, không insert dòng mới.
- Enum lưu DB dùng `EnumType.STRING`. Entity không có setter công khai — đổi trạng thái qua
  phương thức nghiệp vụ (`chuyenTrangThai`, `ketLuan`, `kiemKe`...).
- `chuanBi()` phải idempotent: nhà trường bổ sung học sinh muộn thì chạy lại chỉ sinh phiếu còn thiếu
  và cập nhật lại định mức checklist.
- Thêm hạng mục khám = thêm hằng vào enum `HangMuc` kèm danh sách `ChiSo` (nhãn, kiểu `so`/`text`,
  giá trị ví dụ); UI tự dựng form từ `GET /api/v1/dot-kham/hang-muc`, **không sửa JS**.
- **Nút bị khóa phải nói lý do.** `lyDoKhoa(trangThai)` trong `app.js` trả lý do cho từng nút vòng đời,
  gắn vào `title` và dòng "Bước tiếp theo". Nút xám không kèm lý do khiến người dùng không phân biệt
  được "bước đã qua" với "app hỏng" — đã gặp thật.
- Tab 4 là đường nóng (~375 lượt nhập/đợt). Mọi thay đổi ở đó phải giữ: bàn trực chọn một lần,
  ô tìm luôn được focus, ghi xong tự dọn và quay về ô tìm, và một nút ghi "bình thường" trong 1 click.
- `HocSinh.hoTenTimKiem` là bản không dấu của `hoTen`, set trong constructor qua `ChuanHoaText.boDau`.
  Sửa tên học sinh ở đâu thì phải cập nhật lại cột này, nếu không tìm không dấu sẽ ra sai.

## Bảo mật khi mở ra ngoài

- App **chưa có auth**. Mở ra Internet thì bắt buộc chạy profile `tunnel` hoặc `docker`
  (tắt H2 console — console cho chạy SQL tùy ý, là vector RCE đã biết) và đặt Basic Auth
  qua `scripts/ngrok-policy.yml`.
- Mật khẩu/token không bao giờ nằm trong file commit lên git: `scripts/ngrok-policy.yml`
  và `.env` đã ở `.gitignore`, bản mẫu là `scripts/ngrok-policy.example.yml`.

## Cạm bẫy đã gặp

- `@Transactional` trên phương thức được gọi nội bộ trong cùng bean (self-invocation) **không có tác
  dụng** — proxy bị bỏ qua, thay đổi trên entity không được flush. `DuLieuMau` từng seed sai vì lý do
  này: phải để Spring gọi qua proxy (`implements ApplicationRunner` + `@Transactional` trên `run`).
- `DateTimeFormatter.ofPattern("dd/MM/yyyy")` dùng ResolverStyle SMART → `31/02/2014` bị âm thầm sửa
  thành 28/02 và lỗi nhập liệu không bao giờ lộ. Phải dùng `uuuu` + `ResolverStyle.STRICT`.
- ngrok v3.39 đã **bỏ flag `--basic-auth`** — phải cấu hình qua `--traffic-policy-file`.
- ngrok plan free chèn **trang cảnh báo HTML** vào mọi request từ browser, kể cả `fetch` gọi API →
  UI báo `Unexpected token '<', "<!DOCTYPE "... is not valid JSON`. Mọi `fetch` trong `app.js` phải
  gửi header `ngrok-skip-browser-warning`. `call()` cũng bắt lỗi không-phải-JSON và báo rõ nguyên
  nhân thay vì để lộ lỗi parse.
- Entity trả thẳng ra JSON sẽ lỗi với quan hệ LAZY và không có setter cho Jackson — luôn map sang
  record DTO ở tầng `web`.
