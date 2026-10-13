package com.bin.actx.sql;

import com.bin.actx.context.ResponseContext;
import java.util.List;
import lombok.Data;

@Data
public class SqlRouteRequest {

    private String dataSourceId;

    private List<String> sqlList;

    private ResponseContext responseContext;

    private boolean dataPermissionEnable = false;

}
