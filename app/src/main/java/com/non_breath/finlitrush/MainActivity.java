package com.non_breath.finlitrush;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.TransitionManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.non_breath.finlitrush.chat.ChatActivity;
import com.non_breath.finlitrush.data.ProfessorDirectory;
import com.non_breath.finlitrush.databinding.ActivityMainBinding;
import com.non_breath.finlitrush.model.AnalysisResult;
import com.non_breath.finlitrush.settings.SettingsActivity;
import com.non_breath.finlitrush.ui.MessageViewModel;
import com.non_breath.finlitrush.ui.RecommendationAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MessageViewModel viewModel;
    private RecommendationAdapter recommendationAdapter;
    private Step currentStep = Step.START;
    private final Handler analysisHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingAnalysis;
    private final List<ProfessorDirectory.Professor> remoteProfessors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        initRecommendationList();
        initViewModel();
        loadProfessorsFromFirestore();
        bindListeners();
        renderStep();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshAnonId();
        }
    }

    private void initRecommendationList() {
        recommendationAdapter = new RecommendationAdapter();
        binding.recommendList.setLayoutManager(new LinearLayoutManager(this));
        binding.recommendList.setAdapter(recommendationAdapter);
        binding.recommendList.setHasFixedSize(true);
        binding.recommendList.setItemAnimator(null);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        viewModel.getAnalysis().observe(this, this::renderAnalysis);
        viewModel.getAnonId().observe(this, id -> {
            binding.anonId.setText(id);
            binding.anonIdStart.setText(id);
        });

        recommendationAdapter.setOnChatClickListener(professor -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_RECIPIENT_NAME, professor.name);
            intent.putExtra(ChatActivity.EXTRA_RECIPIENT_EMAIL, professor.email);
            startActivity(intent);
        });
    }

    private void bindListeners() {
        binding.buttonStart.setOnClickListener(v -> {
            currentStep = Step.ASSIST;
            renderStep();
        });
        binding.buttonHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, com.non_breath.finlitrush.chat.ChatListActivity.class));
        });

        binding.sendButton.setOnClickListener(v -> {
            String text = binding.messageInput.getText() != null ? binding.messageInput.getText().toString().trim() : "";
            if (text.isEmpty()) {
                android.widget.Toast.makeText(this, getString(R.string.error_empty_message), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.analyzeDraft(text);
            currentStep = Step.CHAT;
            renderStep();
        });
        binding.messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceAnalysis(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void renderAnalysis(AnalysisResult result) {
        binding.routingRecipient.setText(result.getRecommendedRecipient());
        binding.routingCategory.setText(result.getCategory());
        binding.routingPriority.setText(result.getPriority());
        binding.routingKeywords.setText(result.getKeywords().isEmpty()
                ? getString(R.string.analysis_keyword_placeholder)
                : String.join(", ", result.getKeywords()));
        updateRecommendations(result);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderStep() {
        TransitionManager.beginDelayedTransition((android.view.ViewGroup) binding.getRoot());
        binding.cardStart.setVisibility(currentStep == Step.START ? View.VISIBLE : View.GONE);
        binding.groupAssist.setVisibility(currentStep == Step.ASSIST ? View.VISIBLE : View.GONE);
        binding.groupChat.setVisibility(currentStep == Step.CHAT ? View.VISIBLE : View.GONE);
        renderNavigation();
    }

    private void renderNavigation() {
        if (currentStep == Step.START) {
            binding.toolbar.setNavigationIcon(null);
            binding.toolbar.setNavigationOnClickListener(null);
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back);
            binding.toolbar.setNavigationOnClickListener(v -> {
                currentStep = Step.START;
                renderStep();
            });
        }
    }

    private void debounceAnalysis(String text) {
        if (pendingAnalysis != null) {
            analysisHandler.removeCallbacks(pendingAnalysis);
        }
        pendingAnalysis = () -> viewModel.analyzeDraft(text);
        analysisHandler.postDelayed(pendingAnalysis, 200);
    }

    private enum Step {
        START, ASSIST, CHAT
    }

    private void updateRecommendations(AnalysisResult result) {
        if (currentStep != Step.CHAT || result == null) {
            binding.emptyState.setVisibility(View.GONE);
            recommendationAdapter.submit(Collections.emptyList());
            return;
        }
        String targetName = result.getRecommendedRecipient() != null ? result.getRecommendedRecipient().trim() : "";
        List<ProfessorDirectory.Professor> all = remoteProfessors.isEmpty()
                ? ProfessorDirectory.getAll()
                : new ArrayList<>(remoteProfessors);
        List<ProfessorDirectory.Professor> picks = new ArrayList<>();

        ProfessorDirectory.Professor primary = findByName(all, targetName);
        if (primary != null) {
            picks.add(primary);
        }

        List<String> keywords = result.getKeywords();
        if (keywords != null && !keywords.isEmpty()) {
            for (ProfessorDirectory.Professor p : all) {
                if (picks.contains(p)) continue;
                if (matchesKeywords(p, keywords)) {
                    picks.add(p);
                }
            }
        }

        if (picks.isEmpty()) {
            for (int i = 0; i < Math.min(2, all.size()); i++) {
                picks.add(all.get(i));
            }
        }

        recommendationAdapter.submit(picks);
        binding.emptyState.setVisibility(picks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private ProfessorDirectory.Professor findByName(List<ProfessorDirectory.Professor> all, String name) {
        if (name == null || name.isEmpty()) return null;
        String norm = name.replace("교수", "").trim();
        for (ProfessorDirectory.Professor p : all) {
            String base = p.name.replace("교수", "").trim();
            if (base.equals(norm)) return p;
            if (p.englishName != null && p.englishName.toLowerCase().contains(norm.toLowerCase())) return p;
        }
        return null;
    }

    private boolean matchesKeywords(ProfessorDirectory.Professor p, List<String> keywords) {
        if (keywords == null || keywords.isEmpty() || p.researchAreas == null) return false;
        String fields = String.join(" ", p.researchAreas).toLowerCase();
        for (String kw : keywords) {
            String low = kw.toLowerCase();
            if (fields.contains(low)) return true;
            if (containsAlias(low, fields, "블록체인", "blockchain")) return true;
            if (containsAlias(low, fields, "iot", "i.o.t")) return true;
            if (containsAlias(low, fields, "nlp", "자연어")) return true;
            if (containsAlias(low, fields, "database", "데이터베이스", "db")) return true;
            if (containsAlias(low, fields, "vision", "컴퓨터 비전")) return true;
            if (containsAlias(low, fields, "graphics", "그래픽스")) return true;
            if (containsAlias(low, fields, "health", "헬스케어", "의료")) return true;
            if (containsAlias(low, fields, "reinforcement", "강화학습")) return true;
        }
        return false;
    }

    private boolean containsAlias(String kw, String fields, String... aliases) {
        for (String a : aliases) {
            String alias = a.toLowerCase();
            if (kw.contains(alias) || fields.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private void loadProfessorsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("professors")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    remoteProfessors.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ProfessorDirectory.Professor p = mapProfessor(doc);
                        if (p != null) {
                            remoteProfessors.add(p);
                        }
                    }
                    renderAnalysis(viewModel.getAnalysis().getValue());
                });
    }

    private ProfessorDirectory.Professor mapProfessor(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String englishName = doc.getString("englishName");
        String title = doc.getString("title");
        List<String> researchAreas = (List<String>) doc.get("researchAreas");
        String lab = doc.getString("lab");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String location = doc.getString("location");
        if (name == null || email == null) return null;
        return new ProfessorDirectory.Professor(
                name,
                englishName != null ? englishName : "",
                title != null ? title : "",
                researchAreas != null ? researchAreas : new ArrayList<>(),
                lab != null ? lab : "",
                email,
                phone != null ? phone : "",
                location != null ? location : ""
        );
    }
}
