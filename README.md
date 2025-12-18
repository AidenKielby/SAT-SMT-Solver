# SAT-SMT Playground

A compact Java playground for experimenting with SAT solving and basic SMT reasoning. The project combines a backtracking SAT solver with a lightweight SMT layer capable of handling simple arithmetic and boolean operators.

## Highlights

- **Hybrid Reasoning Pipeline** – Translates SMT atoms into SAT clauses via a custom Tseitin-style encoder, ensuring that higher-level arithmetic relations survive the CNF boundary.
- **Lightweight SMT Clauses** – Supports chained arithmetic comparisons (`<`, `<=`, `>`, `>=`) and equalities, including additive/multiplicative expressions.
- **Domain Inference** – Applies quick domain refinements and equality propagation to clamp literal ranges before search begins.
- **Clause Evaluation Cache** – Memoizes clause evaluations to speed up repeated SMT checks during DFS.
- **Readable Demo** – `SMTSolver.main` bootstraps sample literals and clauses so you can run the solver immediately and inspect printed assignments.

## Project Layout

```
SAT-SMT/
├─ SMTSolver.java        # Entry point orchestrating SAT and SMT layers
├─ SATSolver.java        # Recursive SAT solver with clause evaluation helpers
└─ classes/
   ├─ SAT/               # Clause, Literal, Variable, CNF utilities, tests
   └─ SMT/               # Atom, Clause, Literal, Operator definitions
```

## Getting Started

1. **Compile**
   ```bash
   javac *.java classes/SAT/*.java classes/SMT/*.java
   ```

2. **Run the demo**
   ```bash
   java SMTSolver
   ```
   You should see whether the combined SAT/SMT instance is satisfiable, followed by the chosen assignments and clause evaluations.

3. **Tweak the scenario**
   - Edit `SMTSolver.main` to add/remove literals, adjust domains, or change boolean connectors between SMT clauses.
   - Re-run the solver to observe how the SAT layer reacts to the updated SMT configuration.

## Custom Experiments

- **Add New Clauses** – Instantiate more `SMTClause` objects, fill their `literals` and `operators`, then append them to the `Atom` before calling `solveSMT`.
- **Change Boolean Structure** – Modify the `Atom` operator list (`&, |, ^`) to explore different clause combinations.
- **Inspect Domains** – `printSMTLiteralDomains` shows current literal ranges after inference; useful when debugging domain tightening.
- **Extend Operators** – To support additional arithmetic or boolean operators, update the evaluation switch statements and the operator sets near the top of `SMTSolver`.

## Notes

- The current solver is educational: it favors clarity over raw performance.
- Clause caching and inference hooks are intentionally simple so you can extend them with more sophisticated strategies (e.g., conflict-driven learning, interval propagation).
- Contributions are welcome—feel free to fork and iterate on the SAT/SMT blend!

Enjoy exploring the SAT-SMT frontier! ✨
