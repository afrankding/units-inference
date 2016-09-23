package ontology.solvers.backend;

import checkers.inference.model.CombineConstraint;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Slot;
import checkers.inference.model.VariableSlot;
import constraintsolver.Lattice;
import constraintsolver.VariableCombos;
import java.util.ArrayList;
import java.util.List;
import maxsatbackend.MaxSatSerializer;
import ontology.util.OntologyUtils;
import org.checkerframework.javacutil.AnnotationUtils;
import org.sat4j.core.VecInt;
import util.MathUtils;
import util.VectorUtils;

public class OntologyMaxsatSerializer extends MaxSatSerializer {

  public OntologyMaxsatSerializer(Lattice lattice) {
    super(lattice);
  }

  @Override
  public VecInt[] serialize(CombineConstraint combineConstraint) {
    return new OntologyCombineVariableCombos(new VecInt[0])
        .accept(
            combineConstraint.getTarget(),
            combineConstraint.getDeclared(),
            combineConstraint.getResult(),
            combineConstraint);
  }

  protected class OntologyCombineVariableCombos
      extends VariableCombos<CombineConstraint, VecInt[]> {
    public OntologyCombineVariableCombos(VecInt[] emptyValue) {
      super(emptyValue);
    }

    protected VecInt[] constant_constant(
        ConstantSlot target, ConstantSlot decl, VariableSlot result, CombineConstraint constraint) {
      return encodingRules(target, decl, result, constraint);
    }

    protected VecInt[] variable_constant(
        VariableSlot target, ConstantSlot decl, VariableSlot result, CombineConstraint constraint) {
      return encodingRules(target, decl, result, constraint);
    }

    VecInt[] encodingRules(
        Slot target, Slot decl, VariableSlot result, CombineConstraint constraint) {
      List<VecInt> resultClauses = new ArrayList<>();

      if (decl instanceof ConstantSlot) {
        ConstantSlot cDecl = (ConstantSlot) decl;
        if (AnnotationUtils.areSame(OntologyUtils.POLY_ONTOLOGY, cDecl.getValue())) {
          if (target instanceof ConstantSlot) {
            resultClauses.add(
                VectorUtils.asVec(
                    MathUtils.mapIdToMatrixEntry(
                        result.getId(), ((ConstantSlot) target).getValue(), lattice)));
          } else {
            VecInt[] equalityClauses =
                new EqualityVariableCombos(emptyClauses).accept(result, target, null);
            return equalityClauses;
          }
        } else {
          if (target instanceof ConstantSlot) {
            //TODO: TBD
            return defaultAction();
          } else {
            if (lattice.getAllTypes().contains(cDecl.getValue())) {
              resultClauses.add(
                  VectorUtils.asVec(
                      MathUtils.mapIdToMatrixEntry(result.getId(), cDecl.getValue(), lattice)));
            }
          }
        }
      } else {
        return defaultAction();
      }

      return resultClauses.toArray(new VecInt[resultClauses.size()]);
    }
  }
}
