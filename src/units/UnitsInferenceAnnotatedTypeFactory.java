package units;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceQualifierHierarchy;
import checkers.inference.InferenceTreeAnnotator;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.VariableSlot;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.NewClassTree;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotationClassLoader;
import org.checkerframework.framework.type.DefaultAnnotatedTypeFormatter;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import units.representation.UnitsRepresentationUtils;

public class UnitsInferenceAnnotatedTypeFactory extends InferenceAnnotatedTypeFactory {
    // static reference to the singleton instance
    protected static UnitsRepresentationUtils unitsRepUtils;

    public UnitsInferenceAnnotatedTypeFactory(
            InferenceChecker inferenceChecker,
            BaseAnnotatedTypeFactory realTypeFactory,
            InferrableChecker realChecker,
            SlotManager slotManager,
            ConstraintManager constraintManager) {
        super(
                inferenceChecker,
                false,
                realTypeFactory,
                realChecker,
                slotManager,
                constraintManager);

        // Should already be initialized in the real ATF
        unitsRepUtils = UnitsRepresentationUtils.getInstance();
        // and it should already have some base units
        assert !unitsRepUtils.baseUnits().isEmpty();

        unitsRepUtils.VARANNOT = getVarAnnot();

        postInit();
    }

    @Override
    protected AnnotationClassLoader createAnnotationClassLoader() {
        // Use the Units Annotated Type Loader instead of the default one
        return new UnitsAnnotationClassLoader(checker);
    }

    // In Inference ATF, this returns the set of real qualifiers
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // get all the loaded annotations
        Set<Class<? extends Annotation>> qualSet = new HashSet<Class<? extends Annotation>>();
        // Super grabs all supported qualifiers from the real qualifier hierarchy
        // and also puts in VarAnnot
        qualSet.addAll(super.createSupportedTypeQualifiers());

        // System.out.println( " --- quals = " + qualSet );

        // // load all the external units
        // loadAllExternalUnits();
        //
        // // copy all loaded external Units to qual set
        // qualSet.addAll(externalQualsMap.values());

        return qualSet;
    }

    // In Inference ATF, this returns the alias for a given real qualifier
    @Override
    public AnnotationMirror aliasedAnnotation(AnnotationMirror anno) {
        // TODO: cache results
        AnnotationMirror result = realTypeFactory.aliasedAnnotation(anno);
        // System.out.println(" === Aliasing: " + anno.toString() + " ==> " + result);

        if (result == null) {
            result = super.aliasedAnnotation(anno);
        }
        return result;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UnitsInferenceQualifierHierarchy(factory);
    }

    private final class UnitsInferenceQualifierHierarchy extends InferenceQualifierHierarchy {
        public UnitsInferenceQualifierHierarchy(MultiGraphFactory multiGraphFactory) {
            super(multiGraphFactory);
        }

        // Programmatically set UnitsRepresentationUtils.BOTTOM as the bottom
        @Override
        protected Set<AnnotationMirror> findBottoms(
                Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newBottoms = super.findBottoms(supertypes);

            newBottoms.remove(unitsRepUtils.RAWUNITSINTERNAL);
            newBottoms.add(unitsRepUtils.BOTTOM);

            // set direct supertypes of bottom
            Set<AnnotationMirror> supertypesOfBottom = new LinkedHashSet<>();
            supertypesOfBottom.add(unitsRepUtils.TOP);
            supertypes.put(unitsRepUtils.BOTTOM, supertypesOfBottom);

            return newBottoms;
        }

        // Programmatically set UnitsRepresentationUtils.TOP as the top
        @Override
        protected void finish(
                QualifierHierarchy qualHierarchy,
                Map<AnnotationMirror, Set<AnnotationMirror>> supertypes,
                Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
                Set<AnnotationMirror> tops,
                Set<AnnotationMirror> bottoms,
                Object... args) {
            super.finish(qualHierarchy, supertypes, polyQualifiers, tops, bottoms, args);

            // System.out.println(" === Inference ATF ");
            // System.out.println(" fullMap " + supertypes);
            // System.out.println(" polyQualifiers " + polyQualifiers);
            // System.out.println(" tops " + tops);
            // System.out.println(" bottoms " + bottoms);

            // TODO: see what needs to be here in Inference finish

            /// in Ontology, there's varannot, ontology raw, bottom, poly ontology, poly all, ...

            /*
             * --- full map:
             * {@checkers.inference.qual.VarAnnot=[@org.checkerframework.framework.qual.PolyAll]
             * , @ontology.qual.Ontology=[], @ontology.qual.Ontology(values={ontology.qual.
             * OntologyValue.BOTTOM})=[@ontology.qual.Ontology(values={ontology.qual.OntologyValue.
             * TOP}), @ontology.qual.PolyOntology, @org.checkerframework.framework.qual.PolyAll]
             * , @ontology.qual.PolyOntology=[@ontology.qual.Ontology, @org.checkerframework.
             * framework.qual.PolyAll], @org.checkerframework.framework.qual.PolyAll=[@checkers.
             * inference.qual.VarAnnot, @ontology.qual.Ontology]}
             */

            // System.out.println(" === supertypes: " + supertypes);
            // System.out.println(" === polyQualifiers: " + polyQualifiers);
            // System.out.println(" === tops: " + tops);
            // System.out.println(" === bottoms: " + bottoms);
        }
    }

    @Override
    public VariableAnnotator createVariableAnnotator() {
        return new UnitsVariableAnnotator(
                this, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    private final class UnitsVariableAnnotator extends VariableAnnotator {

        public UnitsVariableAnnotator(
                InferenceAnnotatedTypeFactory typeFactory,
                AnnotatedTypeFactory realTypeFactory,
                InferrableChecker realChecker,
                SlotManager slotManager,
                ConstraintManager constraintManager) {
            super(typeFactory, realTypeFactory, realChecker, slotManager, constraintManager);
        }

        @Override
        public void handleBinaryTree(AnnotatedTypeMirror atm, BinaryTree binaryTree) {
            // Super creates an LUB constraint by default, we create an VariableSlot here instead
            // for the result of the binary op and create LUB constraint in units visitor.
            if (treeToVarAnnoPair.containsKey(binaryTree)) {
                atm.replaceAnnotations(treeToVarAnnoPair.get(binaryTree).second);
            } else {
                // grab slots for the component (only for lub slot)
                AnnotatedTypeMirror lhsATM =
                        inferenceTypeFactory.getAnnotatedType(binaryTree.getLeftOperand());
                AnnotatedTypeMirror rhsATM =
                        inferenceTypeFactory.getAnnotatedType(binaryTree.getRightOperand());
                VariableSlot lhs = slotManager.getVariableSlot(lhsATM);
                VariableSlot rhs = slotManager.getVariableSlot(rhsATM);

                // create varslot for the result of the binary tree computation
                VariableSlot result;
                switch (binaryTree.getKind()) {
                    case PLUS:
                        // if it is a string concatenation, result is dimensionless
                        if (TreeUtils.isStringConcatenation(binaryTree)) {
                            result = slotManager.createConstantSlot(unitsRepUtils.DIMENSIONLESS);
                            break;
                        } // else create arithmetic slot
                    case MINUS:
                    case MULTIPLY:
                    case DIVIDE:
                    case REMAINDER:
                        result =
                                slotManager.createArithmeticVariableSlot(
                                        VariableAnnotator.treeToLocation(
                                                inferenceTypeFactory, binaryTree));
                        // ArithmeticOperationKind.fromTreeKind(binaryTree.getKind()), lhs, rhs);
                        break;
                    case CONDITIONAL_AND: // &&
                    case CONDITIONAL_OR: // ||
                    case LOGICAL_COMPLEMENT: // !
                    case EQUAL_TO: // ==
                    case NOT_EQUAL_TO: // !=
                    case GREATER_THAN: // >
                    case GREATER_THAN_EQUAL: // >=
                    case LESS_THAN: // <
                    case LESS_THAN_EQUAL: // <=
                        result = slotManager.createConstantSlot(unitsRepUtils.DIMENSIONLESS);
                        break;
                    default:
                        result = slotManager.createLubVariableSlot(lhs, rhs);
                        break;
                }

                // insert varAnnot of the slot into the ATM
                AnnotationMirror resultAM = slotManager.getAnnotation(result);
                atm.clearAnnotations();
                atm.replaceAnnotation(resultAM);

                // add to cache
                Set<AnnotationMirror> resultSet = AnnotationUtils.createAnnotationSet();
                resultSet.add(resultAM);
                final Pair<VariableSlot, Set<? extends AnnotationMirror>> varATMPair =
                        Pair.of(slotManager.getVariableSlot(atm), resultSet);
                treeToVarAnnoPair.put(binaryTree, varATMPair);
            }
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new UnitsInferenceImplicitsTreeAnnotator(),
                new UnitsInferenceTreeAnnotator(
                        this, realChecker, realTypeFactory, variableAnnotator, slotManager));
    }

    private final class UnitsInferenceImplicitsTreeAnnotator extends ImplicitsTreeAnnotator {
        // Programmatically set the qualifier implicits
        public UnitsInferenceImplicitsTreeAnnotator() {
            super(UnitsInferenceAnnotatedTypeFactory.this);
            // set BOTTOM as the implicit qualifier for null literals
            addLiteralKind(LiteralKind.NULL, unitsRepUtils.BOTTOM);
            addLiteralKind(LiteralKind.STRING, unitsRepUtils.DIMENSIONLESS);
            addLiteralKind(LiteralKind.CHAR, unitsRepUtils.DIMENSIONLESS);
            addLiteralKind(LiteralKind.BOOLEAN, unitsRepUtils.DIMENSIONLESS);

            // we do not implictly set dimensionless for the number literals as we want to infer
            // casts
        }
    }

    private final class UnitsInferenceTreeAnnotator extends InferenceTreeAnnotator {

        public UnitsInferenceTreeAnnotator(
                InferenceAnnotatedTypeFactory atypeFactory,
                InferrableChecker realChecker,
                AnnotatedTypeFactory realAnnotatedTypeFactory,
                VariableAnnotator variableAnnotator,
                SlotManager slotManager) {
            super(
                    atypeFactory,
                    realChecker,
                    realAnnotatedTypeFactory,
                    variableAnnotator,
                    slotManager);
        }

        // This note also appears in Sparta Checker's TreeAnnotator#visitNewClass(), and
        // SimpleFlowAnnotatedTypeFactory's TreeAnnotator#visitNewClass():

        // This is a horrible hack around the bad implementation of constructor results
        // (CF treats annotations on constructor results in stub files as if it were a
        // default and therefore ignores it.)
        @Override
        public Void visitNewClass(NewClassTree newClassTree, AnnotatedTypeMirror atm) {
            // Call super to annotate the constructor call with variable slots
            super.visitNewClass(newClassTree, atm);

            // This slot is the slot generated for new HERE Class(...)
            VariableSlot callSiteReturnVarSlot = slotManager.getVariableSlot(atm);

            // Then replace Polys declared for the constructor call with variable slots
            // copied from {@link InferenceAnnotatedTypeFactory#constructorFromUse(NewClassTree)}

            // TODO: see if we can just replace the following with
            // constructorFromUse(newClassTree)

            final ExecutableElement constructorElem = TreeUtils.constructor(newClassTree);
            final AnnotatedDeclaredType constructorReturnType = fromNewClass(newClassTree);
            final AnnotatedExecutableType constructorType =
                    AnnotatedTypes.asMemberOf(
                            types,
                            UnitsInferenceAnnotatedTypeFactory.this,
                            constructorReturnType,
                            constructorElem);

            // Replace polymorphic annotations in the constructor call with Variable Slots
            inferencePoly.replacePolys(newClassTree, constructorType);

            AnnotatedTypeMirror declaredReturnType = constructorType.getReturnType();

            // This slot is the slot generated for the LUB of the @Poly annotations
            VariableSlot declaredReturnVarSlot = slotManager.getVariableSlot(declaredReturnType);

            // System.out.println("");
            // System.out.println(" === callSiteReturnVarSlot " + callSiteReturnVarSlot);
            // System.out.println(" === declaredReturnVarSlot " + declaredReturnVarSlot);

            // With a constructor annotated as "@declaredReturnType Class(Type param)"
            // and a call of "new Class( arg )"
            // the call is annotated as:
            // (@declaredReturnType Class) new @callSiteReturnType Class( arg )

            // Create a subtype constraint: callSiteReturnType <: declaredReturnType
            // since after annotation insertion, the declaredReturnType appears as a cast of the
            // newly created object
            constraintManager.addSubtypeConstraint(callSiteReturnVarSlot, declaredReturnVarSlot);

            // Replace the annotation in the atm (callSiteReturnType) with the declaredReturnType
            atm.replaceAnnotation(declaredReturnType.getAnnotationInHierarchy(getVarAnnot()));

            return null;
        }

        // @Override
        // public Void visitVariable(VariableTree varTree, AnnotatedTypeMirror atm) {
        // super.visitVariable(varTree, atm);
        //
        // // For boxed primitive classes, we stub the constructors to be PolyUnit, thus there will
        // // be a VarAnnot created for each instance of PolyUnit.
        //
        // // An additional subtype constraint is generated here to ensure the VarAnnot is a
        // // subtype of the variable's VarAnnot.
        //
        // AnnotationMirror varAnnot =
        // ((InferenceAnnotatedTypeFactory) atypeFactory).getVarAnnot();
        //
        // // AnnotatedTypeMirror varATM = atypeFactory.getAnnotatedType(varTree);
        // AnnotatedTypeMirror expATM = atypeFactory.getAnnotatedType(varTree.getInitializer());
        // UnitsRepresentationUtils unitsRepUtils = UnitsRepresentationUtils.getInstance();
        //
        // // AnnotationMirror varAM = varATM.getEffectiveAnnotationInHierarchy(unitsRepUtils.TOP);
        //
        // AnnotationMirror expAM = expATM.getEffectiveAnnotationInHierarchy(unitsRepUtils.TOP);
        // AnnotationMirror expAnnotAM = expATM.getEffectiveAnnotationInHierarchy(varAnnot);
        // //
        // // VariableSlot expPolyAnnotSlot = slotManager
        // // .createVariableSlot(VariableAnnotator.treeToLocation(atypeFactory, varTree));
        //
        // System.out.println(" === var: " + varTree);
        //
        // // System.out.println(" === varATM " + varATM);
        // // System.out.println(" === varAM " + varAM);
        //
        // System.out.println(" === expATM " + expATM);
        // System.out.println(" === expAM " + expAM);
        // System.out.println(" === expAnnotAM " + expAnnotAM);
        //
        // // System.out.println(" === expPolyAnnotSlot " + expPolyAnnotSlot);
        //
        // System.out.println("");
        //
        // return null;
        // }

        //
        // @Override
        // public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror atm) {
        // super.visitTypeCast(tree, atm);
        //
        // // applyUnitsAnnotationsFromSource(tree, atm);
        //
        // return null;
        // }

        // private void applyUnitsAnnotationsFromSourceAsConstant(Tree tree, AnnotatedTypeMirror
        // atm) {
        // // If there is a units annotation present on the tree, then retrieve it, normalize it,
        // // create a constant slot for it replace the varAnnot in the ATM
        // AnnotationMirror realUnitsAnno = realTypeFactory.getAnnotatedType(tree)
        // .getAnnotationInHierarchy(unitsRepUtils.TOP);
        // if (realUnitsAnno != null) {
        // // Fill in any missing base units
        // realUnitsAnno = unitsRepUtils.fillMissingBaseUnits(realUnitsAnno);
        // // Create a ConstantSlot for the explicit annotation
        // ConstantSlot constSlot = variableAnnotator.createConstant(realUnitsAnno, tree);
        // // // Replace annotation in the slot
        // // atm.replaceAnnotation(slotManager.getAnnotation(constSlot));
        //
        // // Get the VariableSlot generated for the variable
        // VariableSlot varAnnotSlot = slotManager.getVariableSlot(atm);
        // // Add Equality constraint between the VariableSlot and the ConstantSlot
        // constraintManager.addEqualityConstraint(varAnnotSlot, constSlot);
        // }
        // }
        //
        // private void applyUnitsAnnotationsFromSource(Tree tree, AnnotatedTypeMirror atm) {
        // // If there's a units annotation present on the tree, then retrieve it, normalize it,
        // // create a constant slot for it, and an equality constraint between the constant slot
        // // and the VarAnnot generated for the ATM
        // if (unitsRepUtils.hasUnitsAnnotation(realTypeFactory,
        // atm.getUnderlyingType().getAnnotationMirrors())) {
        // AnnotationMirror realUnitsAnno = realTypeFactory.getAnnotatedType(tree)
        // .getAnnotationInHierarchy(unitsRepUtils.TOP);
        // // Fill in any missing base units
        // realUnitsAnno = unitsRepUtils.fillMissingBaseUnits(realUnitsAnno);
        // // Create a ConstantSlot for the explicit annotation
        // ConstantSlot constSlot = variableAnnotator.createConstant(realUnitsAnno, tree);
        //
        // // // Replace annotation in the slot
        // // atm.replaceAnnotation(slotManager.getAnnotation(constSlot));
        //
        // // Get the VariableSlot generated for the variable
        // VariableSlot varAnnotSlot = slotManager.getVariableSlot(atm);
        // // Add Equality constraint between the VariableSlot and the ConstantSlot
        // constraintManager.addEqualityConstraint(varAnnotSlot, constSlot);
        // }
        // }
    }

    // for use in AnnotatedTypeMirror.toString()
    @Override
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
        return new DefaultAnnotatedTypeFormatter(
                new UnitsAnnotationFormatter(checker),
                checker.hasOption("printVerboseGenerics"),
                checker.hasOption("printAllQualifiers"));
    }

    // for use in generating error outputs
    @Override
    protected AnnotationFormatter createAnnotationFormatter() {
        return new UnitsAnnotationFormatter(checker);
    }
}
