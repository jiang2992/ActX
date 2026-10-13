package com.bin.actx.tool.kit.data;

import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.sql.SqlExecuteResult;
import com.bin.actx.sql.SqlGateway;
import com.bin.actx.sql.SqlRouteRequest;
import com.bin.actx.sql.datasource.DataSourceInfo;
import com.bin.actx.sql.datasource.DataSourceManager;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 数据源工具类 提供数据源管理、SQL执行等功能
 *
 * @author Bin
 * @since 2026/8/26 11:33
 */
@Slf4j
@Toolkit(name = "数据源", description = "允许管理外部数据源、在指定数据源中执行命令等", autoDiscover = false)
public class DataSourceToolkit {

    @Autowired
    private DataSourceManager dataSourceManager;
    @Autowired
    private SqlGateway sqlGateway;
    @Autowired
    private ResponseContextManager responseContextManager;

    @Tool(name = "list_data_sources", description = "列出当前可用的数据源")
    public ToolCallResult<List<DataSourceInfo>> listDataSources() {
        List<DataSourceInfo> infoList = dataSourceManager.getAllDataSourceInfos();
        return ToolCallResult.of(infoList, infoList.isEmpty() ? "当前没有可用的数据源" : "找到%s个可用的数据源".formatted(infoList.size()));
    }

    @Tool(name = "execute_read_sql", description = "在指定数据源中执行只读SQL（支持批量）")
    public ToolCallResult<List<SqlExecuteResult>> executeReadSql(
            @ToolParam(description = "数据源id") String dataSourceId,
            @ToolParam(description = "SQL语句（最多10条）") List<String> sqlList,
            ToolContext toolContext
    ) {
        if (sqlList.size() > 10) {
            return ToolCallResult.of(null, "Up to 10 SQL statements are supported.");
        }

        ResponseContext responseContext = responseContextManager.getByToolContext(toolContext.getContext());
        SqlRouteRequest request = new SqlRouteRequest();
        request.setDataSourceId(dataSourceId);
        request.setSqlList(sqlList);
        request.setResponseContext(responseContext);
        request.setDataPermissionEnable(true);
        List<SqlExecuteResult> result = sqlGateway.route(request);
        return ToolCallResult.of(result);
    }

}
