package com.pnkx.service.impl;

import com.pnkx.domain.po.PxBookkeepingBudget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PxBookkeepingBudgetServiceImplTest {
    @Test
    void warningStartsAtNinetyPercent() {
        PxBookkeepingBudget budget = new PxBookkeepingBudget();
        budget.setPercent(89);
        assertFalse(PxBookkeepingBudgetServiceImpl.shouldWarn(budget));
        budget.setPercent(90);
        assertTrue(PxBookkeepingBudgetServiceImpl.shouldWarn(budget));
    }

    @Test
    void exceededBudgetDoesNotDuplicateWarningLevel() {
        PxBookkeepingBudget budget = new PxBookkeepingBudget();
        budget.setPercent(120);
        budget.setExceeded(true);
        assertFalse(PxBookkeepingBudgetServiceImpl.shouldWarn(budget));
    }
}
