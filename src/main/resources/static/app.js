'use strict';

const API = '/api/v1';
const state = {
  dotKham: null, truongId: null, hangMuc: [], banKham: [], phieu: null,
  ban: null,        // bàn đang trực (chọn 1 lần/buổi, nhớ trong localStorage)
  bacSi: '',
  daGhiPhien: [],   // nhật ký các lượt vừa ghi, để bác sĩ tự đối chiếu
};

/** localStorage có thể bị chặn (chế độ riêng tư) — không để vỡ UI. */
const nho = {
  doc(k) { try { return localStorage.getItem(k); } catch { return null; } },
  ghi(k, v) { try { localStorage.setItem(k, v); } catch { /* bỏ qua */ } },
};

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
  state.lop = truong[0]?.lop ?? [];
  el('truongId').onchange = (e) => {
    state.truongId = Number(e.target.value);
    state.lop = truong.find((t) => t.id === state.truongId)?.lop ?? [];
  };

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
  // Đang trực bàn nào thì giữ nguyên, chưa chọn thì hiện màn hình chọn bàn.
  if (state.ban) {
    const moi = state.banKham.find((b) => b.id === state.ban.id);
    if (moi) state.ban = moi;
  } else {
    phucHoiBan();
  }
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
/* Đây là màn hình được dùng nhiều nhất: 75 học sinh x 5 bàn = ~375 lượt nhập mỗi đợt.
   Mọi động tác dư ở đây đều bị nhân lên 375 lần, nên luồng được gói lại thành:
   quét/gõ -> Enter -> (đa số) một nút "Bình thường" -> tự sang học sinh kế tiếp. */

const KHOA_BAN = 'medilink.ban';
const KHOA_BACSI = 'medilink.bacSi';

function veChonBan() {
  el('chonBanList').innerHTML = state.banKham.map((b) => `
    <button class="chon-ban-item" data-id="${b.id}">
      <strong>${esc(b.tenHangMuc)}</strong>
      <span>${esc(b.ten)}</span>
    </button>`).join('');

  let chon = null;
  el('chonBanList').querySelectorAll('.chon-ban-item').forEach((btn) => {
    btn.onclick = () => {
      chon = Number(btn.dataset.id);
      el('chonBanList').querySelectorAll('.chon-ban-item')
        .forEach((x) => x.classList.toggle('active', x === btn));
      el('btnVaoBan').disabled = false;
    };
  });

  el('bacSi').value = nho.doc(KHOA_BACSI) || '';
  el('btnVaoBan').onclick = () => {
    const ban = state.banKham.find((b) => b.id === chon);
    if (ban) vaoBan(ban, el('bacSi').value.trim());
  };
}

function vaoBan(ban, bacSi) {
  state.ban = ban;
  state.bacSi = bacSi;
  nho.ghi(KHOA_BAN, String(ban.id));
  nho.ghi(KHOA_BACSI, bacSi);

  el('chonBanCard').hidden = true;
  el('khamCard').hidden = false;
  el('tenBanDangTruc').textContent = `${ban.tenHangMuc} — ${ban.ten}`;
  el('bacSiDangTruc').textContent = bacSi ? '· ' + bacSi : '';
  el('tieuDeGhi').textContent = 'Ghi kết quả ' + ban.tenHangMuc;

  el('locLop').innerHTML = '<option value="">Mọi lớp</option>'
    + (state.lop ?? []).map((l) => `<option>${esc(l)}</option>`).join('');

  veChiSoForm();
  hocSinhMoi();
}

el('btnDoiBan').onclick = () => {
  state.ban = null;
  el('khamCard').hidden = true;
  el('chonBanCard').hidden = false;
  el('btnVaoBan').disabled = true;
  veChonBan();
};

/** Khôi phục bàn đã chọn ở phiên trước, để F5 không phải chọn lại. */
function phucHoiBan() {
  const id = Number(nho.doc(KHOA_BAN));
  const ban = state.banKham.find((b) => b.id === id);
  if (ban) vaoBan(ban, nho.doc(KHOA_BACSI) || '');
  else { el('khamCard').hidden = true; el('chonBanCard').hidden = false; veChonBan(); }
}

function veChiSoForm() {
  const hm = state.hangMuc.find((h) => h.ma === state.ban?.hangMuc);
  el('chiSoForm').innerHTML = (hm?.chiSo ?? []).map((c) => `
    <label>${esc(c.nhan)}
      <input data-chi-so="${esc(c.ma)}" placeholder="${esc(c.vd)}" autocomplete="off"
             ${c.kieu === 'so' ? 'inputmode="decimal"' : ''}>
    </label>`).join('');
}

/** Dọn màn hình để đón học sinh tiếp theo và đưa con trỏ về ô quét. */
function hocSinhMoi() {
  state.phieu = null;
  el('qKham').value = '';
  el('ketQuaTim').innerHTML = '';
  el('phieuInfo').innerHTML = '';
  el('canhBaoGhiLai').innerHTML = '';
  el('formGhiCard').hidden = true;
  el('ghiChuBox').hidden = true;
  el('ghiChuKham').value = '';
  document.querySelectorAll('#chiSoForm input').forEach((i) => { i.value = ''; });
  el('qKham').focus();
}

async function timHocSinh() {
  const q = el('qKham').value.trim();
  if (q.length < 2) { toast('Nhập ít nhất 2 ký tự', 'bad'); return; }
  const lop = el('locLop').value;
  try {
    const ds = await get(`/dot-kham/${state.dotKham.ma}/tim?q=${encodeURIComponent(q)}`
      + (lop ? `&lop=${encodeURIComponent(lop)}` : ''));
    if (ds.length === 0) {
      el('ketQuaTim').innerHTML = `<div class="alert bad">Không tìm thấy "${esc(q)}".
        Thử gõ tên không dấu, hoặc bỏ lọc lớp.</div>`;
      el('formGhiCard').hidden = true;
      return;
    }
    if (ds.length === 1) { chonPhieu(ds[0]); return; }
    // Nhiều kết quả: cho chọn bằng chuột hoặc bấm số 1..9.
    el('ketQuaTim').innerHTML = `<div class="hint">${ds.length} học sinh trùng khớp — chọn một
      (hoặc bấm số):</div><div class="ds-chon">` + ds.map((t, i) => `
      <button class="ds-chon-item" data-i="${i}">
        <span class="stt">${i + 1}</span>
        <span><strong>${esc(t.hoTen)}</strong><small>${esc(t.lop)} · ${esc(t.maDinhDanh)}</small></span>
        ${t.duHangMuc ? '<span class="badge ok">đủ hạng mục</span>'
          : `<span class="badge warn">còn ${t.conThieu.length} bàn</span>`}
      </button>`).join('') + '</div>';
    el('ketQuaTim').querySelectorAll('.ds-chon-item').forEach((btn) => {
      btn.onclick = () => chonPhieu(ds[Number(btn.dataset.i)]);
    });
    state.ketQuaTim = ds;
    el('phieuInfo').innerHTML = '';
    el('formGhiCard').hidden = true;
  } catch (e) { loi(e); }
}

function chonPhieu(t) {
  state.ketQuaTim = null;
  el('ketQuaTim').innerHTML = '';
  vePhieu(t, 'phieuInfo');

  if (t.trangThai === 'DA_KET_LUAN') {
    el('canhBaoGhiLai').innerHTML = `<div class="alert bad">Phiếu đã kết luận và thu lại —
      không ghi thêm được.</div>`;
    el('formGhiCard').hidden = true;
    return;
  }

  el('formGhiCard').hidden = false;
  // Bàn này đã ghi cho em rồi: nói rõ đây là SỬA, kèm giá trị cũ, tránh ghi đè mà không biết.
  const daCo = t.daKham.find((k) => k.hangMuc === state.ban.hangMuc);
  if (daCo) {
    el('canhBaoGhiLai').innerHTML = `<div class="alert warn">Bàn này <strong>đã ghi</strong> cho em
      lúc ${new Date(daCo.thoiDiem).toLocaleTimeString('vi-VN')}
      (${esc(daCo.ketLuanChuyenMon)}${daCo.bacSi ? ', ' + esc(daCo.bacSi) : ''}).
      Ghi tiếp là <strong>sửa kết quả cũ</strong>.</div>`;
    Object.entries(daCo.chiTiet ?? {}).forEach(([k, v]) => {
      const i = document.querySelector(`#chiSoForm input[data-chi-so="${k}"]`);
      if (i) i.value = v;
    });
    el('ghiChuKham').value = daCo.ghiChu ?? '';
  } else {
    el('canhBaoGhiLai').innerHTML = '';
  }
  el('chiSoForm').querySelector('input')?.focus();
}

async function ghiKetQua(ketLuanChuyenMon) {
  if (!state.phieu) { toast('Chưa chọn học sinh', 'bad'); return; }
  const chiTiet = {};
  document.querySelectorAll('#chiSoForm input').forEach((i) => {
    if (i.value.trim()) chiTiet[i.dataset.chiSo] = i.value.trim();
  });
  const body = {
    maNhanDien: state.phieu.soPhieu,
    hangMuc: state.ban.hangMuc,
    banKhamId: state.ban.id,
    bacSi: state.bacSi || null,
    ketLuanChuyenMon,
    chiTiet,
    ghiChu: el('ghiChuKham').value.trim() || null,
  };
  try {
    const t = await post(`/dot-kham/${state.dotKham.ma}/ket-qua`, body);
    themVaoNhatKy(t, ketLuanChuyenMon);
    toast(`${t.hoTen}: đã ghi ${state.ban.tenHangMuc}`
      + (t.duHangMuc ? ' — ĐỦ hạng mục, mời sang bàn kết luận'
        : ` — còn ${t.conThieu.length} bàn`), 'ok');
    hocSinhMoi();
    veBanKham();
  } catch (e) { loi(e); }
}

function themVaoNhatKy(t, ketLuan) {
  state.daGhiPhien.unshift({ gio: new Date().toLocaleTimeString('vi-VN'), t, ketLuan });
  state.daGhiPhien = state.daGhiPhien.slice(0, 8);
  el('demDaGhi').textContent = `${state.daGhiPhien.length} lượt phiên này`;
  el('vuaGhiCard').hidden = false;
  $('#tblVuaGhi tbody').innerHTML = state.daGhiPhien.map((r) => `
    <tr><td>${esc(r.gio)}</td><td>${esc(r.t.hoTen)}</td><td>${esc(r.t.lop)}</td>
      <td>${badgeTrangThai(r.ketLuan)}</td>
      <td>${r.t.duHangMuc ? '<span class="badge ok">đủ</span>'
        : `<span class="badge warn">còn ${r.t.conThieu.length}</span>`}</td></tr>`).join('');
}

el('btnTimHocSinh').onclick = timHocSinh;
el('btnBinhThuong').onclick = () => ghiKetQua('BINH_THUONG');

function moGhiChu(ketLuan) {
  if (!state.phieu) { toast('Chưa chọn học sinh', 'bad'); return; }
  el('ghiChuBox').hidden = false;
  el('ghiChuBox').dataset.ketLuan = ketLuan;
  el('ghiChuKham').placeholder = ketLuan === 'CAN_THEO_DOI'
    ? 'Cần theo dõi điều gì?' : 'Mô tả bất thường (bắt buộc)';
  el('ghiChuKham').focus();
}

el('btnTheoDoi').onclick = () => moGhiChu('CAN_THEO_DOI');
el('btnBatThuong').onclick = () => moGhiChu('BAT_THUONG');
el('btnHuyBatThuong').onclick = () => { el('ghiChuBox').hidden = true; };
el('btnXacNhanBatThuong').onclick = () => {
  const ketLuan = el('ghiChuBox').dataset.ketLuan;
  if (ketLuan === 'BAT_THUONG' && !el('ghiChuKham').value.trim()) {
    toast('Ghi bất thường thì phải mô tả — bàn kết luận cần thông tin này', 'bad');
    el('ghiChuKham').focus();
    return;
  }
  ghiKetQua(ketLuan);
};

/* Bàn phím: bác sĩ không phải rời tay khỏi bàn phím giữa các học sinh. */
el('qKham').onkeydown = (e) => { if (e.key === 'Enter') { e.preventDefault(); timHocSinh(); } };

document.addEventListener('keydown', (e) => {
  if (!el('tab-ban-kham').classList.contains('active') || el('khamCard').hidden) return;

  // Nhiều kết quả tìm được: bấm 1..9 để chọn.
  if (state.ketQuaTim && /^[1-9]$/.test(e.key) && document.activeElement !== el('qKham')) {
    const t = state.ketQuaTim[Number(e.key) - 1];
    if (t) { e.preventDefault(); chonPhieu(t); }
    return;
  }
  if (e.key === 'Escape') { e.preventDefault(); hocSinhMoi(); return; }
  if (e.altKey && ['1', '2', '3'].includes(e.key)) {
    e.preventDefault();
    if (e.key === '1') ghiKetQua('BINH_THUONG');
    if (e.key === '2') moGhiChu('CAN_THEO_DOI');
    if (e.key === '3') moGhiChu('BAT_THUONG');
    return;
  }
  // Enter trong ô chỉ số = ghi bình thường (đường đi của ~85% ca).
  if (e.key === 'Enter' && e.target.closest('#chiSoForm')) {
    e.preventDefault();
    ghiKetQua('BINH_THUONG');
  }
});

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
