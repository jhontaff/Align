package com.jet.align.finance.enums;

import lombok.Getter;

@Getter
public enum Category {
    // Gastos
    FOOD(TransactionType.EXPENSE), TRANSPORT(TransactionType.EXPENSE), HOUSING(TransactionType.EXPENSE), HEALTH(TransactionType.EXPENSE),
    ENTERTAINMENT(TransactionType.EXPENSE), EDUCATION(TransactionType.EXPENSE), SHOPPING(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE), OTHER_EXPENSE(TransactionType.EXPENSE),
    // Ingresos
    SALARY(TransactionType.INCOME), FREELANCE(TransactionType.INCOME), INVESTMENT(TransactionType.INCOME),
    GIFT(TransactionType.INCOME), OTHER_INCOME(TransactionType.INCOME);

    private final TransactionType type;
    Category(TransactionType type) {
        this.type = type;
    }

}


