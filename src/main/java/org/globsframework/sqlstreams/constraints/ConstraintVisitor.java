package org.globsframework.sqlstreams.constraints;

import org.globsframework.metamodel.Field;
import org.globsframework.sqlstreams.constraints.impl.*;

public interface ConstraintVisitor {
    void visitEqual(EqualConstraint constraint);

    void visitNotEqual(NotEqualConstraint constraint);

    void visitAnd(AndConstraint constraint);

    void visitOr(OrConstraint constraint);

    void visitLessThan(LessThanConstraint constraint);

    void visitBiggerThan(BiggerThanConstraint constraint);

    void visitStrictlyBiggerThan(StrictlyBiggerThanConstraint constraint);

    void visitStrictlyLesserThan(StrictlyLesserThanConstraint constraint);

    void visitIn(InConstraint constraint);

    void visitIsOrNotNull(NullOrNotConstraint constraint);

    void visitNotIn(NotInConstraint constraint);

    void visitContains(Field field, String value, boolean contains);
}
