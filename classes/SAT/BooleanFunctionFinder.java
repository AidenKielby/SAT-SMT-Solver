package SAT;
import java.util.ArrayList;
import java.util.Arrays;

public class BooleanFunctionFinder {
    public ArrayList<ArrayList<Boolean>> inputTable = new ArrayList<>();
    public ArrayList<Boolean> outputTable = new ArrayList<>();

    public String findBooleanFunction(){
        StringBuilder function = new StringBuilder();

        for (int tableIndex = 0; tableIndex < outputTable.size(); tableIndex++) {
            ArrayList<Boolean> variables = inputTable.get(tableIndex);
            StringBuilder subFunction = new StringBuilder();
            Boolean output = outputTable.get(tableIndex);

            if (output == true){
                for (int variableIndex = 0; variableIndex < variables.size()-1; variableIndex++){
                    String var1 = variables.get(variableIndex+1) == true ? "Var"+(variableIndex+1) : "NOT Var"+(variableIndex+1);
                    String var = variables.get(variableIndex) == true ? "Var"+(variableIndex) : "NOT Var"+(variableIndex);

                    subFunction.append(var + " AND " + var1);
                }
                function.append("(" + subFunction.toString() + ") OR ");
            }
            
        }

        return function.replace(function.length()-3, function.length(), "").toString();
    }

    /**
     * Parse a truth table given as columns: "col1/col2/..." where each column
     * is a string of '0' and '1' characters representing that variable's value
     * for each row (ordered). The outputs string should contain one '0' or '1'
     * character per row. This method populates `inputTable` and `outputTable`.
     *
     * Example for XOR with rows (A,B):
     *  rows: (0,0),(0,1),(1,0),(1,1)
     *  columns: A = "0011", B = "0101" -> call: parseTruthTable("0011/0101", "0110")
     */
    public void parseTruthTable(String inputColumns, String outputs) {
        if (inputColumns == null || outputs == null) {
            throw new IllegalArgumentException("inputColumns and outputs must not be null");
        }

        String[] cols = inputColumns.trim().split("/");
        if (cols.length == 0) {
            throw new IllegalArgumentException("No columns found in inputColumns");
        }

        int rows = cols[0].trim().length();
        for (String c : cols) {
            if (c == null || c.trim().length() != rows) {
                throw new IllegalArgumentException("All columns must have the same length");
            }
        }

        String out = outputs.trim();
        if (out.length() != rows) {
            throw new IllegalArgumentException("Outputs length must match number of rows (" + rows + ")");
        }

        inputTable.clear();
        outputTable.clear();

        for (int r = 0; r < rows; r++) {
            ArrayList<Boolean> row = new ArrayList<>();
            for (String c : cols) {
                char ch = c.trim().charAt(r);
                if (ch != '0' && ch != '1') {
                    throw new IllegalArgumentException("Columns must contain only '0' or '1'");
                }
                row.add(ch == '1');
            }
            inputTable.add(row);

            char outch = out.charAt(r);
            if (outch != '0' && outch != '1') {
                throw new IllegalArgumentException("Outputs must contain only '0' or '1'");
            }
            outputTable.add(outch == '1');
        }
    }

    /**
     * Convenience factory: creates an instance and parses the provided columns/outputs.
     */
    public static BooleanFunctionFinder fromColumns(String inputColumns, String outputs) {
        BooleanFunctionFinder b = new BooleanFunctionFinder();
        b.parseTruthTable(inputColumns, outputs);
        return b;
    }

    /**
     * Builds the truth tables for a 2-bit adder. Inputs (in this order): A1,A0,B1,B0.
     * Returns an ArrayList of three BooleanFunctionFinder instances for the outputs
     * S0, S1, and Cout respectively (so you can call `findBooleanFunction()` on
     * each to get the expression for that output bit).
     */
    public static ArrayList<BooleanFunctionFinder> twoBitAdder() {
        StringBuilder A1 = new StringBuilder();
        StringBuilder A0 = new StringBuilder();
        StringBuilder B1 = new StringBuilder();
        StringBuilder B0 = new StringBuilder();

        StringBuilder S0 = new StringBuilder();
        StringBuilder S1 = new StringBuilder();
        StringBuilder Cout = new StringBuilder();

        // Iterate all combinations of A1,A0,B1,B0 (lexicographic)
        for (int a1 = 0; a1 <= 1; a1++) {
            for (int a0 = 0; a0 <= 1; a0++) {
                for (int b1 = 0; b1 <= 1; b1++) {
                    for (int b0 = 0; b0 <= 1; b0++) {
                        int aval = (a1 << 1) | a0;
                        int bval = (b1 << 1) | b0;
                        int sum = aval + bval; // range 0..6

                        A1.append(a1 == 1 ? '1' : '0');
                        A0.append(a0 == 1 ? '1' : '0');
                        B1.append(b1 == 1 ? '1' : '0');
                        B0.append(b0 == 1 ? '1' : '0');

                        S0.append(((sum >> 0) & 1) == 1 ? '1' : '0');
                        S1.append(((sum >> 1) & 1) == 1 ? '1' : '0');
                        Cout.append(((sum >> 2) & 1) == 1 ? '1' : '0');
                    }
                }
            }
        }

        String inputColumns = A1.toString() + "/" + A0.toString() + "/" + B1.toString() + "/" + B0.toString();

        ArrayList<BooleanFunctionFinder> outputs = new ArrayList<>();
        outputs.add(BooleanFunctionFinder.fromColumns(inputColumns, S0.toString()));
        outputs.add(BooleanFunctionFinder.fromColumns(inputColumns, S1.toString()));
        outputs.add(BooleanFunctionFinder.fromColumns(inputColumns, Cout.toString()));

        return outputs;
    }

    /**
     * Convert the currently loaded truth table (in this instance's
     * `inputTable`/`outputTable`) into CNF clauses that encode the function
     * for the provided input variables and a single output variable.
     *
     * For each row the method adds the implication
     *   (row_match) -> (output == outputBit)
     * as a single clause by turning the implication into a disjunction.
     */
    public ArrayList<Clause> toClauses(ArrayList<Variable> vars, Variable outVar) {
        if (inputTable.isEmpty()) {
            throw new IllegalStateException("inputTable is empty; parse or set a truth table first");
        }

        int varCount = inputTable.get(0).size();
        if (vars == null || vars.size() != varCount) {
            throw new IllegalArgumentException("Number of provided variables must match truth table columns: expected " + varCount);
        }

        ArrayList<Clause> cnf = new ArrayList<>();

        for (int r = 0; r < inputTable.size(); r++) {
            ArrayList<Boolean> row = inputTable.get(r);
            boolean outBit = outputTable.get(r);

            ArrayList<Literal> lits = new ArrayList<>();

            // ¬row_match is a disjunction of the negated literals of the row_match
            // For a variable assigned true in the row, ¬row_match contains ¬var (neg=true)
            // For a variable assigned false in the row, ¬row_match contains var (neg=false)
            for (int i = 0; i < varCount; i++) {
                boolean bit = row.get(i);
                Literal l = new Literal(vars.get(i), bit); // bit==true -> neg=true -> literal is ¬var
                lits.add(l);
            }

            // Add the output literal: for outBit==true include positive output, else include ¬output
            Literal outLit = new Literal(outVar, !outBit);
            lits.add(outLit);

            cnf.add(Clause.of(lits.toArray(new Literal[0])));
        }

        return cnf;
    }

    public static void main(String[] args){
        ArrayList<BooleanFunctionFinder> bits = BooleanFunctionFinder.twoBitAdder();

        String[] labels = {"S0", "S1", "Cout"};
        for (int i = 0; i < bits.size(); i++) {
            System.out.println(labels[i] + ": " + bits.get(i).findBooleanFunction());
        }
    }
}
