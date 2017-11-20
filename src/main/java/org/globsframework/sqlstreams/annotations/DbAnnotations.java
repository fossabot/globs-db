package org.globsframework.sqlstreams.annotations;

import org.globsframework.metamodel.GlobModel;
import org.globsframework.metamodel.impl.DefaultGlobModel;

public class DbAnnotations {
    public final static GlobModel MODEL = new DefaultGlobModel(DbFieldName.TYPE, IsDbRef.TYPE, IsBigDecimal.TYPE);
}
