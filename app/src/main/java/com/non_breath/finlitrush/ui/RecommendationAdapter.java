package com.non_breath.finlitrush.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.non_breath.finlitrush.data.ProfessorDirectory;
import com.non_breath.finlitrush.databinding.ItemRecommendationBinding;

import java.util.ArrayList;
import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.Holder> {

    private final List<ProfessorDirectory.Professor> items = new ArrayList<>();
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChat(ProfessorDirectory.Professor professor);
    }

    public void setOnChatClickListener(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<ProfessorDirectory.Professor> professors) {
        items.clear();
        if (professors != null) {
            items.addAll(professors);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemRecommendationBinding binding = ItemRecommendationBinding.inflate(inflater, parent, false);
        Holder holder = new Holder(binding);
        holder.setOnChatClickListener(listener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemRecommendationBinding binding;
        private OnChatClickListener listener;

        Holder(ItemRecommendationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProfessorDirectory.Professor professor) {
            binding.recName.setText(professor.title + " " + professor.name);
            binding.recField.setText(professor.researchAreas == null ? "" : String.join(", ", professor.researchAreas));
            binding.recLab.setText(professor.lab != null ? professor.lab : "");
            binding.recEmail.setText(professor.email != null ? professor.email : "");
            binding.recOffice.setText(professor.location != null ? professor.location : "");
            binding.chatButton.setOnClickListener(v -> {
                if (listener != null) listener.onChat(professor);
            });
        }

        void setOnChatClickListener(OnChatClickListener listener) {
            this.listener = listener;
        }
    }
}
