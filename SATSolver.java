import java.util.ArrayList;

import SAT.Clause;
import SAT.Literal;
import SAT.Results;
import SAT.SATTests;
import SAT.Variable;

public class SATSolver {

    public static ArrayList<Clause> solveSAT(ArrayList<Clause> clauses, int depth){
        if (depth < getVariables(clauses).size()-1){

            if (isContradiction()){return null;}

            Variable variable = getVariables(clauses).get(depth);
            
            variable.on_off = true;
            ArrayList<Clause> newClauses = solveSAT(clauses, depth+1);
            if (newClauses != null){
                if (solved(newClauses)){
                    return newClauses;
                }
            }
            

            variable.on_off = false;
            newClauses = solveSAT(clauses, depth+1);
            if (newClauses != null){
                if (solved(newClauses)){
                    return newClauses;
                }
            } 

            return null;
        }
        else{
            if (isContradiction()){return null;}

            Variable variable = getVariables(clauses).get(depth);
            
            variable.on_off = true;
            if (solved(clauses)){
                return clauses;
            }

            variable.on_off = false;
            if (solved(clauses)){
                return clauses;
            } 

            return null;
        }
    }

    // assuming you want it to be true
    private static boolean solved(ArrayList<Clause> sat) {
        // ive decided that it starts as solved, and it just wants to prove otherwise, that way if the first clause is false its faster
        for (Clause c : sat){
            boolean clauseSolved = false;
            for (Literal lit : c.clause){
                if (lit.var.on_off == null){return false;}
                if (lit.var.on_off ^ lit.neg) {clauseSolved = true;}
            }
            if (!clauseSolved){return false;}
        }
        return true;
    }

    private static ArrayList<Variable> getVariables(ArrayList<Clause> sat) {   
        ArrayList<Variable> result = new ArrayList<>();
        for (Clause cl : sat){
            for (Literal lit : cl.clause){
                Variable var = lit.var;
                if (!result.contains(var)){
                    result.add(var);
                }
            }
        }
        return result;
    }

    private static boolean isContradiction() {
        return false;
    }

    public static ArrayList<Clause> setVariableValue(Variable var, boolean on_off, ArrayList<Clause> clauses){
        clauses.add(Clause.of(new Literal(var, !on_off)));
        return clauses;
    }

    public static void main(String[] args){
        ArrayList<Clause> cnf = new ArrayList<>();

        Variable var = new Variable(1);
        Variable var3 = new Variable(3);
        cnf = SATTests.xor3(var, new Variable(2), var3);

        cnf = setVariableValue(var, false, cnf);
        cnf = setVariableValue(var3, true, cnf);

        cnf = solveSAT(cnf, 0);
        System.out.print(Results.SAT(cnf));
    }
}
