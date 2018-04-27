package org.globsframework.sqlstreams.drivers.jdbc;

import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Glob;
import org.globsframework.model.GlobList;
import org.globsframework.sqlstreams.SelectQuery;
import org.globsframework.sqlstreams.SqlService;
import org.globsframework.sqlstreams.accessors.SqlAccessor;
import org.globsframework.sqlstreams.constraints.Constraint;
import org.globsframework.sqlstreams.drivers.jdbc.impl.ValueConstraintVisitor;
import org.globsframework.sqlstreams.drivers.jdbc.impl.WhereClauseConstraintVisitor;
import org.globsframework.sqlstreams.drivers.jdbc.request.SqlQueryBuilder;
import org.globsframework.sqlstreams.utils.StringPrettyWriter;
import org.globsframework.streams.GlobStream;
import org.globsframework.utils.exceptions.ItemNotFound;
import org.globsframework.utils.exceptions.TooManyItems;
import org.globsframework.utils.exceptions.UnexpectedApplicationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

public class SqlSelectQuery implements SelectQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlSelectQuery.class);
    private Set<GlobType> globTypes = new HashSet<GlobType>();
    private Constraint constraint;
    private BlobUpdater blobUpdater;
    private boolean autoClose;
    private Map<Field, SqlAccessor> fieldToAccessorHolder;
    private SqlService sqlService;
    private final List<SqlQueryBuilder.Order> orders;
    private final int top;
    private Set<Field> distinct;
    private PreparedStatement preparedStatement;
    private String sql;

    public SqlSelectQuery(Connection connection, Constraint constraint,
                          Map<Field, SqlAccessor> fieldToAccessorHolder, SqlService sqlService,
                          BlobUpdater blobUpdater, boolean autoClose, List<SqlQueryBuilder.Order> orders, int top, Set<Field> distinct,
                          String externalRequest) {
        this.constraint = constraint;
        this.blobUpdater = blobUpdater;
        this.autoClose = autoClose;
        this.fieldToAccessorHolder = new HashMap<>(fieldToAccessorHolder);
        this.sqlService = sqlService;
        this.orders = orders;
        this.top = top;
        this.distinct = distinct;
        if (externalRequest == null) {
            sql = prepareSqlRequest();
        }
        else {
            sql = externalRequest;
        }
        try {
            this.preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
        } catch (SQLException e) {
            throw new UnexpectedApplicationState("for request " + sql, e);
        }
        if (externalRequest != null) {
            initIndexFromMetadata(fieldToAccessorHolder, sqlService);
        }
    }

    private void initIndexFromMetadata(Map<Field, SqlAccessor> fieldToAccessorHolder, SqlService sqlService) {
        try {
            ResultSetMetaData metaData = preparedStatement.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                if (!updateSqlIndex(fieldToAccessorHolder, sqlService, i, columnName)) {
                    LOGGER.warn("column " + columnName + " not found in type got " + fieldToAccessorHolder.keySet());
                }
            }
        } catch (SQLException e) {
            String msg = "Fail to analyse metadata of " + sql;
            LOGGER.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private boolean updateSqlIndex(Map<Field, SqlAccessor> fieldToAccessorHolder, SqlService sqlService, int i, String columnName) {
        for (Map.Entry<Field, SqlAccessor> fieldSqlAccessorEntry : fieldToAccessorHolder.entrySet()) {
            if (sqlService.getColumnName(fieldSqlAccessorEntry.getKey()).equals(columnName)) {
                fieldSqlAccessorEntry.getValue().setIndex(i);
                return true;
            }
        }
        return false;
    }

    private String prepareSqlRequest() {
        int index = 0;
        StringPrettyWriter prettyWriter = new StringPrettyWriter();
        prettyWriter.append("select ");
        for (Iterator<Map.Entry<Field, SqlAccessor>> iterator = fieldToAccessorHolder.entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<Field, SqlAccessor> fieldAndAccessor = iterator.next();
            fieldAndAccessor.getValue().setIndex(++index);
            GlobType globType = fieldAndAccessor.getKey().getGlobType();
            globTypes.add(globType);
            String tableName = sqlService.getTableName(globType);
            if (distinct.contains(fieldAndAccessor.getKey())) {
                prettyWriter.append(" DISTINCT ");
            }
            prettyWriter.append(tableName)
                  .append(".")
                  .append(sqlService.getColumnName(fieldAndAccessor.getKey()))
                  .appendIf(", ", iterator.hasNext());
        }
        StringPrettyWriter where = null;
        if (constraint != null) {
            where = new StringPrettyWriter();
            where.append(" WHERE ");
            constraint.visit(new WhereClauseConstraintVisitor(where, sqlService, globTypes));
        }

        prettyWriter.append(" from ");
        for (Iterator it = globTypes.iterator(); it.hasNext(); ) {
            GlobType globType = (GlobType) it.next();
            prettyWriter.append(sqlService.getTableName(globType))
                  .appendIf(", ", it.hasNext());
        }
        if (where != null) {
            prettyWriter.append(where.toString());
        }

        if (!orders.isEmpty()) {
            prettyWriter.append(" ORDER BY ");
            for (SqlQueryBuilder.Order order : orders) {
                prettyWriter.append(sqlService.getColumnName(order.field));
                if (order.asc) {
                    prettyWriter.append(" ASC");
                }
                else {
                    prettyWriter.append(" DESC");
                }
                prettyWriter.append(", ");
            }
            prettyWriter.removeLast().removeLast();
        }
        if (top != -1) {
            prettyWriter.append(" LIMIT " + top);
        }
        return prettyWriter.toString();
    }

    public Stream<?> executeAsStream() {
        throw new RuntimeException("Not implemented");
    }

    public GlobStream execute() {
        if (preparedStatement == null) {
            throw new UnexpectedApplicationState("Query closed " + sql);
        }
        try {
            if (constraint != null) {
                constraint.visit(new ValueConstraintVisitor(preparedStatement, blobUpdater));
            }
            return new SqlGlobStream(preparedStatement.executeQuery(), fieldToAccessorHolder, this);
        } catch (SQLException e) {
            throw new UnexpectedApplicationState("for request : " + sql, e);
        }
    }

    public GlobList executeAsGlobs() {
        GlobStream globStream = execute();
        AccessorGlobsBuilder accessorGlobsBuilder = AccessorGlobsBuilder.init(globStream);
        GlobList result = new GlobList();
        while (globStream.next()) {
            result.addAll(accessorGlobsBuilder.getGlobs());
        }
        return result;
    }

    public Glob executeUnique() throws ItemNotFound, TooManyItems {
        GlobList globs = executeAsGlobs();
        if (globs.size() == 1) {
            return globs.get(0);
        }
        if (globs.isEmpty()) {
            throw new ItemNotFound("No result returned for: " + sql);
        }
        throw new TooManyItems("Too many results for: " + sql);
    }

    public void resultSetClose() {
        if (autoClose) {
            close();
        }
    }

    public void close() {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
                preparedStatement = null;
            } catch (SQLException e) {
                throw new UnexpectedApplicationState("PreparedStatement close fail", e);
            }
        }
    }
}
