package com.example.fakeuber.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fakeuber.R
import com.example.fakeuber.databinding.ItemFavChipBinding
import com.example.fakeuber.model.Car

/**
 * Adapter cho RecyclerView ngang ở mục Favourites. RecyclerView chỉ tạo đủ số chip vừa màn hình
 * rồi tái sử dụng khi cuộn; adapter có 2 việc: tạo chip (onCreateViewHolder) và đổ dữ liệu
 * một xe vào chip (onBindViewHolder).
 *
 * @param isRented hỏi xe có đang được thuê không (nếu có thì hiện "Rented").
 * @param onClick được gọi khi chạm vào chip.
 */
class FavouriteAdapter(
    private val isRented: (Car) -> Boolean,
    private val onClick: (Car) -> Unit,
) : RecyclerView.Adapter<FavouriteAdapter.ChipHolder>() {

    private var cars: List<Car> = emptyList()

    /** Thay danh sách mới. Tối đa 5 phần tử nên làm mới toàn bộ là đơn giản nhất. */
    fun submit(newCars: List<Car>) {
        cars = newCars
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cars.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipHolder =
        ChipHolder(ItemFavChipBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ChipHolder, position: Int) = holder.bind(cars[position])

    /** Giữ các view của một chip. "inner" để dùng được isRented và onClick của adapter. */
    inner class ChipHolder(private val b: ItemFavChipBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(car: Car) {
            val ctx = b.root.context
            val rented = isRented(car)
            b.imgFav.setImageResource(car.imageRes)
            b.tvFavName.text = car.model
            b.tvFavSub.text = if (rented) ctx.getString(R.string.fav_chip_rented)
            else ctx.getString(R.string.fav_chip_price, car.costPerDay)
            b.root.alpha = if (rented) 0.6f else 1f // làm mờ = xe đang được thuê
            b.root.contentDescription = ctx.getString(R.string.cd_fav_chip, car.displayName)
            b.root.setOnClickListener { onClick(car) }
        }
    }
}
