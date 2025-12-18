package SAT;
public class Literal {
    public Variable var;
    public boolean neg;

    public Literal(Variable var, boolean neg){
        this.var = var;
        this.neg = neg;
    }
}
