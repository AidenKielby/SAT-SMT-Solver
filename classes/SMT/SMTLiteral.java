package SMT;

public class SMTLiteral {
    public boolean isValueLocked = false;
    public int value;
    public String id;
    public int[] domain = {-100, 100};

    public SMTLiteral(boolean isValueLocked, int value, String id){
        this.isValueLocked = isValueLocked;
        if (isValueLocked){
            int[] d = {value, value};
            this.domain = d;
        }
        this.value = value;
        this.id = id;
    }
}
