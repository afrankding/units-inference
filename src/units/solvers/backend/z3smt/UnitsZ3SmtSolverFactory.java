package units.solvers.backend.z3smt;

import backend.z3smt.Z3SmtFormatTranslator;
import backend.z3smt.Z3SmtSolverFactory;
import checkers.inference.solver.frontend.Lattice;
import units.representation.TypecheckUnit;
import units.solvers.backend.z3smt.representation.Z3InferenceUnit;

public class UnitsZ3SmtSolverFactory extends Z3SmtSolverFactory<Z3InferenceUnit, TypecheckUnit> {

    @Override
    protected Z3SmtFormatTranslator<Z3InferenceUnit, TypecheckUnit> createFormatTranslator(
            Lattice lattice) {
        return new UnitsZ3SmtFormatTranslator(lattice);
    }
}
