package SAT;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CNFNormalizer {
    private CNFNormalizer() {}

    public static ArrayList<Clause> normalize(ArrayList<Clause> rawClauses) {
        ArrayList<Clause> normalized = new ArrayList<>();
        if (rawClauses == null) {
            return normalized;
        }

        for (Clause clause : rawClauses) {
            Clause cleaned = cleanClause(clause);
            if (cleaned != null) {
                normalized.add(cleaned);
            }
        }

        return normalized;
    }

    public static boolean isValidCNF(ArrayList<Clause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return false;
        }

        for (Clause clause : clauses) {
            if (clause == null || clause.clause == null || clause.clause.isEmpty()) {
                return false;
            }

            Map<Variable, Boolean> seen = new LinkedHashMap<>();
            for (Literal literal : clause.clause) {
                if (literal == null || literal.var == null) {
                    return false;
                }

                literal.var.on_off = null;
                Boolean existing = seen.get(literal.var);
                if (existing != null && existing != literal.neg) {
                    return false;
                }
                seen.put(literal.var, literal.neg);
            }
        }

        return true;
    }

    private static Clause cleanClause(Clause clause) {
        if (clause == null || clause.clause == null) {
            return null;
        }

        Clause cleaned = new Clause();
        Map<Variable, Boolean> seen = new LinkedHashMap<>();

        for (Literal literal : clause.clause) {
            if (literal == null || literal.var == null) {
                continue;
            }

            literal.var.on_off = null;
            Boolean existing = seen.get(literal.var);
            if (existing != null) {
                if (existing != literal.neg) {
                    return null;
                }
                continue;
            }

            seen.put(literal.var, literal.neg);
            cleaned.clause.add(new Literal(literal.var, literal.neg));
        }

        return cleaned.clause.isEmpty() ? null : cleaned;
    }
}
