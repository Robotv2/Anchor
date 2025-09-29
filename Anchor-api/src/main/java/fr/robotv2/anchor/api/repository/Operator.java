package fr.robotv2.anchor.api.repository;

import org.jetbrains.annotations.NotNull;

public enum Operator {

    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("="),
    NOT_EQUAL("!="),
    ;

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    @NotNull
    public String getSymbol() {
        return symbol;
    }
}
