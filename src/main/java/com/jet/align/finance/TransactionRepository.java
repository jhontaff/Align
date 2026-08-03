package com.jet.align.finance;

import com.jet.align.finance.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<List<Transaction>> findByIdAndUserId(UUID id, UUID userId);
    Optional<Transaction> search(UUID id, UUID userId);

}
