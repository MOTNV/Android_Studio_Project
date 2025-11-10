package com.kunsan.anonletters

import android.graphics.drawable.GradientDrawable
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.kunsan.anonletters.databinding.ItemStaffMemberBinding

// 한국어 주석: 상담 담당자 카드를 묶어주는 RecyclerView 어댑터이다.
class StaffAdapter(
    private val items: List<StaffMember>,
    private val onItemClick: (StaffMember) -> Unit
) : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemStaffMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StaffViewHolder(
        private val binding: ItemStaffMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 한국어 주석: 데이터에 맞춰 텍스트와 배지 컬러를 갱신한다.
        fun bind(item: StaffMember) {
            binding.staffName.text = item.name
            binding.staffTitle.text = item.title
            binding.staffSpecialty.text = item.specialty
            (binding.avatarHolder.background as? GradientDrawable)?.setColor(item.badgeColor)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
