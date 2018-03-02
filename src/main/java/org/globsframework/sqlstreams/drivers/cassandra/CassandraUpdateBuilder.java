package org.globsframework.sqlstreams.drivers.cassandra;

import com.datastax.driver.core.Session;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.sqlstreams.SqlRequest;
import org.globsframework.sqlstreams.UpdateBuilder;
import org.globsframework.sqlstreams.constraints.Constraint;
import org.globsframework.streams.accessors.*;
import org.globsframework.streams.accessors.utils.*;

import java.util.HashMap;
import java.util.Map;

public class CassandraUpdateBuilder implements UpdateBuilder {
    private Map<Field, Accessor> values = new HashMap<Field, Accessor>();
    private GlobType globType;
    private DbCasandra sqlService;
    private Constraint constraint;
    private Session session;

    public CassandraUpdateBuilder(Session session, GlobType globType, DbCasandra sqlService,
                                  Constraint constraint) {
        this.session = session;
        this.globType = globType;
        this.sqlService = sqlService;
        this.constraint = constraint;
    }

    public UpdateBuilder updateUntyped(Field field, final Object value) {
        field.safeVisit(new FieldVisitor() {
            public void visitInteger(IntegerField field) throws Exception {
                update(field, (Integer) value);
            }

            public void visitLong(LongField field) throws Exception {
                update(field, (Long) value);
            }

            public void visitDouble(DoubleField field) throws Exception {
                update(field, (Double) value);
            }

            public void visitString(StringField field) throws Exception {
                update(field, (String) value);
            }

            public void visitBoolean(BooleanField field) throws Exception {
                update(field, (Boolean) value);
            }

            public void visitBlob(BlobField field) throws Exception {
                update(field, (byte[]) value);
            }

        });
        return this;
    }

    public UpdateBuilder updateUntyped(Field field, Accessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(IntegerField field, IntegerAccessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(IntegerField field, Integer value) {
        return update(field, new ValueIntegerAccessor(value));
    }

    public UpdateBuilder update(LongField field, LongAccessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(LongField field, Long value) {
        return update(field, new ValueLongAccessor(value));
    }

    public UpdateBuilder update(DoubleField field, DoubleAccessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(DoubleField field, Double value) {
        return update(field, new ValueDoubleAccessor(value));
    }

    public UpdateBuilder update(StringField field, StringAccessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(StringField field, String value) {
        return update(field, new ValueStringAccessor(value));
    }

    public UpdateBuilder update(BooleanField field, BooleanAccessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(BooleanField field, Boolean value) {
        return update(field, new ValueBooleanAccessor(value));
    }

    public UpdateBuilder update(BlobField field, byte[] value) {
        return update(field, new ValueBlobAccessor(value));
    }

    public UpdateBuilder update(BlobField field, BlobAccessor accessor) {
        values.put(field, accessor);
        return this;
    }

    public SqlRequest getRequest() {
        try {
            return new CassandraUpdateRequest(globType, constraint, values, session, sqlService);
        } finally {
            values.clear();
        }
    }
}
