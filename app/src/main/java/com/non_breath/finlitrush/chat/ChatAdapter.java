package com.non_breath.finlitrush.chat;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.non_breath.finlitrush.databinding.ItemChatIncomingBinding;
import com.non_breath.finlitrush.databinding.ItemChatOutgoingBinding;
import com.non_breath.finlitrush.auth.RoleManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;
    private final List<ChatMessage> items = new ArrayList<>();
    private final String meId;
    private final SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private RoleManager.Role myRole = RoleManager.Role.STUDENT;
    private final Map<String, Integer> anonNumberMap = new HashMap<>();
    private int nextAnonNumber = 1;

    public ChatAdapter(String meId) {
        this.meId = meId;
    }

    public void setRole(RoleManager.Role role) {
        this.myRole = role;
    }

    public void submit(List<ChatMessage> messages) {
        items.clear();
        if (messages != null) items.addAll(messages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).senderId != null && items.get(position).senderId.equals(meId) ? TYPE_OUTGOING : TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_OUTGOING) {
            ItemChatOutgoingBinding b = ItemChatOutgoingBinding.inflate(inflater, parent, false);
            return new OutHolder(b);
        }
        ItemChatIncomingBinding b = ItemChatIncomingBinding.inflate(inflater, parent, false);
        return new InHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        if (holder instanceof OutHolder) {
            ((OutHolder) holder).bind(msg, format);
        } else if (holder instanceof InHolder) {
            String displayName = formatSenderName(msg.senderName, msg.senderId);
            ((InHolder) holder).bind(msg, format, displayName);
        }
    }

    private String formatSenderName(String senderName, String senderId) {
        // 교수/관리자가 볼 때: 익명 ID를 번호로 변환
        if (myRole == RoleManager.Role.RESPONDENT || myRole == RoleManager.Role.ADMIN) {
            if (senderName != null && senderName.startsWith("anon-")) {
                if (!anonNumberMap.containsKey(senderName)) {
                    anonNumberMap.put(senderName, nextAnonNumber++);
                }
                return "익명 " + anonNumberMap.get(senderName);
            }
            // senderId도 확인
            if (senderId != null && senderId.startsWith("anon-")) {
                if (!anonNumberMap.containsKey(senderId)) {
                    anonNumberMap.put(senderId, nextAnonNumber++);
                }
                return "익명 " + anonNumberMap.get(senderId);
            }
        }
        // 그 외: 원래 이름 사용
        return senderName != null ? senderName : "";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OutHolder extends RecyclerView.ViewHolder {
        private final ItemChatOutgoingBinding binding;

        OutHolder(ItemChatOutgoingBinding b) {
            super(b.getRoot());
            binding = b;
        }

        void bind(ChatMessage msg, SimpleDateFormat format) {
            binding.messageBody.setText(msg.text);
            binding.messageTime.setText(format.format(new Date(msg.createdAt)));
        }
    }

    static class InHolder extends RecyclerView.ViewHolder {
        private final ItemChatIncomingBinding binding;

        InHolder(ItemChatIncomingBinding b) {
            super(b.getRoot());
            binding = b;
        }

        void bind(ChatMessage msg, SimpleDateFormat format, String displayName) {
            binding.messageBody.setText(msg.text);
            binding.messageMeta.setText(displayName);
            binding.messageTime.setText(format.format(new Date(msg.createdAt)));
        }
    }
}
