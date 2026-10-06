package com.medilink.checkup.domain;

/**
 * Vòng đời đợt khám. Mỗi bước chuyển là một chốt kiểm soát lỗi:
 * NHAP → (chuẩn bị: sinh phiếu + checklist) → DANG_CHUAN_BI → (kiểm kê đủ) → SAN_SANG
 * → (đến trường, setup) → DANG_KHAM → (mọi phiếu đã kết luận) → HOAN_THANH
 */
public enum TrangThaiDotKham { NHAP, DANG_CHUAN_BI, SAN_SANG, DANG_KHAM, HOAN_THANH }
