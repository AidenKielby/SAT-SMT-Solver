package SMT;

import java.util.ArrayList;

public class Atom {
    public ArrayList<SMTClause> clauses;
    public ArrayList<SMTOperator> operators; 

    public Atom(ArrayList<SMTClause> clauses, ArrayList<SMTOperator> operators){
        this.clauses = clauses;
        this.operators = operators;
    }
}
