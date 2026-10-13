package com.bin.actx.storage.local.entity;

import java.time.LocalDateTime;
import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 沙箱池状态实体
 *
 * @author Bin
 * @since 2026/7/28 14:53
 */
@Data
public class SandboxPoolStateEntity {

    @ColumnName("id")
    private String id;

    @ColumnName("pool_name")
    private String poolName;

    @ColumnName("sandbox_id")
    private String sandboxId;

    @ColumnName("expire_time")
    private LocalDateTime expireTime;

}
