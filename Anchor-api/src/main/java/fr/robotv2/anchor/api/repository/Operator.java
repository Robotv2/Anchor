package fr.robotv2.anchor.api.repository;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of comparison operators used in database queries.
 * <p>
 * These operators are used with {@link QueryBuilder} to construct WHERE clauses
 * for filtering query results. Each operator maps to a corresponding SQL
 * comparison symbol that is understood by all supported database dialects.
 * </p>
 *
 * @since 1.0
 * @see QueryBuilder
 */
public enum Operator {

    /**
     * Greater than comparison operator.
     * <p>
     * SQL equivalent: {@code >}
     * </p>
     */
    GREATER_THAN(">"),

    /**
     * Less than comparison operator.
     * <p>
     * SQL equivalent: {@code <}
     * </p>
     */
    LESS_THAN("<"),

    /**
     * Greater than or equal comparison operator.
     * <p>
     * SQL equivalent: {@code >=}
     * </p>
     */
    GREATER_THAN_OR_EQUAL(">="),

    /**
     * Less than or equal comparison operator.
     * <p>
     * SQL equivalent: {@code <=}
     * </p>
     */
    LESS_THAN_OR_EQUAL("<="),

    /**
     * Equality comparison operator.
     * <p>
     * SQL equivalent: {@code =}
     * </p>
     */
    EQUAL("="),

    /**
     * Inequality comparison operator.
     * <p>
     * SQL equivalent: {@code !=} or {@code <>} depending on database dialect
     * </p>
     */
    NOT_EQUAL("!="),
    ;

    private final String symbol;

    /**
     * Creates an Operator with the specified SQL symbol.
     *
     * @param symbol the representation of this operator, must not be {@code null}
     */
    Operator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the SQL symbol representation of this operator.
     * <p>
     * This symbol is used when constructing SQL WHERE clauses.
     * </p>
     *
     * @return the SQL symbol, never {@code null}
     */
    @NotNull
    public String getSymbol() {
        return symbol;
    }
}
