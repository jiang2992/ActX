package com.bin.actx.config;

import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.storage.local.dao.AttachmentDao;
import com.bin.actx.storage.local.dao.SandboxPoolStateDao;
import com.bin.actx.storage.local.dao.SessionDao;
import com.bin.actx.storage.local.dao.SessionMessageDao;
import com.bin.actx.storage.local.dao.SessionMessageSectionDao;
import com.bin.actx.storage.local.dao.SessionSummaryDao;
import com.bin.actx.storage.local.dao.SubTaskExecuteResultDao;
import com.bin.actx.storage.local.dao.SubTaskRuntimeDao;
import com.bin.actx.storage.local.dao.TaskEvaluateResultDao;
import com.bin.actx.storage.local.dao.TaskOrderDao;
import com.bin.actx.storage.local.dao.TaskPlanDao;
import java.io.File;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

/**
 * JDBI配置
 *
 * @author Bin
 * @since 2026/5/28
 */
@Slf4j
@Configuration
public class JdbiConfig {

    @Autowired
    private AppProperties appProperties;

    @Bean
    public DataSource dataSource() {
        File dbDirFile = new File(appProperties.getStorageLocalDir());
        if (!dbDirFile.exists()) {
            if (dbDirFile.mkdirs()) {
                log.info("Created database directory: {}", appProperties.getStorageLocalDir());
            } else {
                log.error("Failed to create database directory: {}", appProperties.getStorageLocalDir());
            }
        }
        String databasePath = appProperties.getStorageLocalDir() + "actx.db";
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + databasePath);
        log.info("Connect to the database file: {}", databasePath);
        return ds;
    }

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());

        // 设置PRAGMA
        jdbi.useHandle(handle -> {
            handle.execute("PRAGMA journal_mode=WAL");
            handle.execute("PRAGMA synchronous=NORMAL");
            handle.execute("PRAGMA foreign_keys=ON");
        });

        return jdbi;
    }

    @Bean
    public SessionDao sessionDao(Jdbi jdbi) {
        SessionDao dao = jdbi.onDemand(SessionDao.class);
        dao.createTable();
        return dao;
    }

    @Bean
    public SessionMessageDao sessionMessageDao(Jdbi jdbi) {
        SessionMessageDao dao = jdbi.onDemand(SessionMessageDao.class);
        dao.createTable();
        log.debug("Create table 'session_message' (if not exists)");
        return dao;
    }

    @Bean
    public SessionMessageSectionDao sessionMessageSectionDao(Jdbi jdbi) {
        SessionMessageSectionDao dao = jdbi.onDemand(SessionMessageSectionDao.class);
        dao.createTable();
        log.debug("Create table 'session_message_section' (if not exists)");
        return dao;
    }

    @Bean
    public SessionSummaryDao sessionSummaryDao(Jdbi jdbi) {
        SessionSummaryDao dao = jdbi.onDemand(SessionSummaryDao.class);
        dao.createTable();
        log.debug("Create table 'session_summary' (if not exists)");
        return dao;
    }

    @Bean
    public AttachmentDao fileDao(Jdbi jdbi) {
        AttachmentDao dao = jdbi.onDemand(AttachmentDao.class);
        dao.createTable();
        log.debug("Create table 'file' (if not exists)");
        return dao;
    }

    @Bean
    public TaskPlanDao taskPlanDao(Jdbi jdbi) {
        TaskPlanDao dao = jdbi.onDemand(TaskPlanDao.class);
        dao.createTable();
        log.debug("Create table 'task_plan' (if not exists)");
        return dao;
    }

    @Bean
    public TaskOrderDao taskOrderDao(Jdbi jdbi) {
        TaskOrderDao dao = jdbi.onDemand(TaskOrderDao.class);
        dao.createTable();
        log.debug("Create table 'task_order' (if not exists)");
        return dao;
    }

    @Bean
    public SubTaskExecuteResultDao subTaskExecuteResultDao(Jdbi jdbi) {
        SubTaskExecuteResultDao dao = jdbi.onDemand(SubTaskExecuteResultDao.class);
        dao.createTable();
        log.debug("Create table 'sub_task_execute_result' (if not exists)");
        return dao;
    }

    @Bean
    public SubTaskRuntimeDao subTaskRuntimeDao(Jdbi jdbi) {
        SubTaskRuntimeDao dao = jdbi.onDemand(SubTaskRuntimeDao.class);
        dao.createTable();
        log.debug("Create table 'sub_task_runtime' (if not exists)");
        return dao;
    }

    @Bean
    public TaskEvaluateResultDao taskEvaluateResultDao(Jdbi jdbi) {
        TaskEvaluateResultDao dao = jdbi.onDemand(TaskEvaluateResultDao.class);
        dao.createTable();
        log.debug("Create table 'task_evaluate_result' (if not exists)");
        return dao;
    }

    @Bean
    public SandboxPoolStateDao sandboxPoolStateDao(Jdbi jdbi) {
        SandboxPoolStateDao dao = jdbi.onDemand(SandboxPoolStateDao.class);
        dao.createTable();
        log.debug("Create table 'sandbox_pool_state' (if not exists)");
        return dao;
    }

}
