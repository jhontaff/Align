package com.jet.align.finance;

import com.jet.align.finance.dto.TransactionRequest;
import com.jet.align.finance.dto.TransactionResponse;
import com.jet.align.finance.dto.TransactionUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    Transaction toTransaction(TransactionRequest request);

    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "type", ignore = true)
    void updateTransaction(TransactionUpdateRequest request, @MappingTarget Transaction transaction);
}
