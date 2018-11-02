package units.solvers.backend;

import org.checkerframework.javacutil.BugInCF;

import checkers.inference.solver.SolverEngine;
import checkers.inference.solver.backend.SolverFactory;
import units.solvers.backend.z3smt.UnitsZ3SmtSolverFactory;

public class UnitsSolverEngine extends SolverEngine {
    @Override
    protected SolverFactory createSolverFactory() {
        System.out.println("======= solver name: " + solverName);
        if (solverName.contentEquals("Z3smt")) {
            return new UnitsZ3SmtSolverFactory();
        // TODO: add GJE solver
        } else {
            throw new BugInCF(
                    "A back end solver (Z3smt, GJE) must be supplied in solverArgs: solver=Z3smt");
        }
    }
}
