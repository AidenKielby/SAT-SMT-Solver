package SAT;
import java.util.ArrayList;
import java.util.Arrays;

public class Clause {
    public ArrayList<Literal> clause;

    public Clause() {
        this.clause = new ArrayList<>();
    }

    // Factory method for convenience
    public static Clause of(Literal... literals) {
        Clause c = new Clause();
        c.clause.addAll(Arrays.asList(literals));
        return c;
    }
}
