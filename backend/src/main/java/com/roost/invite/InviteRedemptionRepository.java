package com.roost.invite;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRedemptionRepository extends JpaRepository<InviteRedemption, UUID> {
}
