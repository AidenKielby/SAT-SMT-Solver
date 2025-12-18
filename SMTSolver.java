import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import SAT.Clause;
import SAT.Literal;
import SAT.Variable;
import SMT.Atom;
import SMT.SMTClause;
import SMT.SMTLiteral;
import SMT.SMTOperator;

public class SMTSolver {

    private static final Set<String> MULTIPLICATIVE_OPS = Set.of("*", "/", "%");
    private static final Set<String> ADDITIVE_OPS = Set.of("+", "-");
    private static final Set<String> COMPARISON_OPS = Set.of("<", "<=", ">", ">=");
    private static final Set<String> EQUALITY_OPS = Set.of("=", "==", "!=", "<>");
    private static final Map<SMTClause, ClauseEvaluationCache> CLAUSE_EVAL_CACHE = new IdentityHashMap<>();

    public static void main(String[] args){

        // Variables
        SMTLiteral a = new SMTLiteral(false, 0, "a");
        SMTLiteral b = new SMTLiteral(false, 0, "b");
        SMTLiteral c = new SMTLiteral(false, 0, "c");
        SMTLiteral d = new SMTLiteral(false, 0, "d");
        SMTLiteral e = new SMTLiteral(false, 0, "e");
        SMTLiteral f = new SMTLiteral(false, 0, "f");

        SMTLiteral const3 = new SMTLiteral(true, 3, "c3");
        SMTLiteral const5 = new SMTLiteral(true, 5, "c5");
        SMTLiteral const8 = new SMTLiteral(true, 8, "c8");
        SMTLiteral const10 = new SMTLiteral(true, 10, "c10");

        // Clauses
        SMTClause cl1 = new SMTClause(); // a < 10
        cl1.literals = new ArrayList<>(Arrays.asList(a, const10));
        cl1.operators = new ArrayList<>(List.of(new SMTOperator("<")));

        SMTClause cl2 = new SMTClause(); // b > a
        cl2.literals = new ArrayList<>(Arrays.asList(b, a));
        cl2.operators = new ArrayList<>(List.of(new SMTOperator(">")));

        SMTClause cl3 = new SMTClause(); // c = b
        cl3.literals = new ArrayList<>(Arrays.asList(c, b));
        cl3.operators = new ArrayList<>(List.of(new SMTOperator("=")));

        SMTClause cl4 = new SMTClause(); // d >= c
        cl4.literals = new ArrayList<>(Arrays.asList(d, c));
        cl4.operators = new ArrayList<>(List.of(new SMTOperator(">=")));

        SMTClause cl5 = new SMTClause(); // e <= d
        cl5.literals = new ArrayList<>(Arrays.asList(e, d));
        cl5.operators = new ArrayList<>(List.of(new SMTOperator("<=")));

        SMTClause cl6 = new SMTClause(); // f = 5 OR a = 3
        SMTClause eq1 = new SMTClause();
        eq1.literals = new ArrayList<>(Arrays.asList(f, const5));
        eq1.operators = new ArrayList<>(List.of(new SMTOperator("=")));

        SMTClause eq2 = new SMTClause();
        eq2.literals = new ArrayList<>(Arrays.asList(a, const3));
        eq2.operators = new ArrayList<>(List.of(new SMTOperator("=")));

        // Atom with & and | operators
        Atom atom = new Atom(
            new ArrayList<>(Arrays.asList(cl1, cl2, cl3, cl4, cl5, eq1, eq2)),
            new ArrayList<>(Arrays.asList(
                new SMTOperator("&"), 
                new SMTOperator("&"), 
                new SMTOperator("&"), 
                new SMTOperator("&"), 
                new SMTOperator("&"), 
                new SMTOperator("|")
            ))
        );

        atom = SMTSolver.solveSMT(atom);
        SMTSolver.printResult(atom);


    }

    public static Atom solveSMT(Atom atom){
        ArrayList<Clause> satClauses = atomToCNF(atom);

        ArrayList<Clause> satResult = SATSolver.solveSAT(satClauses, 0);

        if (satResult == null){return null;}

        atom = makeInferences(atom);

        // run a seccond time in case some variables were made unchangeable
        atom = makeInferences(atom);
        printSMTLiteralDomains(atom);

        Atom result = performSMT(atom, satResult, 0);

        if (result == null){
            // try all combinations
            for (int i = 0; i < 5; i ++){
                // implement later
            }
        }
        return result;
    }

    private static Atom makeInferences(Atom atom){
        for (int index = 0; index < atom.clauses.size(); index++) {

            SMTClause clause = atom.clauses.get(index);
            for (int opIndex = 0; opIndex < clause.operators.size(); opIndex++) {

                SMTOperator operator = clause.operators.get(opIndex);
                SMTLiteral leftLiteral = clause.literals.get(opIndex);
                SMTLiteral rightLiteral = clause.literals.get(opIndex+1);
                if (COMPARISON_OPS.contains(operator.operator)){
                    if (rightLiteral.isValueLocked){
                        leftLiteral = refineLeftLiteralDomain(leftLiteral, rightLiteral, operator);
                    }
                    else if (leftLiteral.isValueLocked){
                        rightLiteral = refineRightLiteralDomain(leftLiteral, rightLiteral, operator);
                    }
                    else{
                        SMTLiteral[] lits = refineBothLiteralDomains(leftLiteral, rightLiteral, operator);
                        leftLiteral = lits[0];
                        rightLiteral = lits[1];
                    }
                }
            }
        }

        return atom;
    }

    static void propagateEqualities(Atom atom) {
        boolean changed;
        do {
            changed = false;
            for (SMTClause c : atom.clauses) {
                if (isSimpleEquality(c)) {
                    SMTLiteral left = c.literals.get(0);
                    SMTLiteral right = c.literals.get(1);
                    if (right.isValueLocked && !left.isValueLocked) {
                        left.value = right.value;
                        left.isValueLocked = true;
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    private static boolean isSimpleEquality(SMTClause clause) {
        if (clause == null || clause.literals == null || clause.operators == null) {
            return false;
        }
        if (clause.literals.size() != 2 || clause.operators.size() != 1) {
            return false;
        }
        String op = clause.operators.get(0).operator;
        return "=".equals(op) || "==".equals(op);
    }

    public static SMTLiteral refineLeftLiteralDomain(SMTLiteral leftLit, SMTLiteral rightLit, SMTOperator operator){
        switch (operator.operator) {
            case "<":
                leftLit.domain[1] = rightLit.value - 1;
                return leftLit;
            
            case ">":
                leftLit.domain[0] = rightLit.value + 1;
                return leftLit;
            
            case "=":
                leftLit.value = rightLit.value;
                leftLit.isValueLocked = true;
                return leftLit;

            case "==":
                leftLit.value = rightLit.value;
                leftLit.isValueLocked = true;
                return leftLit;
            
            case "<=":
                leftLit.domain[1] = rightLit.value;
                return leftLit;

            case ">=":
                leftLit.domain[0] = rightLit.value;
                return leftLit;
        
            default:
                return leftLit;
        }
    }

    public static SMTLiteral refineRightLiteralDomain(SMTLiteral leftLit, SMTLiteral rightLit, SMTOperator operator){
        switch (operator.operator) {
            case "<":
                rightLit.domain[0] = leftLit.value + 1;
                return rightLit;
            
            case ">":
                rightLit.domain[1] = leftLit.value - 1;
                return rightLit;
            
            case "=":
                rightLit.value = leftLit.value;
                rightLit.isValueLocked = true;
                return rightLit;

            case "==":
                rightLit.value = leftLit.value;
                rightLit.isValueLocked = true;
                return rightLit;
            
            case "<=":
                rightLit.domain[0] = leftLit.value;
                return rightLit;

            case ">=":
                rightLit.domain[1] = leftLit.value;
                return rightLit;
        
            default:
                return rightLit;
        }
    }

    public static SMTLiteral[] refineBothLiteralDomains(SMTLiteral leftLit, SMTLiteral rightLit, SMTOperator operator){
        SMTLiteral[] lits = new SMTLiteral[2];
        switch (operator.operator) {
            case "<":
                // left < right
                leftLit.domain[1] = Math.min(leftLit.domain[1], rightLit.domain[1] - 1);
                rightLit.domain[0] = Math.max(rightLit.domain[0], leftLit.domain[0] + 1);
                break;
            
            case ">":
                // left > right
                leftLit.domain[0] = Math.max(leftLit.domain[0], rightLit.domain[0] + 1);
                rightLit.domain[1] = Math.min(rightLit.domain[1], leftLit.domain[1] - 1);
                break;
            
            case "<=":
                leftLit.domain[1] = Math.min(leftLit.domain[1], rightLit.domain[1]);
                rightLit.domain[0] = Math.max(rightLit.domain[0], leftLit.domain[0]);
                break;
            
            case ">=":
                leftLit.domain[0] = Math.max(leftLit.domain[0], rightLit.domain[0]);
                rightLit.domain[1] = Math.min(rightLit.domain[1], leftLit.domain[1]);
                break;
        }
        lits[0] = leftLit;
        lits[1] = rightLit;
        return lits;
    }

    private static ArrayList<Clause> atomToCNF(Atom atom) {
        ArrayList<Clause> satClauses = new ArrayList<>();
        if (atom == null || atom.clauses == null || atom.clauses.isEmpty()) {
            return satClauses;
        }

        int clauseCount = atom.clauses.size();
        ArrayList<Variable> satVariables = new ArrayList<>();
        for (int i = 0; i < clauseCount; i++) {
            satVariables.add(new Variable(i));
        }

        ClauseTruthStatus[] clauseStatuses = new ClauseTruthStatus[clauseCount];
        for (int i = 0; i < clauseCount; i++) {
            clauseStatuses[i] = determineClauseStatus(atom.clauses.get(i));
            if (clauseStatuses[i] == ClauseTruthStatus.TRUE) {
                satClauses.add(Clause.of(new Literal(satVariables.get(i), false)));
            } else if (clauseStatuses[i] == ClauseTruthStatus.FALSE) {
                satClauses.add(Clause.of(new Literal(satVariables.get(i), true)));
            }
        }

        BoolNode expressionTree = buildExpressionTree(atom, clauseStatuses);
        if (expressionTree == null) {
            return satClauses;
        }

        TseitinContext context = new TseitinContext(satClauses, satVariables);
        EncodedResult encoded = encodeBoolNode(expressionTree, context);

        if (encoded.isConstant) {
            if (!encoded.constantValue) {
                satClauses.add(new Clause());
            }
            return satClauses;
        }

        Clause rootClause = new Clause();
        rootClause.clause.add(new Literal(context.variable(encoded.varIndex), false));
        satClauses.add(rootClause);
        return satClauses;
    }

    private static List<String> normalizeClauseOperators(Atom atom, int clauseCount) {
        List<String> operators = new ArrayList<>();
        int expected = Math.max(0, clauseCount - 1);
        for (int i = 0; i < expected; i++) {
            if (atom.operators != null && i < atom.operators.size()) {
                operators.add(atom.operators.get(i).operator);
            } else {
                operators.add("&");
            }
        }
        return operators;
    }

    private static BoolNode buildExpressionTree(Atom atom, ClauseTruthStatus[] statuses) {
        int clauseCount = atom.clauses.size();
        if (clauseCount == 0) {return null;}

        List<String> operators = normalizeClauseOperators(atom, clauseCount);
        Deque<BoolNode> valueStack = new ArrayDeque<>();
        Deque<String> operatorStack = new ArrayDeque<>();

        for (int i = 0; i < clauseCount; i++) {
            valueStack.push(createClauseNode(i, statuses[i]));

            if (i < clauseCount - 1) {
                String op = operators.get(i);
                while (!operatorStack.isEmpty() && booleanPrecedence(operatorStack.peek()) >= booleanPrecedence(op)) {
                    foldExpressionStacks(valueStack, operatorStack.pop());
                }
                operatorStack.push(op);
            }
        }

        while (!operatorStack.isEmpty()) {
            foldExpressionStacks(valueStack, operatorStack.pop());
        }

        return valueStack.isEmpty() ? null : valueStack.pop();
    }

    private static void foldExpressionStacks(Deque<BoolNode> valueStack, String operator) {
        BoolNode right = valueStack.pop();
        BoolNode left = valueStack.pop();
        valueStack.push(new BinaryNode(operator, left, right));
    }

    private static BoolNode createClauseNode(int index, ClauseTruthStatus status) {
        if (status == ClauseTruthStatus.TRUE) {
            return new ConstNode(true);
        }
        if (status == ClauseTruthStatus.FALSE) {
            return new ConstNode(false);
        }
        return new ClauseNode(index);
    }

    private static ClauseTruthStatus determineClauseStatus(SMTClause clause) {
        if (clause == null || clause.literals == null || clause.literals.isEmpty()) {
            return ClauseTruthStatus.FALSE;
        }
        if (!areAllLiteralsLocked(clause)) {
            return ClauseTruthStatus.UNKNOWN;
        }
        return evaluateSMTClause(clause) ? ClauseTruthStatus.TRUE : ClauseTruthStatus.FALSE;
    }

    private static boolean areAllLiteralsLocked(SMTClause clause) {
        for (SMTLiteral literal : clause.literals) {
            if (!literal.isValueLocked) {
                return false;
            }
        }
        return true;
    }

    private static EncodedResult encodeBoolNode(BoolNode node, TseitinContext context) {
        if (node instanceof ConstNode) {
            return EncodedResult.constant(((ConstNode) node).value);
        }
        if (node instanceof ClauseNode) {
            return EncodedResult.variable(((ClauseNode) node).clauseIndex);
        }

        BinaryNode binary = (BinaryNode) node;
        EncodedResult left = encodeBoolNode(binary.left, context);
        EncodedResult right = encodeBoolNode(binary.right, context);

        switch (binary.operator) {
            case "&":
                return encodeAnd(left, right, context);
            case "|":
                return encodeOr(left, right, context);
            case "^":
                return encodeXor(left, right, context);
            default:
                return EncodedResult.constant(true);
        }
    }

    private static EncodedResult encodeAnd(EncodedResult left, EncodedResult right, TseitinContext ctx) {
        if (left.isConstant && !left.constantValue) {return left;}
        if (right.isConstant && !right.constantValue) {return right;}
        if (left.isConstant && left.constantValue) {return right;}
        if (right.isConstant && right.constantValue) {return left;}
        if (left.isConstant && right.isConstant) {return EncodedResult.constant(left.constantValue && right.constantValue);}

        int output = ctx.newVariable();
        ctx.addClause(literal(output, true), literal(left.varIndex, false));
        ctx.addClause(literal(output, true), literal(right.varIndex, false));
        ctx.addClause(literal(output, false), literal(left.varIndex, true), literal(right.varIndex, true));
        return EncodedResult.variable(output);
    }

    private static EncodedResult encodeOr(EncodedResult left, EncodedResult right, TseitinContext ctx) {
        if (left.isConstant && left.constantValue) {return left;}
        if (right.isConstant && right.constantValue) {return right;}
        if (left.isConstant && !left.constantValue) {return right;}
        if (right.isConstant && !right.constantValue) {return left;}
        if (left.isConstant && right.isConstant) {return EncodedResult.constant(left.constantValue || right.constantValue);}

        int output = ctx.newVariable();
        ctx.addClause(literal(left.varIndex, true), literal(output, false));
        ctx.addClause(literal(right.varIndex, true), literal(output, false));
        ctx.addClause(literal(output, true), literal(left.varIndex, false), literal(right.varIndex, false));
        return EncodedResult.variable(output);
    }

    private static EncodedResult encodeXor(EncodedResult left, EncodedResult right, TseitinContext ctx) {
        if (left.isConstant && right.isConstant) {
            return EncodedResult.constant(left.constantValue ^ right.constantValue);
        }
        if (left.isConstant) {
            return left.constantValue ? encodeNot(right, ctx) : right;
        }
        if (right.isConstant) {
            return right.constantValue ? encodeNot(left, ctx) : left;
        }

        int output = ctx.newVariable();
        ctx.addClause(literal(output, true), literal(left.varIndex, false), literal(right.varIndex, false));
        ctx.addClause(literal(output, true), literal(left.varIndex, true), literal(right.varIndex, true));
        ctx.addClause(literal(output, false), literal(left.varIndex, false), literal(right.varIndex, true));
        ctx.addClause(literal(output, false), literal(left.varIndex, true), literal(right.varIndex, false));
        return EncodedResult.variable(output);
    }

    private static EncodedResult encodeNot(EncodedResult operand, TseitinContext ctx) {
        if (operand.isConstant) {
            return EncodedResult.constant(!operand.constantValue);
        }
        int output = ctx.newVariable();
        ctx.addClause(literal(output, true), literal(operand.varIndex, true));
        ctx.addClause(literal(output, false), literal(operand.varIndex, false));
        return EncodedResult.variable(output);
    }

    private static LiteralSpec literal(int varIndex, boolean negated) {
        return new LiteralSpec(varIndex, negated);
    }

    private static int booleanPrecedence(String operator) {
        if ("&".equals(operator)) {return 3;}
        if ("^".equals(operator)) {return 2;}
        if ("|".equals(operator)) {return 1;}
        return 0;
    }

    private static Atom performSMT(Atom atom, ArrayList<Clause> satResult, int depth) {
        ArrayList<SMTLiteral> variables = getVariables(atom);
        if (variables.isEmpty()) {
            return checkSMTValidity(atom, satResult);
        }

        SMTLiteral literal = variables.get(depth);
        if (depth < variables.size() - 1){
            if (literal.isValueLocked){
                return performSMT(atom, satResult, depth+1);
            }

            for (int i = literal.domain[0]; i <= literal.domain[1]; i++){
                literal.value = i;
                Atom result = performSMT(atom, satResult, depth+1);
                if (result != null) {return result;}
            }

        }
        else{
            if (literal.isValueLocked){
                return checkSMTValidity(atom,satResult);
            }

            for (int i = literal.domain[0]; i < literal.domain[1]; i++){
                literal.value = i;
                Atom result = checkSMTValidity(atom,satResult);
                if (result != null) {return result;}
            }
        }

        return null;
    }

    private static Atom checkSMTValidity(Atom atom, ArrayList<Clause> satResult) {
        // Each variable in SAT represents one clause in SMT
        // Check if all SMT clauses evaluate correctly based on SAT assignment

        for (int i = 0; i < atom.clauses.size(); i++) {
            SMTClause smtClause = atom.clauses.get(i);
            boolean clauseResult = evaluateSMTClause(smtClause);
            
            // Find the expected value from SAT result for this clause (variable i)
            boolean expectedValue = getExpectedValueForClause(satResult, i);
            
            if (clauseResult != expectedValue) {
                return null; // This assignment doesn't satisfy the constraints
            }
        }

        
        
        // All clauses satisfied, return the clauses with their current values
        return atom;
    }

    private static boolean evaluateSMTClause(SMTClause clause) {
        if (clause == null || clause.literals == null || clause.literals.isEmpty()) {
            return false;
        }

        if (clause.operators == null || clause.operators.isEmpty()) {
            // Fallback to previous behavior: a single literal clause is considered satisfied
            return clause.literals.size() == 1;
        }

        List<Integer> values = new ArrayList<>();
        for (SMTLiteral lit : clause.literals) {
            values.add(lit.value);
        }

        int[] currentValues = values.stream().mapToInt(Integer::intValue).toArray();
        ClauseEvaluationCache cache = CLAUSE_EVAL_CACHE.get(clause);
        if (cache != null && cache.matches(currentValues)) {
            return cache.lastResult;
        }

        List<String> ops = new ArrayList<>();
        for (SMTOperator op : clause.operators) {
            ops.add(op.operator);
        }

        if (values.size() - 1 != ops.size()) {
            return false;
        }

        if (!processArithmeticOps(values, ops, MULTIPLICATIVE_OPS)) {
            return false;
        }
        if (!processArithmeticOps(values, ops, ADDITIVE_OPS)) {
            return false;
        }

        Boolean comparisonResult = processComparisonOps(values, ops, COMPARISON_OPS);
        Boolean equalityResult = processComparisonOps(values, ops, EQUALITY_OPS);

        boolean finalResult;
        if (equalityResult != null) {
            finalResult = equalityResult;
        } else if (comparisonResult != null) {
            finalResult = comparisonResult;
        } else {
            finalResult = !values.isEmpty() && values.get(0) != 0;
        }

        cache = CLAUSE_EVAL_CACHE.computeIfAbsent(clause, key -> new ClauseEvaluationCache());
        cache.update(currentValues, finalResult);
        return finalResult;
    }

    private static boolean processArithmeticOps(List<Integer> values, List<String> ops, Set<String> targetOps) {
        for (int i = 0; i < ops.size(); ) {
            String op = ops.get(i);
            if (!targetOps.contains(op)) {
                i++;
                continue;
            }

            int left = values.get(i);
            int right = values.get(i + 1);
            int result;
            switch (op) {
                case "+":
                    result = left + right;
                    break;
                case "-":
                    result = left - right;
                    break;
                case "*":
                    result = left * right;
                    break;
                case "/":
                    if (right == 0) {return false;}
                    result = left / right;
                    break;
                case "%":
                    if (right == 0) {return false;}
                    result = left % right;
                    break;
                default:
                    result = left;
            }

            values.set(i, result);
            values.remove(i + 1);
            ops.remove(i);
        }
        return true;
    }

    private static Boolean processComparisonOps(List<Integer> values, List<String> ops, Set<String> targetOps) {
        Boolean lastResult = null;
        for (int i = 0; i < ops.size(); ) {
            String op = ops.get(i);
            if (!targetOps.contains(op)) {
                i++;
                continue;
            }

            int left = values.get(i);
            int right = values.get(i + 1);
            boolean comparison;
            switch (op) {
                case "<":
                    comparison = left < right;
                    break;
                case "<=":
                    comparison = left <= right;
                    break;
                case ">":
                    comparison = left > right;
                    break;
                case ">=":
                    comparison = left >= right;
                    break;
                case "=":
                case "==":
                    comparison = left == right;
                    break;
                case "!=":
                case "<>":
                    comparison = left != right;
                    break;
                default:
                    comparison = false;
            }

            lastResult = comparison;
            values.set(i, comparison ? 1 : 0);
            values.remove(i + 1);
            ops.remove(i);
        }
        return lastResult;
    }


    private static boolean getExpectedValueForClause(ArrayList<Clause> satResult, int clauseIndex) {
        // Find the variable with val == clauseIndex in the SAT result
        for (Clause clause : satResult) {
            for (Literal lit : clause.clause) {
                if (lit.var.val == clauseIndex) {
                    // If negated, we expect false; otherwise true
                    // on_off indicates the assigned value
                    if (lit.var.on_off != null) {
                        return lit.neg ? !lit.var.on_off : lit.var.on_off;
                    }
                    // Default: not negated means we expect true
                    return !lit.neg;
                }
            }
        }
        // Default to true if not found
        return true;
    }

    private static ArrayList<SMTLiteral> getVariables(Atom clauses) {
        ArrayList<SMTLiteral> result = new ArrayList<>();
        for (SMTClause cl : clauses.clauses){
            for (SMTLiteral lit : cl.literals){
                if (!result.contains(lit)){
                    result.add(lit);
                }
            }
        }
        return result;
    }

    public static void printResult(Atom atom) {
        if (atom == null) {
            System.out.println("UNSATISFIABLE");
            return;
        }

        System.out.println("SATISFIABLE");
        System.out.println("Variable assignments:");
        
        ArrayList<SMTLiteral> variables = getVariables(atom);
        for (SMTLiteral lit : variables) {
            System.out.println("  " + lit.id + " = " + lit.value);
        }

        System.out.println("\nClause evaluations:");
        for (int i = 0; i < atom.clauses.size(); i++) {
            SMTClause clause = atom.clauses.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("  Clause ").append(i).append(": ");
            
            if (clause.literals.size() >= 2 && clause.operators != null && clause.operators.size() == clause.literals.size() - 1) {
                appendClause(sb, clause, false);
                sb.append(" => ");
                appendClause(sb, clause, true);
                sb.append(" = ").append(evaluateSMTClause(clause));
            } else if (clause.literals.size() == 1) {
                sb.append(clause.literals.get(0).id)
                  .append(" = ").append(clause.literals.get(0).value);
            }
            
            System.out.println(sb.toString());
        }
    }

    private static void appendClause(StringBuilder sb, SMTClause clause, boolean useValues) {
        for (int j = 0; j < clause.literals.size(); j++) {
            SMTLiteral lit = clause.literals.get(j);
            sb.append(useValues ? lit.value : lit.id);
            if (clause.operators != null && j < clause.operators.size()) {
                sb.append(" ").append(clause.operators.get(j).operator).append(" ");
            }
        }
    }

    private enum ClauseTruthStatus {
        TRUE,
        FALSE,
        UNKNOWN
    }

    private abstract static class BoolNode {}

    private static final class ConstNode extends BoolNode {
        final boolean value;

        ConstNode(boolean value) {
            this.value = value;
        }
    }

    private static final class ClauseNode extends BoolNode {
        final int clauseIndex;

        ClauseNode(int clauseIndex) {
            this.clauseIndex = clauseIndex;
        }
    }

    private static final class BinaryNode extends BoolNode {
        final String operator;
        final BoolNode left;
        final BoolNode right;

        BinaryNode(String operator, BoolNode left, BoolNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
    }

    private static final class EncodedResult {
        final boolean isConstant;
        final boolean constantValue;
        final int varIndex;

        private EncodedResult(boolean isConstant, boolean constantValue, int varIndex) {
            this.isConstant = isConstant;
            this.constantValue = constantValue;
            this.varIndex = varIndex;
        }

        static EncodedResult constant(boolean value) {
            return new EncodedResult(true, value, -1);
        }

        static EncodedResult variable(int index) {
            return new EncodedResult(false, false, index);
        }
    }

    private static final class LiteralSpec {
        final int varIndex;
        final boolean negated;

        LiteralSpec(int varIndex, boolean negated) {
            this.varIndex = varIndex;
            this.negated = negated;
        }
    }

    private static final class TseitinContext {
        private final ArrayList<Clause> clauses;
        private final ArrayList<Variable> variables;
        private int nextVarIndex;

        TseitinContext(ArrayList<Clause> clauses, ArrayList<Variable> baseVariables) {
            this.clauses = clauses;
            this.variables = baseVariables;
            this.nextVarIndex = baseVariables.size();
        }

        int newVariable() {
            int index = nextVarIndex++;
            variables.add(new Variable(index));
            return index;
        }

        Variable variable(int index) {
            while (variables.size() <= index) {
                variables.add(new Variable(variables.size()));
            }
            return variables.get(index);
        }

        void addClause(LiteralSpec... literals) {
            Clause clause = new Clause();
            for (LiteralSpec spec : literals) {
                clause.clause.add(new Literal(variable(spec.varIndex), spec.negated));
            }
            clauses.add(clause);
        }
    }

    private static final class ClauseEvaluationCache {
        private int[] lastValues;
        private boolean lastResult;
        private boolean initialized;

        boolean matches(int[] values) {
            if (!initialized || lastValues == null || lastValues.length != values.length) {
                return false;
            }
            for (int i = 0; i < values.length; i++) {
                if (lastValues[i] != values[i]) {
                    return false;
                }
            }
            return true;
        }

        void update(int[] values, boolean result) {
            lastValues = values.clone();
            lastResult = result;
            initialized = true;
        }
    }

    private static void printSMTLiteralDomains(Atom atom){
        ArrayList<SMTLiteral> usedLiterals = new ArrayList<>();
        for (SMTClause clause : atom.clauses) {
            for (SMTLiteral lit : clause.literals) {
                if (!usedLiterals.contains(lit)){
                    System.out.println(lit.id + ": {" + lit.domain[0] + ", " + lit.domain[1] + "}");
                    usedLiterals.add(lit);
                }
            }
        }
    }
}
