package com.roost.server;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<Server, UUID> {

    List<Server> findByOwnerId(UUID ownerId);
}
