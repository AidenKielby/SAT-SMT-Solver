package SAT;
import java.util.ArrayList;

public class SATTests {
    public static ArrayList<Clause> xor3(Variable x, Variable y, Variable z) {
        ArrayList<Clause> cnf = new ArrayList<>();

        cnf.add(Clause.of(
            new Literal(x, false),
            new Literal(y, false),
            new Literal(z, false)
        ));

        cnf.add(Clause.of(
            new Literal(x, true),
            new Literal(y, true),
            new Literal(z, false)
        ));

        cnf.add(Clause.of(
            new Literal(x, true),
            new Literal(y, false),
            new Literal(z, true)
        ));

        cnf.add(Clause.of(
            new Literal(x, false),
            new Literal(y, true),
            new Literal(z, true)
        ));

        return cnf;
    }

    
}
