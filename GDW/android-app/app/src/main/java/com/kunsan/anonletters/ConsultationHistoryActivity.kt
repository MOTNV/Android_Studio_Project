package com.kunsan.anonletters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.kunsan.anonletters.databinding.ActivityConsultationHistoryBinding

// 한국어 주석: 진행된 상담 세션을 요약해 보여주는 화면이다.
class ConsultationHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsultationHistoryBinding

    private val historyItems = listOf(
        ConsultationHistory(
            staffName = "김은별 교수",
            summary = "학업 스트레스로 인한 불안을 공유하고 장기 계획을 세웠습니다.",
            status = "상담 완료",
            timestamp = "2024-07-03 14:20"
        ),
        ConsultationHistory(
            staffName = "최지안 매니저",
            summary = "익명 제안을 전달하고 후속 진행 계획을 확인했습니다.",
            status = "후속 조치 중",
            timestamp = "2024-06-25 10:05"
        ),
        ConsultationHistory(
            staffName = "박시온 교수",
            summary = "밤마다 이어지는 걱정을 다루는 호흡법을 안내받았습니다.",
            status = "상담 완료",
            timestamp = "2024-06-11 09:40"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecycler()
    }

    // 한국어 주석: 공통 툴바 패턴을 적용하고 뒤로 가기를 연결한다.
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.title = getString(R.string.history_list_title)
        binding.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // 한국어 주석: 히스토리 카드 리스트를 설정한다.
    private fun setupRecycler() {
        binding.historyRecycler.adapter = ConsultationHistoryAdapter(historyItems)
    }
}

data class ConsultationHistory(
    val staffName: String,
    val summary: String,
    val status: String,
    val timestamp: String
)
