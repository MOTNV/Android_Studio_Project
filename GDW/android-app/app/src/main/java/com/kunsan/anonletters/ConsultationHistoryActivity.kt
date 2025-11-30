package com.kunsan.anonletters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import com.kunsan.anonletters.data.ChatRepository
import com.kunsan.anonletters.data.room.MessageEntity
import com.kunsan.anonletters.databinding.ActivityConsultationHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 한국어 주석: 진행된 상담 세션을 요약해 보여주는 화면이다.
// Room DB에서 실제 대화 내역을 가져오도록 수정됨.
class ConsultationHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsultationHistoryBinding
    private lateinit var repository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ChatRepository(application)

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
        val adapter = ConsultationHistoryAdapter(emptyList())
        binding.historyRecycler.adapter = adapter

        repository.allMessages.observe(this, Observer { messages ->
            val historyItems = messages.map { entity ->
                ConsultationHistory(
                    staffName = "익명 상담사", // 실제로는 상대방 ID나 이름을 매핑해야 함
                    summary = entity.encryptedContent, // 복호화 필요하지만 데모용으로 암호문 표시
                    status = if (entity.isSentByMe) "보냄" else "받음",
                    timestamp = formatTimestamp(entity.timestamp)
                )
            }
            // Adapter가 List<ConsultationHistory>를 받도록 되어있으므로, 
            // 실제로는 Adapter도 LiveData를 관찰하거나 update 메서드를 추가해야 함.
            // 여기서는 간단히 새 어댑터를 설정함 (비효율적일 수 있음)
            binding.historyRecycler.adapter = ConsultationHistoryAdapter(historyItems)
        })
    }

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
