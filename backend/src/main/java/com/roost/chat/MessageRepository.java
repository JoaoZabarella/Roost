package com.roost.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Newest-first page for a channel; backed by idx_messages_channel_created.
    List<Message> findByChannelIdOrderByCreatedAtDesc(UUID channelId, Pageable pageable);
}
