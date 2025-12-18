package SAT;
import java.util.ArrayList;

public class Results {
    public static final String UNSAT = "UNSAT";
    public static final String SAT(ArrayList<Clause> clauses){
        String result = "";
        ArrayList<Variable> usedLits = new ArrayList<>();
        for (Clause cl : clauses){
            for (Literal lit : cl.clause){
                if (!usedLits.contains(lit.var)){
                    result += String.valueOf(lit.var.on_off ^ lit.neg);
                    result += "\n";
                    usedLits.add(lit.var);
                }
            }
        }

        return result;
    }
}
