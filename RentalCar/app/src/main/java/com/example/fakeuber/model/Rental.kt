package com.example.fakeuber.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Một đơn thuê đã lưu. RentActivity tạo ra khi bấm Save và gửi về MainActivity qua
 * Intent kết quả, nên cũng phải là Parcelable.
 *
 * @property car xe được thuê (Parcelable nằm trong Parcelable).
 * @property total tổng tiền = costPerDay x days.
 * @property idPhotoUri Uri (dạng chuỗi) của ảnh CCCD người dùng đã chọn.
 * @property licencePhotoUri Uri (dạng chuỗi) của ảnh bằng lái.
 */
data class Rental(
    val car: Car,
    val days: Int,
    val total: Int,
    val idPhotoUri: String,
    val licencePhotoUri: String,
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        car = Car.createFromParcel(parcel),
        days = parcel.readInt(),
        total = parcel.readInt(),
        idPhotoUri = parcel.readString().orEmpty(),
        licencePhotoUri = parcel.readString().orEmpty(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        car.writeToParcel(parcel, flags) // ghi các field của xe trước, rồi tới field của Rental
        parcel.writeInt(days)
        parcel.writeInt(total)
        parcel.writeString(idPhotoUri)
        parcel.writeString(licencePhotoUri)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Rental> {
        override fun createFromParcel(parcel: Parcel): Rental = Rental(parcel)
        override fun newArray(size: Int): Array<Rental?> = arrayOfNulls(size)
    }
}
