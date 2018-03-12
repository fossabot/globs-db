package org.globsframework.sqlstreams.drivers.mongodb;

import com.mongodb.client.MongoDatabase;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.sqlstreams.BulkDbRequest;
import org.globsframework.sqlstreams.SqlRequest;
import org.globsframework.sqlstreams.UpdateBuilder;
import org.globsframework.sqlstreams.constraints.Constraint;
import org.globsframework.streams.accessors.*;
import org.globsframework.streams.accessors.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MongoUpdateBuilder implements UpdateBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoSelectBuilder.class);
    private final MongoDatabase mongoDatabase;
    private final GlobType globType;
    private MongoDbService sqlService;
    private final Constraint constraint;
    private final Map<Field, Accessor> fieldsValues = new HashMap<>();

    public MongoUpdateBuilder(MongoDatabase mongoDatabase, GlobType globType, MongoDbService sqlService, Constraint constraint) {
        this.mongoDatabase = mongoDatabase;
        this.globType = globType;
        this.sqlService = sqlService;
        this.constraint = constraint;
    }

    public UpdateBuilder updateUntyped(Field field, Object value) {
        field.safeVisit(new FieldValueVisitor() {
            public void visitInteger(IntegerField field, Integer value) {
                update(field, value);
            }

            public void visitDouble(DoubleField field, Double value) {
                update(field, value);
            }

            public void visitString(StringField field, String value) {
                update(field, value);
            }

            public void visitBoolean(BooleanField field, Boolean value) {
                update(field, value);
            }

            public void visitLong(LongField field, Long value) {
                update(field, value);
            }

            public void visitBlob(BlobField field, byte[] value) {
                update(field, value);
            }
        }, value);

        return this;
    }

    public UpdateBuilder updateUntyped(Field field, Accessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(IntegerField field, IntegerAccessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(IntegerField field, Integer value) {
        fieldsValues.put(field, new ValueIntegerAccessor(value));
        return this;
    }

    public UpdateBuilder update(LongField field, LongAccessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(LongField field, Long value) {
        fieldsValues.put(field, new ValueLongAccessor(value));
        return this;
    }

    public UpdateBuilder update(DoubleField field, DoubleAccessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(DoubleField field, Double value) {
        fieldsValues.put(field, new ValueDoubleAccessor(value));
        return this;
    }

    public UpdateBuilder update(StringField field, StringAccessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(StringField field, String value) {
        fieldsValues.put(field, new ValueStringAccessor(value));
        return this;
    }

    public UpdateBuilder update(BooleanField field, BooleanAccessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public UpdateBuilder update(BooleanField field, Boolean value) {
        fieldsValues.put(field, new ValueBooleanAccessor(value));
        return this;
    }

    public UpdateBuilder update(BlobField field, byte[] value) {
        fieldsValues.put(field, new ValueBlobAccessor(value));
        return this;
    }

    public UpdateBuilder update(BlobField field, BlobAccessor accessor) {
        fieldsValues.put(field, accessor);
        return this;
    }

    public SqlRequest getRequest() {
        return new MongoUpdateSqlRequest(sqlService, mongoDatabase, globType, constraint, fieldsValues, false);
    }

    public BulkDbRequest getBulkRequest() {
        return new MongoUpdateSqlRequest(sqlService, mongoDatabase, globType, constraint, fieldsValues, true);
    }

}
