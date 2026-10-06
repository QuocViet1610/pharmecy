# Hướng dẫn chạy và chia sẻ demo

Tổng quan dự án: xem [README.md](README.md). File này chỉ nói về **vận hành**: chạy ở máy mới,
mở ra Internet bằng ngrok, và xử lý các lỗi hay gặp.

---

## 1. Chạy ở máy mới

```bash
git clone https://github.com/QuocViet1610/pharmecy.git
cd pharmecy
```

**Chỉ có Docker** (không cần Java, không cần Maven):

```bash
docker compose up -d --build     # http://localhost:8099
docker compose logs -f app       # xem log
docker compose down              # dừng, giữ dữ liệu
```

**Có JDK 21** (Maven không cần cài, `./mvnw` tự tải):

```bash
./mvnw spring-boot:run           # http://localhost:8099
```

Máy có nhiều JDK thì chỉ rõ: `JAVA_HOME=/duong/dan/jdk-21 ./mvnw spring-boot:run`

> Chạy bằng Docker thì app **tự bật lại sau khi khởi động máy** (`restart: unless-stopped`).
> Chạy bằng `./mvnw` thì phải gõ lại lệnh mỗi lần.

---

## 2. Mở ra Internet bằng ngrok

### 2.1 Cài ngrok

Cách không cần quyền sudo (đã kiểm chứng trên Ubuntu):

```bash
mkdir -p ~/bin
curl -sL -o /tmp/ngrok.tgz https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar -xzf /tmp/ngrok.tgz -C ~/bin ngrok && chmod +x ~/bin/ngrok
~/bin/ngrok version
```

`~/bin` thường không có trong `PATH`, nên gọi bằng đường dẫn đầy đủ `~/bin/ngrok`, hoặc thêm vào
`~/.bashrc`: `export PATH="$HOME/bin:$PATH"`.

Cách khác: `sudo snap install ngrok`, hoặc kho apt chính thức của ngrok. Trên macOS: `brew install ngrok`.

### 2.2 Cài authtoken (một lần cho mỗi máy)

ngrok **bắt buộc có account**, không chạy ẩn danh được. Đăng ký free tại
https://dashboard.ngrok.com/signup rồi copy token ở
https://dashboard.ngrok.com/get-started/your-authtoken:

```bash
~/bin/ngrok config add-authtoken <TOKEN_CỦA_BẠN>
```

Token lưu vào `~/.config/ngrok/ngrok.yml`, **nằm ngoài repo** nên không bao giờ bị commit.
Một account dùng được trên nhiều máy.

### 2.3 Đặt mật khẩu Basic Auth

```bash
cp scripts/ngrok-policy.example.yml scripts/ngrok-policy.yml
```

Mở `scripts/ngrok-policy.yml`, đổi `DAT_MAT_KHAU_O_DAY` thành mật khẩu thật:

```yaml
credentials:
  - "demo:matkhauthatcuaban"
```

File này **đã được `.gitignore`** vì chứa mật khẩu — bản lên git là `ngrok-policy.example.yml`.
ngrok v3.39 đã bỏ flag `--basic-auth` nên phải cấu hình bằng traffic policy như trên.

### 2.4 Chạy app với profile `tunnel`

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=tunnel
```

**Bắt buộc.** Cấu hình mặc định bật H2 console tại `/h2-console` với user `sa` không mật khẩu —
để lộ ra Internet là cho người lạ chạy SQL tùy ý trên máy bạn. Profile `tunnel` tắt nó đi.
Chạy bằng Docker thì profile `docker` cũng đã tắt sẵn, không cần làm gì thêm.

### 2.5 Mở tunnel

Terminal khác:

```bash
scripts/share-ngrok.sh
```

Script sẽ: kiểm tra app đã chạy chưa → **chối mở tunnel nếu H2 console còn bật** → mở tunnel →
in ra link public dạng `https://xxxx.ngrok-free.app`.

Xem request đang đi qua: http://127.0.0.1:4040

Không muốn đặt mật khẩu (chỉ demo nhanh rồi đóng ngay):

```bash
scripts/share-ngrok.sh --khong-mat-khau
```

### 2.6 Dừng

`Ctrl+C` ở terminal chạy script. Link chết ngay lập tức.

---

## 3. Những điều cần biết trước

**Link đổi mỗi lần khởi động.** ngrok free cấp URL ngẫu nhiên. Muốn URL cố định cần ngrok trả phí
hoặc Cloudflare Tunnel.

**Lần đầu vào sẽ thấy trang cảnh báo của ngrok** — bấm **Visit Site** là qua. Đây là hành vi của
ngrok free, không phải lỗi app.

**Các lời gọi API đã được xử lý sẵn.** ngrok chèn trang cảnh báo HTML vào cả request `fetch`, khiến
UI báo `Unexpected token '<', "<!DOCTYPE "... is not valid JSON`. Code đã gửi header
`ngrok-skip-browser-warning` trong mọi request nên không gặp lỗi này nữa. Nếu bạn viết thêm lời gọi
API mới, nhớ dùng hàm `call()` có sẵn trong `app.js` chứ đừng gọi `fetch` trực tiếp.

**App chưa có xác thực của riêng nó.** Basic Auth của ngrok là lớp bảo vệ duy nhất. Ai qua được lớp đó
đều dùng được mọi API, kể cả ghi kết quả và kết luận phiếu. **Đừng nhập dữ liệu học sinh thật khi
đang mở tunnel.**

---

## 4. Chuyển sang máy khác — 3 thứ không theo bạn

Code thì `git clone` là có đủ. Nhưng ba thứ này **nằm ngoài repo**, phải làm lại ở máy mới:

| Thứ | Nằm ở đâu | Làm lại thế nào |
|---|---|---|
| Dữ liệu đã nhập | H2 trên máy cũ | Không mang theo được. Máy mới có dữ liệu mẫu mới: 75 học sinh, đợt khám về `DANG_CHUAN_BI`, **2 dòng vật tư thiếu xuất hiện lại** → làm lại bước kiểm kê (mục 2 trong "Đi một vòng demo" của README) |
| ngrok authtoken | `~/.config/ngrok/ngrok.yml` | Chạy lại `ngrok config add-authtoken` (mục 2.2) |
| Mật khẩu Basic Auth | `scripts/ngrok-policy.yml` (gitignore) | Copy lại từ file `.example.yml` (mục 2.3) |

Nếu quên bước thứ ba, `share-ngrok.sh` tự copy file mẫu rồi **dừng lại nhắc bạn**, chứ không mở
tunnel với mật khẩu placeholder.

---

## 5. Xử lý sự cố

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `ERR_NGROK_4018 ... session is not authenticated` | Chưa cài authtoken | Mục 2.2 |
| `Unexpected token '<', "<!DOCTYPE "...` | Trang cảnh báo ngrok chen vào lời gọi API | Code đã sửa — **hard refresh** `Ctrl+Shift+R` để bỏ JS cũ trong cache |
| Script báo `H2 console đang BẬT` | Quên profile `tunnel` | Chạy lại với `-Dspring-boot.run.profiles=tunnel` |
| `Port 8099 was already in use` | Cổng bị chiếm | `PORT=9000 docker compose up -d`, hoặc tìm và tắt tiến trình đang giữ cổng: `ss -ltnp \| grep 8099` |
| Vào link bị hỏi mật khẩu liên tục | Sai mật khẩu trong `ngrok-policy.yml` | Kiểm tra lại file, nhớ restart tunnel sau khi sửa |
| ngrok báo lỗi plan khi dùng traffic policy | Basic Auth bị giới hạn theo plan | Dùng `scripts/share-ngrok.sh --khong-mat-khau`, và đóng tunnel ngay sau khi demo |
| `Chưa lấy được URL` | ngrok chết ngay khi khởi động | Xem log ngrok in ở terminal, thường là thiếu authtoken |
| Build lỗi `invalid target release: 21` | `JAVA_HOME` trỏ JDK cũ | `JAVA_HOME=/duong/dan/jdk-21 ./mvnw ...` |

---

## 6. Thay thế ngrok: Cloudflare Tunnel

Không có trang cảnh báo, URL ổn định hơn, miễn phí:

```bash
cloudflared tunnel --url http://localhost:8099
```

Nhưng **không có Basic Auth sẵn** — phải tự thêm xác thực cho app trước khi dùng cho dữ liệu thật.
Vẫn bắt buộc chạy profile `tunnel` để tắt H2 console.
