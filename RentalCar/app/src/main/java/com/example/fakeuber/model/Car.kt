package com.example.fakeuber.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes

/**
 * Một chiếc xe cho thuê. Chỉ nằm trong bộ nhớ (xem [CarRepository]), không ghi xuống đĩa.
 *
 * Tự viết [Parcelable] (thay vì dùng @Parcelize) để truyền xe qua Intent từ MainActivity
 * sang RentActivity. Nguyên tắc: các field được ghi vào [Parcel] theo một thứ tự cố định,
 * và bên nhận phải đọc ra ĐÚNG thứ tự đó.
 *
 * @property id mã cố định để tìm lại xe (yêu thích, đơn thuê).
 * @property rating điểm đánh giá thang 1-5.
 * @property costPerDay giá thuê mỗi ngày (credit).
 * @property imageRes id ảnh xe trong res/drawable-nodpi.
 */
data class Car(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val type: String,
    val seats: Int,
    val fuel: String,
    val km: Int,
    val rating: Float,
    val costPerDay: Int,
    @param:DrawableRes val imageRes: Int,
) : Parcelable {

    /** Dựng lại Car từ Parcel. Phải đọc đúng thứ tự mà [writeToParcel] đã ghi. */
    private constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        brand = parcel.readString().orEmpty(),
        model = parcel.readString().orEmpty(),
        year = parcel.readInt(),
        type = parcel.readString().orEmpty(),
        seats = parcel.readInt(),
        fuel = parcel.readString().orEmpty(),
        km = parcel.readInt(),
        rating = parcel.readFloat(),
        costPerDay = parcel.readInt(),
        imageRes = parcel.readInt(),
    )

    /** Ví dụ "Toyota Vios 1.5G": dùng cho tiêu đề, thông báo và nhãn accessibility. */
    val displayName: String get() = "$brand $model"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(brand)
        parcel.writeString(model)
        parcel.writeInt(year)
        parcel.writeString(type)
        parcel.writeInt(seats)
        parcel.writeString(fuel)
        parcel.writeInt(km)
        parcel.writeFloat(rating)
        parcel.writeInt(costPerDay)
        parcel.writeInt(imageRes)
    }

    override fun describeContents(): Int = 0

    /** Android dùng CREATOR để dựng lại Car từ Parcel ở phía nhận. */
    companion object CREATOR : Parcelable.Creator<Car> {
        override fun createFromParcel(parcel: Parcel): Car = Car(parcel)
        override fun newArray(size: Int): Array<Car?> = arrayOfNulls(size)
    }
}
