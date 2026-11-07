package com.example.fakeuber.model

import com.example.fakeuber.R

/**
 * 5 xe của công ty, chỉ lưu trong bộ nhớ (đề không cho lưu xuống đĩa).
 *
 * [cars] là List chỉ-đọc: tìm kiếm và sắp xếp luôn làm trên bản sao nên dữ liệu gốc không đổi.
 * Giá tính bằng credit (1 credit = 10.000đ, theo giá thuê xe tự lái ở Việt Nam năm 2026).
 * Số km và rating chỉ mang tính minh hoạ.
 */
object CarRepository {

    /** Số dư ban đầu của mỗi người dùng. */
    const val START_BALANCE = 500

    /** Số tiền tối đa cho một đơn thuê (credit). */
    const val RENTAL_LIMIT = 400

    // Thứ tự cột: id, hãng, model, năm, loại xe, số chỗ, nhiên liệu, km, rating, giá/ngày, ảnh.
    val cars: List<Car> = listOf(
        Car(1, "VinFast", "VF 5 Plus", 2025, "Electric SUV", 5, "Electric", 18_500, 4.6f, 60, R.drawable.car_vinfast_vf5),
        Car(2, "Toyota", "Vios 1.5G", 2024, "Sedan", 5, "Petrol", 41_000, 4.8f, 80, R.drawable.car_toyota_vios),
        Car(3, "Mazda", "Mazda3 1.5", 2022, "Sedan", 5, "Petrol", 74_000, 4.7f, 90, R.drawable.car_mazda3),
        Car(4, "Mitsubishi", "Xpander AT", 2023, "MPV", 7, "Petrol", 58_000, 4.5f, 100, R.drawable.car_mitsubishi_xpander),
        Car(5, "Toyota", "Fortuner 2.4AT", 2021, "SUV", 7, "Diesel", 92_000, 4.9f, 110, R.drawable.car_toyota_fortuner),
    )
}
