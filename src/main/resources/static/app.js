'use strict';

const API = '/api/v1';
const state = { dotKham: null, truongId: null, hangMuc: [], banKham: [], phieu: null };

/* ---------- helpers ---------- */

async function call(method, path, body, kieu) {
  const opt = {
    method,
    // ngrok (plan free) chèn trang cảnh báo HTML vào request từ browser; header này tắt nó đi,
    // nếu không mọi lời gọi API sẽ nhận HTML và JSON.parse báo "Unexpected token '<'".
    headers: { 'ngrok-skip-browser-warning': 'true' },
  };
  if (body !== undefined) {
    if (kieu === 'text') {
      opt.headers['Content-Type'] = 'text/plain;charset=UTF-8';
      opt.body = body;
    } else {
      opt.headers['Content-Type'] = 'application/json';
      opt.body = JSON.stringify(body);
    }
  }

  const res = await fetch(API + path, opt);
  const text = await res.text();

  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      // Không phải JSON: proxy/tunnel/trang đăng nhập chen vào giữa. Báo rõ thay vì lỗi parse.
      const dau = text.trim().slice(0, 120);
      throw Object.assign(
        new Error(`Server trả về ${res.headers.get('content-type') || 'nội dung không rõ'} `
          + `thay vì JSON (HTTP ${res.status}). Có thể một proxy/tunnel đang chen trang HTML vào: ${dau}`),
        { status: res.status });
    }
  }

  if (!res.ok) throw Object.assign(new Error(data?.detail || res.statusText), { data, status: res.status });
  return data;
}

const get = (p) => call('GET', p);
const post = (p, b) => call('POST', p, b);
const put = (p, b) => call('PUT', p, b);

const $ = (s) => document.querySelector(s);
const el = (id) => document.getElementById(id);

function esc(v) {
  return String(v ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}

function toast(msg, kieu) {
  const d = document.createElement('div');
  if (kieu) d.className = kieu;
  d.textContent = msg;
  el('toast').append(d);
  setTimeout(() => d.remove(), 5000);
}

function loi(e, out) {
  const d = e.data || {};
  const dong = [d.detail || e.message];
  if (d.code) dong.push('code: ' + d.code);
  if (d.loiTheoTruong) dong.push(JSON.stringify(d.loiTheoTruong, null, 2));
  if (d.conThieu) dong.push('Còn thiếu: ' + d.conThieu.map((h) => h.ten).join(', '));
  if (d.dongThieu) dong.push('Thiếu: ' + d.dongThieu.map((t) => `${t.ten} (thiếu ${t.soConThieu} ${t.donVi})`).join('; '));
  toast(dong[0], 'bad');
  if (out) el(out).textContent = dong.join('\n');
}

function badgeTrangThai(tt) {
  const map = {
    NHAP: 'muted', DANG_CHUAN_BI: 'warn', SAN_SANG: '', DANG_KHAM: 'ok', HOAN_THANH: 'ok',
    CHUA_KHAM: 'muted', DU_HANG_MUC: 'warn', DA_KET_LUAN: 'ok',
    BINH_THUONG: 'ok', CAN_THEO_DOI: 'warn', BAT_THUONG: 'bad',
  };
  return `<span class="badge ${map[tt] ?? ''}">${esc(tt)}</span>`;
}

function stat(nhan, giaTri) {
  return `<div class="stat"><b>${esc(giaTri)}</b><span>${esc(nhan)}</span></div>`;
}

/* ---------- tabs ---------- */

document.querySelectorAll('nav.tabs button').forEach((b) => {
  b.onclick = () => {
    document.querySelectorAll('nav.tabs button').forEach((x) => x.classList.toggle('active', x === b));
    document.querySelectorAll('.tab').forEach((s) => s.classList.toggle('active', s.id === 'tab-' + b.dataset.tab));
    if (b.dataset.tab === 'chuan-bi') veChecklist();
    if (b.dataset.tab === 'bao-cao') veBaoCao();
  };
});

/* ---------- khởi tạo ---------- */

async function khoiTao() {
  state.hangMuc = await get('/dot-kham/hang-muc');
  const truong = await get('/truong');
  el('truongId').innerHTML = truong
    .map((t) => `<option value="${t.id}">${esc(t.ten)} — ${t.soHocSinh} HS</option>`).join('');
  state.truongId = truong[0]?.id ?? null;
  el('truongId').onchange = (e) => { state.truongId = Number(e.target.value); };

  const ds = await get('/dot-kham');
  el('dotKham').innerHTML = ds
    .map((d) => `<option value="${esc(d.ma)}">${esc(d.ma)} · ${esc(d.tenTruong)}</option>`).join('');
  el('dotKham').onchange = () => doiDotKham(el('dotKham').value);
  if (ds.length) await doiDotKham(ds[0].ma);
}

async function doiDotKham(ma) {
  state.dotKham = await get('/dot-kham/' + ma);
  el('dotKham').value = ma;
  el('linkCsv').href = `${API}/bao-cao/${ma}/csv`;
  veDotKham();
  await veBanKham();
}

/* ---------- 1. đợt khám ---------- */

const BUOC = [
  ['NHAP', 'Tạo đợt khám — nhận danh sách từ nhà trường'],
  ['DANG_CHUAN_BI', 'Chuẩn bị — in phiếu, dựng bàn, tính checklist vật tư'],
  ['SAN_SANG', 'Kiểm kê đủ → xuất phát đến trường'],
  ['DANG_KHAM', 'Setup khu khám → học sinh khám tự do giữa các bàn'],
  ['HOAN_THANH', 'Mọi phiếu đã kết luận → thu phiếu, lưu trữ, báo cáo'],
];

function veDotKham() {
  const d = state.dotKham;
  el('trangThaiDot').outerHTML = `<span id="trangThaiDot" class="badge ok">${esc(d.trangThai)}</span>`;
  const hienTai = BUOC.findIndex((b) => b[0] === d.trangThai);
  el('flow').innerHTML = BUOC.map(([ma, nhan], i) =>
    `<li class="${i < hienTai ? 'xong' : i === hienTai ? 'hientai' : ''}">${esc(nhan)}</li>`).join('');
  el('goiKham').innerHTML = d.hangMucBatBuoc.map((h) => `<span class="badge">${esc(h.ten)}</span>`).join('')
    + ` <span class="badge muted">${d.soPhieu} phiếu · ${d.soDaKetLuan} đã kết luận</span>`;

  const tt = d.trangThai;
  el('btnChuanBi').disabled = !(tt === 'NHAP' || tt === 'DANG_CHUAN_BI');
  el('btnXuatPhat').disabled = tt !== 'DANG_CHUAN_BI';
  el('btnBatDau').disabled = tt !== 'SAN_SANG';
  el('btnHoanThanh').disabled = tt !== 'DANG_KHAM';
}

async function veBanKham() {
  state.banKham = await get(`/dot-kham/${state.dotKham.ma}/ban-kham`);
  $('#tblBan tbody').innerHTML = state.banKham.map((b) =>
    `<tr><td>${esc(b.ten)}</td><td>${esc(b.tenHangMuc)}</td><td>${esc(b.nhanSu ?? '—')}</td>
     <td class="num">${b.soLuotDaKham}</td></tr>`).join('');
  el('banKhamSelect').innerHTML = state.banKham
    .map((b) => `<option value="${b.id}" data-hang-muc="${esc(b.hangMuc)}">${esc(b.ten)}</option>`).join('');
  veChiSoForm();
}

async function buoc(url, nhan) {
  try {
    const kq = await post(`/dot-kham/${state.dotKham.ma}${url}`);
    el('dotKhamOut').textContent = JSON.stringify(kq, null, 2);
    toast(nhan + ' thành công', 'ok');
    await doiDotKham(state.dotKham.ma);
  } catch (e) { loi(e, 'dotKhamOut'); }
}

el('btnChuanBi').onclick = () => buoc('/chuan-bi', 'Chuẩn bị');
el('btnXuatPhat').onclick = () => buoc('/xuat-phat', 'Xuất phát');
el('btnBatDau').onclick = () => buoc('/bat-dau-kham', 'Bắt đầu khám');
el('btnHoanThanh').onclick = () => buoc('/hoan-thanh', 'Hoàn thành đợt khám');

/* ---------- 2. import danh sách ---------- */

const CSV_MAU = `ma_dinh_danh,ho_ten,ngay_sinh,gioi_tinh,lop,khoi,ho_ten_phu_huynh,dien_thoai_phu_huynh
HS09001,Nguyễn Văn Khoa,12/05/2014,Nam,8A1,8,Nguyễn Văn Bình,0912000001
HS09002,Trần Thị Mai,03/09/2014,Nữ,8A1,8,Trần Văn Hùng,0912000002
HS09003,,21/07/2014,Nam,8A1,8,Lê Thị Hoa,0912000003
HS09004,Phạm Minh Long,31/02/2014,Nam,8A1,8,Phạm Văn Tú,0912000004
HS09002,Trần Thị Mai,03/09/2014,Nữ,8A1,8,Trần Văn Hùng,0912000002
HS09005,Vũ Ngọc Linh,18/11/2014,Khac,8A2,8,Vũ Văn Thái,091200
HS09006,Đỗ Thành Nam,05/01/2014,Nam2,8A2,8,Đỗ Văn Lâm,0912000006
HS09007,Hoàng Thu Trang,27/04/1998,Nữ,8A2,8,Hoàng Văn Sơn,0912000007
HS09008,Bùi Đức Việt,09/08/2014,Nam,,8,Bùi Thị Lan,0912000008`;

el('btnMauLoi').onclick = () => { el('csv').value = CSV_MAU; };

async function nhapDanhSach(cheDo) {
  try {
    const kq = await call('POST', `/import/hoc-sinh?truongId=${state.truongId}&cheDo=${cheDo}`,
      el('csv').value, 'text');
    el('importTomTat').innerHTML = [
      stat('Tổng dòng', kq.tongDong),
      stat('Hợp lệ', kq.soDongHopLe),
      stat('Dòng lỗi', kq.soDongLoi),
      stat('Đã lưu', kq.soDaLuu),
    ].join('');

    const khoi = [];
    if (kq.dongLoi.length) {
      khoi.push(`<div class="alert bad"><strong>${kq.dongLoi.length} dòng bị chặn</strong> — sửa với nhà trường
        trước ngày khám:<ul>${kq.dongLoi.map((d) =>
        `<li>Dòng ${d.dong} ${d.maDinhDanh ? `(${esc(d.maDinhDanh)})` : ''}: ${d.loi.map(esc).join('; ')}</li>`).join('')}</ul></div>`);
    }
    if (kq.dongCanhBao.length) {
      khoi.push(`<div class="alert warn"><strong>${kq.dongCanhBao.length} dòng có cảnh báo</strong> — vẫn nhập được:
        <ul>${kq.dongCanhBao.map((d) =>
        `<li>Dòng ${d.dong} (${esc(d.maDinhDanh)}): ${d.canhBao.map(esc).join('; ')}</li>`).join('')}</ul></div>`);
    }
    if (!khoi.length) khoi.push('<div class="alert ok">Danh sách sạch, không có lỗi.</div>');
    el('importLoi').innerHTML = khoi.join('');

    if (cheDo === 'LUU') {
      toast(`Đã lưu ${kq.soDaLuu} học sinh`, 'ok');
      await khoiTao();
    }
  } catch (e) { loi(e); }
}

el('btnKiemTra').onclick = () => nhapDanhSach('KIEM_TRA');
el('btnLuu').onclick = () => nhapDanhSach('LUU');

/* ---------- 3. checklist + kiểm kê ---------- */

async function veChecklist() {
  const ds = await get(`/dot-kham/${state.dotKham.ma}/checklist`);
  $('#tblChecklist tbody').innerHTML = ds.map((t) => `
    <tr class="${t.du ? '' : 'thieu'}" data-id="${t.id}">
      <td>${esc(t.loai)}</td><td>${esc(t.ten)}</td>
      <td class="num">${t.soLuongCan}</td><td class="num">${t.soLuongDuPhong}</td>
      <td class="num"><strong>${t.tongPhaiCo}</strong></td>
      <td class="num"><input type="number" min="0" class="chuanBi" value="${t.soLuongDaChuanBi}"></td>
      <td class="num"><input type="number" min="0" class="kiemKe" value="${t.soLuongKiemKe ?? ''}"></td>
      <td class="num">${t.soConThieu ? `<span class="badge bad">${t.soConThieu} ${esc(t.donVi ?? '')}</span>` : '<span class="badge ok">đủ</span>'}</td>
    </tr>`).join('');
}

el('btnLuuChecklist').onclick = async () => {
  const body = [...document.querySelectorAll('#tblChecklist tbody tr')].map((tr) => ({
    id: Number(tr.dataset.id),
    soLuongDaChuanBi: Number(tr.querySelector('.chuanBi').value || 0),
    soLuongKiemKe: tr.querySelector('.kiemKe').value === '' ? null : Number(tr.querySelector('.kiemKe').value),
  }));
  try {
    await put(`/dot-kham/${state.dotKham.ma}/checklist`, body);
    toast('Đã lưu checklist', 'ok');
    await veChecklist();
  } catch (e) { loi(e); }
};

el('btnKiemKe').onclick = async () => {
  try {
    const kq = await get(`/dot-kham/${state.dotKham.ma}/kiem-ke`);
    const canhBao = kq.canhBao.length
      ? `<ul>${kq.canhBao.map((c) => `<li>${esc(c)}</li>`).join('')}</ul>` : '';
    el('kiemKeOut').innerHTML = kq.choPhepXuatPhat
      ? `<div class="alert ok"><strong>Đủ điều kiện xuất phát.</strong>${canhBao}</div>`
      : `<div class="alert bad"><strong>Chưa được xuất phát — còn ${kq.soDongThieu} dòng thiếu:</strong>
         <ul>${kq.dongThieu.map((t) => `<li>${esc(t.ten)}: cần ${t.tongPhaiCo}, có ${t.soLuongKiemKe ?? t.soLuongDaChuanBi}
           → <strong>thiếu ${t.soConThieu} ${esc(t.donVi ?? '')}</strong></li>`).join('')}</ul>${canhBao}</div>`;
    await veChecklist();
  } catch (e) { loi(e); }
};

/* ---------- 4. bàn khám ---------- */

function hangMucDangChon() {
  return el('banKhamSelect').selectedOptions[0]?.dataset.hangMuc;
}

function veChiSoForm() {
  const hm = state.hangMuc.find((h) => h.ma === hangMucDangChon());
  el('chiSoForm').innerHTML = (hm?.chiSoGoiY ?? [])
    .map((c) => `<label>${esc(c)}<input data-chi-so="${esc(c)}" autocomplete="off"></label>`).join('');
}

el('banKhamSelect').onchange = veChiSoForm;

function vePhieu(t, dich) {
  const daKham = t.daKham.map((k) =>
    `<span class="badge ok" title="${esc(k.banKham ?? '')} ${esc(k.bacSi ?? '')}">${esc(k.tenHangMuc)}: ${esc(k.ketLuanChuyenMon)}</span>`).join(' ');
  const thieu = t.conThieu.map((h) => `<span class="badge bad">${esc(h.ten)}</span>`).join(' ');
  el(dich).innerHTML = `
    <div class="alert ${t.duHangMuc ? 'ok' : 'warn'}">
      <strong>${esc(t.hoTen)}</strong> · ${esc(t.lop)} · ${esc(t.maDinhDanh)} · phiếu ${esc(t.soPhieu)}
      ${badgeTrangThai(t.trangThai)}
      <div class="chips" style="margin-top:8px">${daKham || '<span class="badge muted">chưa khám bàn nào</span>'}</div>
      ${t.conThieu.length
        ? `<div style="margin-top:8px">Còn thiếu: <span class="chips">${thieu}</span></div>`
        : `<div style="margin-top:8px">Đủ hạng mục — hệ thống đề xuất phân loại
            <strong>${esc(t.phanLoaiDeXuat ?? '—')}</strong></div>`}
      ${t.ketLuan ? `<div style="margin-top:8px">Kết luận: ${esc(t.ketLuan)} (${esc(t.nguoiKetLuan)})</div>` : ''}
    </div>`;
  state.phieu = t;
}

el('btnTraCuu').onclick = async () => {
  try {
    vePhieu(await get(`/dot-kham/${state.dotKham.ma}/tra-cuu?q=${encodeURIComponent(el('qKham').value)}`), 'phieuInfo');
  } catch (e) { loi(e, 'khamOut'); el('phieuInfo').innerHTML = ''; }
};
el('qKham').onkeydown = (e) => { if (e.key === 'Enter') el('btnTraCuu').click(); };

el('btnGhiKetQua').onclick = async () => {
  const chiTiet = {};
  document.querySelectorAll('#chiSoForm input').forEach((i) => { if (i.value) chiTiet[i.dataset.chiSo] = i.value; });
  const body = {
    maNhanDien: el('qKham').value.trim(),
    hangMuc: hangMucDangChon(),
    banKhamId: Number(el('banKhamSelect').value),
    bacSi: el('bacSi').value || null,
    ketLuanChuyenMon: el('ketLuanChuyenMon').value,
    chiTiet,
    ghiChu: el('ghiChuKham').value || null,
  };
  try {
    const t = await post(`/dot-kham/${state.dotKham.ma}/ket-qua`, body);
    vePhieu(t, 'phieuInfo');
    el('khamOut').textContent = `Đã ghi ${body.hangMuc} cho ${t.hoTen}. ` +
      (t.duHangMuc ? 'Đủ hạng mục → mời sang bàn kết luận.' : `Còn thiếu: ${t.conThieu.map((h) => h.ten).join(', ')}`);
    document.querySelectorAll('#chiSoForm input').forEach((i) => { i.value = ''; });
    el('ghiChuKham').value = '';
    await veBanKham();
  } catch (e) { loi(e, 'khamOut'); }
};

/* ---------- 5. bàn kết luận ---------- */

el('btnTraCuuKetLuan').onclick = async () => {
  try {
    const t = await get(`/dot-kham/${state.dotKham.ma}/tra-cuu?q=${encodeURIComponent(el('qKetLuan').value)}`);
    vePhieu(t, 'ketLuanInfo');
    el('btnKetLuan').disabled = !t.duHangMuc;
    if (!t.duHangMuc) toast(`Thiếu ${t.conThieu.length} bàn — mời học sinh quay lại`, 'bad');
  } catch (e) { loi(e, 'ketLuanOut'); el('ketLuanInfo').innerHTML = ''; }
};
el('qKetLuan').onkeydown = (e) => { if (e.key === 'Enter') el('btnTraCuuKetLuan').click(); };

el('btnKetLuan').onclick = async () => {
  try {
    const t = await post(`/dot-kham/${state.dotKham.ma}/ket-luan`, {
      maNhanDien: el('qKetLuan').value.trim(),
      phanLoaiSucKhoe: el('phanLoai').value || null,
      ketLuan: el('noiDungKetLuan').value || null,
      nguoiKetLuan: el('nguoiKetLuan').value,
    });
    vePhieu(t, 'ketLuanInfo');
    el('ketLuanOut').textContent = `Phiếu ${t.soPhieu} đã kết luận — phân loại ${t.phanLoaiSucKhoe}.`;
    toast('Đã kết luận và thu phiếu', 'ok');
    await doiDotKham(state.dotKham.ma);
  } catch (e) { loi(e, 'ketLuanOut'); }
};

/* ---------- 6. báo cáo ---------- */

async function veBaoCao() {
  const b = await get(`/bao-cao/${state.dotKham.ma}`);
  el('baoCaoStats').innerHTML = [
    stat('Tổng học sinh', b.tongHocSinh),
    stat('Chưa khám', b.chuaKham),
    stat('Đang khám', b.dangKham),
    stat('Đủ, chờ kết luận', b.duHangMucChoKetLuan),
    stat('Đã kết luận', b.daKetLuan),
    stat('Hoàn thành', b.phanTramHoanThanh + '%'),
  ].join('');

  $('#tblLop tbody').innerHTML = b.theoLop.map((l) =>
    `<tr><td>${esc(l.lop)}</td><td class="num">${l.tongSo}</td><td class="num">${l.chuaKham}</td>
     <td class="num">${l.dangKham}</td><td class="num">${l.duHangMuc}</td><td class="num">${l.daKetLuan}</td></tr>`).join('');

  $('#tblTaiBan tbody').innerHTML = b.theoBan.map((t) =>
    `<tr><td>${esc(t.tenHangMuc)}</td><td class="num">${t.daKham}</td><td class="num">${t.conLai}</td></tr>`).join('');

  const pl = Object.entries(b.phanLoaiSucKhoe);
  el('phanLoaiOut').innerHTML = pl.length
    ? pl.map(([k, v]) => `<span class="badge">Loại ${esc(k)}: ${v}</span>`).join('')
    : '<span class="badge muted">chưa có phiếu nào được kết luận</span>';

  const ctd = Object.entries(b.canTheoDoiTheoHangMuc);
  el('canTheoDoiOut').innerHTML = ctd.length
    ? ctd.map(([k, v]) => `<span class="badge warn">${esc(k)}: ${v}</span>`).join('')
    : '<span class="badge ok">không có ca cần theo dõi</span>';

  $('#tblConThieu tbody').innerHTML = b.hocSinhConThieu.length
    ? b.hocSinhConThieu.map((h) =>
      `<tr><td>${esc(h.soPhieu)}</td><td>${esc(h.hoTen)}</td><td>${esc(h.lop)}</td>
       <td>${h.hangMucConThieu.map((x) => `<span class="badge bad">${esc(x)}</span>`).join(' ')}</td></tr>`).join('')
    : '<tr><td colspan="4">Tất cả học sinh đã đủ hạng mục.</td></tr>';
}

el('btnLamMoiBaoCao').onclick = veBaoCao;

khoiTao().catch((e) => toast('Không tải được dữ liệu: ' + e.message, 'bad'));
