package com.non_breath.finlitrush.chat;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.non_breath.finlitrush.databinding.ItemChatSummaryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.Holder> {

    public interface OnClickListener {
        void onClick(ChatListActivity.ChatSummary item);
    }

    private final List<ChatListActivity.ChatSummary> items = new ArrayList<>();
    private final OnClickListener listener;
    private final SimpleDateFormat format = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

    public ChatListAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<ChatListActivity.ChatSummary> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatSummaryBinding b = ItemChatSummaryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(b, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), format);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemChatSummaryBinding binding;
        private final OnClickListener listener;

        Holder(ItemChatSummaryBinding binding, OnClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ChatListActivity.ChatSummary item, SimpleDateFormat format) {
            binding.peerName.setText(item.peerId);
            binding.lastMessage.setText(item.lastMessage);
            binding.time.setText(format.format(new Date(item.lastAt)));
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
        }
    }
}
