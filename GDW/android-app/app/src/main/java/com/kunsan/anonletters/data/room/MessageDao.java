package com.kunsan.anonletters.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getAllMessages();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);
}
