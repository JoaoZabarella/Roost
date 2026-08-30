package com.roost.server;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerMemberRepository extends JpaRepository<ServerMember, UUID> {

    Optional<ServerMember> findByServerIdAndUserId(UUID serverId, UUID userId);

    List<ServerMember> findByServerId(UUID serverId);

    List<ServerMember> findByUserId(UUID userId);

    boolean existsByServerIdAndUserId(UUID serverId, UUID userId);
}
