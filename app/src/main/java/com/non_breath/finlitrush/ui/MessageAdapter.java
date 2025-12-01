package com.non_breath.finlitrush.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.non_breath.finlitrush.databinding.ItemMessageIncomingBinding;
import com.non_breath.finlitrush.databinding.ItemMessageOutgoingBinding;
import com.non_breath.finlitrush.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;
    private final List<Message> items = new ArrayList<>();

    public void submit(List<Message> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems != null ? newItems.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).getMessageId()
                        .equals(newItems.get(newItemPosition).getMessageId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Message oldItem = items.get(oldItemPosition);
                Message newItem = newItems.get(newItemPosition);
                return oldItem.getBody().equals(newItem.getBody())
                        && oldItem.getCategory().equals(newItem.getCategory())
                        && oldItem.getPriority().equals(newItem.getPriority())
                        && oldItem.getRecipient().equals(newItem.getRecipient())
                        && oldItem.getStatus().equals(newItem.getStatus())
                        && oldItem.isOutbound() == newItem.isOutbound()
                        && oldItem.getCreatedAt() == newItem.getCreatedAt();
            }
        });
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        diff.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isOutbound() ? TYPE_OUTGOING : TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_INCOMING) {
            ItemMessageIncomingBinding binding = ItemMessageIncomingBinding.inflate(inflater, parent, false);
            return new IncomingHolder(binding);
        }
        ItemMessageOutgoingBinding binding = ItemMessageOutgoingBinding.inflate(inflater, parent, false);
        return new OutgoingHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = items.get(position);
        if (holder instanceof IncomingHolder) {
            ((IncomingHolder) holder).bind(message);
        } else if (holder instanceof OutgoingHolder) {
            ((OutgoingHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OutgoingHolder extends RecyclerView.ViewHolder {
        private final ItemMessageOutgoingBinding binding;
        private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        OutgoingHolder(ItemMessageOutgoingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Message message) {
            binding.messageBody.setText(message.getBody());
            binding.messageMeta.setText(message.getCategory() + " • " + message.getPriority());
            binding.messageFooter.setText(message.getRecipient() + " • " + message.getStatus());
            binding.messageTime.setText(FORMAT.format(new Date(message.getCreatedAt())));
        }
    }

    static class IncomingHolder extends RecyclerView.ViewHolder {
        private final ItemMessageIncomingBinding binding;
        private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        IncomingHolder(ItemMessageIncomingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Message message) {
            binding.messageBody.setText(message.getBody());
            binding.messageMeta.setText(message.getCategory() + " • " + message.getPriority());
            binding.messageFooter.setText(message.getSenderAlias());
            binding.messageTime.setText(FORMAT.format(new Date(message.getCreatedAt())));
        }
    }
}
