package com.non_breath.finlitrush.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    LiveData<List<MessageEntity>> observeAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity entity);

    @Query("DELETE FROM messages WHERE expiresAt <= :nowMillis")
    int deleteExpired(long nowMillis);
}
