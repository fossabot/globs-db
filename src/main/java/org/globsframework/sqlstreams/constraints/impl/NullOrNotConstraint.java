package org.globsframework.sqlstreams.constraints.impl;

import org.globsframework.metamodel.Field;
import org.globsframework.sqlstreams.constraints.Constraint;
import org.globsframework.sqlstreams.constraints.ConstraintVisitor;

public class NullOrNotConstraint implements Constraint {
    private final Field field;
    private final Boolean checkNull;

    public NullOrNotConstraint(Field field, Boolean checkNull) {
        this.field = field;
        this.checkNull = checkNull;
    }

    public void visit(ConstraintVisitor constraintVisitor) {
        constraintVisitor.visitIsOrNotNull(this);
    }

    public Field getField() {
        return field;
    }

    public boolean checkNull() {
        return checkNull;
    }
}
