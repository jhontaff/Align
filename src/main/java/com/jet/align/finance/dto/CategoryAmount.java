package com.jet.align.finance.dto;

import com.jet.align.finance.enums.Category;

import java.math.BigDecimal;

public record CategoryAmount(
        Category category,
        BigDecimal amount,
        BigDecimal percentage)
{ }
