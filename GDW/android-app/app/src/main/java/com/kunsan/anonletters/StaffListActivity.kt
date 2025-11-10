package com.kunsan.anonletters

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.snackbar.Snackbar
import com.kunsan.anonletters.databinding.ActivityStaffListBinding

// 한국어 주석: 상담 담당자 정보를 노출하고 채팅 진입을 안내하는 화면이다.
class StaffListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffListBinding

    private val staffMembers = listOf(
        StaffMember(
            name = "김은별 교수",
            title = "상담센터장",
            specialty = "학업/생활 심층상담",
            badgeColor = Color.parseColor("#A78BFA")
        ),
        StaffMember(
            name = "박시온 교수",
            title = "정신건강 전담",
            specialty = "불안 · 우울 케어",
            badgeColor = Color.parseColor("#60A5FA")
        ),
        StaffMember(
            name = "최지안 매니저",
            title = "학생지원팀",
            specialty = "익명 제보/제안",
            badgeColor = Color.parseColor("#34D399")
        ),
        StaffMember(
            name = "이서준 매니저",
            title = "IT 운영담당",
            specialty = "시스템/보안 문의",
            badgeColor = Color.parseColor("#FBBF24")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecycler()
    }

    // 한국어 주석: 툴바 내비게이션을 적용해 뒤로 가기를 처리한다.
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.title = getString(R.string.staff_list_title)
        binding.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // 한국어 주석: 상담 대상자 목록을 스크롤 리스트로 구성하고 클릭 시 안내한다.
    private fun setupRecycler() {
        val adapter = StaffAdapter(staffMembers) { member ->
            Snackbar.make(
                binding.root,
                getString(R.string.chat_placeholder, member.name),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        binding.staffRecycler.adapter = adapter
    }
}

data class StaffMember(
    val name: String,
    val title: String,
    val specialty: String,
    val badgeColor: Int
)
