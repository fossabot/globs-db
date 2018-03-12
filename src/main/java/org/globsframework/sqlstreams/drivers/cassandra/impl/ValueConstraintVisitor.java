package org.globsframework.sqlstreams.drivers.cassandra.impl;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import org.globsframework.metamodel.Field;
import org.globsframework.sqlstreams.constraints.ConstraintVisitor;
import org.globsframework.sqlstreams.constraints.OperandVisitor;
import org.globsframework.sqlstreams.constraints.impl.*;
import org.globsframework.utils.exceptions.UnexpectedApplicationState;

public class ValueConstraintVisitor extends SqlValueFieldVisitor implements ConstraintVisitor, OperandVisitor {
    private int index = 0;

    public ValueConstraintVisitor(BoundStatement bind) {
        super(bind);
    }


    public void visitEqual(EqualConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitNotEqual(NotEqualConstraint constraint) {
        constraint.getLeftOperand().visitOperand(this);
        constraint.getRightOperand().visitOperand(this);
    }

    private void visitBinary(BinaryOperandConstraint operandConstraint) {
        operandConstraint.getLeftOperand().visitOperand(this);
        operandConstraint.getRightOperand().visitOperand(this);
    }

    private void visitBinary(BinaryConstraint constraint) {
        constraint.getLeftConstraint().visit(this);
        constraint.getRightConstraint().visit(this);
    }

    public void visitAnd(AndConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitOr(OrConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitLessThan(LessThanConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitBiggerThan(BiggerThanConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitStrictlyBiggerThan(StrictlyBiggerThanConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitStrictlyLesserThan(StrictlyLesserThanConstraint constraint) {
        visitBinary(constraint);
    }

    public void visitIn(InConstraint inConstraint) {
        Field field = inConstraint.getField();
        for (Object value : inConstraint.getValues()) {
            setValue(value, ++index);
            field.safeVisit(this);
        }
    }

    public void visitIsOrNotNull(NullOrNotConstraint constraint) {
    }

    public void visitNotIn(NotInConstraint constraint) {

    }

    public void visitContains(Field field, String value, boolean contains) {

    }

    public void visitValueOperand(ValueOperand value) {
        Object o = value.getValue();
        if (o == null) {
            throw new UnexpectedApplicationState("null not supported, Should be explicit (is null) ");
        }
        setValue(o, ++index);
        value.getField().safeVisit(this);
    }

    public void visitAccessorOperand(AccessorOperand accessorOperand) {
        setValue(accessorOperand.getAccessor().getObjectValue(), ++index);
        accessorOperand.getField().safeVisit(this);
    }

    public void visitFieldOperand(Field field) {
    }
}
