package com.kunsan.anonletters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kunsan.anonletters.databinding.ItemConsultationHistoryBinding

// 한국어 주석: 상담 히스토리 카드를 바인딩하는 어댑터이다.
class ConsultationHistoryAdapter(
    private val items: List<ConsultationHistory>
) : RecyclerView.Adapter<ConsultationHistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemConsultationHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(
        private val binding: ItemConsultationHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 한국어 주석: 상담 상태에 따라 배경 색상을 다르게 표현한다.
        fun bind(item: ConsultationHistory) {
            binding.historyStaff.text = item.staffName
            binding.historyTimestamp.text = item.timestamp
            binding.historySummary.text = item.summary
            binding.historyStatus.text = item.status

            val badgeColor = if (item.status.contains("완료")) {
                Color.parseColor("#DCFCE7")
            } else {
                Color.parseColor("#FEF3C7")
            }
            (binding.historyStatus.background as? GradientDrawable)?.setColor(badgeColor)
        }
    }
}
