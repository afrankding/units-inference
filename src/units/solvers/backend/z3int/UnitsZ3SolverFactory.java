package units.solvers.backend.z3int;

import checkers.inference.solver.backend.z3Int.Z3IntFormatTranslator;
import checkers.inference.solver.backend.z3Int.Z3IntSolverFactory;
import checkers.inference.solver.frontend.Lattice;
import units.solvers.backend.z3int.encoder.UnitsZ3EncodedSlot;

public class UnitsZ3SolverFactory
        extends Z3IntSolverFactory<UnitsZ3EncodedSlot, UnitsZ3EncodedSlot> {

    @Override
    protected Z3IntFormatTranslator<UnitsZ3EncodedSlot, UnitsZ3EncodedSlot> createFormatTranslator(
            Lattice lattice) {
        return new UnitsZ3FormatTranslator(lattice);
    }
}