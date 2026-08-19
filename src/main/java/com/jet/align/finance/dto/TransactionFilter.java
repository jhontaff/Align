package com.jet.align.finance.dto;

import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;

import java.time.LocalDate;

public record TransactionFilter (
        TransactionType type,
        Category category,
        LocalDate from,
        LocalDate to
){

}
