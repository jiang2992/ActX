package com.bin.actx.sql.datasource;

import javax.sql.DataSource;
import lombok.Getter;

/**
 * 数据源实例
 *
 * @author Bin
 * @since 2026/8/25 16:54
 */
@Getter
public class DataSourceInstance {

    private final DataSourceInfo info;

    private final DatabaseType databaseType;

    private final DataSource dataSource;

    public DataSourceInstance(DataSourceInfo info, DatabaseType databaseType, DataSource dataSource) {
        this.info = info;
        this.databaseType = databaseType;
        this.dataSource = dataSource;
    }

}
