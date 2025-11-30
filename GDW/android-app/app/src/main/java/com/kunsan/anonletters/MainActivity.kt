package com.kunsan.anonletters

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kunsan.anonletters.databinding.ActivityMainBinding

// 한국어 주석: 메인 액티비티는 상담/내역 화면으로 이동하는 허브 역할을 담당한다.
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderStatusCard()
        setupInteractions()
    }

    // 한국어 주석: 추후 서버 연동 전까지는 안내 문구를 고정으로 노출한다.
    private fun renderStatusCard() {
        binding.statusMessage.text = getString(R.string.status_body_default)
        binding.statusHint.text = getString(R.string.status_hint)
    }

    // 한국어 주석: 버튼 클릭 시 각 화면으로 이동하도록 연결한다.
    private fun setupInteractions() {
        binding.consultButton.text = getString(R.string.consult_button_label)
        binding.historyButton.text = getString(R.string.history_button_label)

        binding.consultButton.setOnClickListener {
            startActivity(Intent(this, com.kunsan.anonletters.chat.ChatActivity::class.java))
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, ConsultationHistoryActivity::class.java))
        }
    }
}
