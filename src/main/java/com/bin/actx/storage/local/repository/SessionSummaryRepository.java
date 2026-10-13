package com.bin.actx.storage.local.repository;

import com.bin.actx.message.SessionSummary;
import com.bin.actx.storage.local.dao.SessionSummaryDao;
import com.bin.actx.storage.local.entity.SessionSummaryEntity;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 会话摘要存储
 *
 * @author Bin
 * @since 2026/5/13 16:39
 */
@Component
public class SessionSummaryRepository {

    @Autowired
    private SessionSummaryDao sessionSummaryDao;

    public SessionSummary getLastSessionSummary(String sessionId) {
        SessionSummary sessionSummary = new SessionSummary(sessionId);
        SessionSummaryEntity sessionSummaryEntity = sessionSummaryDao.findLastBySessionId(sessionId);
        if (sessionSummaryEntity != null) {
            SessionSummary.Record record = new SessionSummary.Record(sessionSummaryEntity.getStartIndex(),
                    sessionSummaryEntity.getEndIndex(), sessionSummaryEntity.getContent());
            sessionSummary.setLastSummary(record);
        }
        return sessionSummary;
    }

    public void saveSessionSummary(SessionSummary sessionSummary) {
        SessionSummaryEntity entity = new SessionSummaryEntity();
        entity.setId(IdUtils.uuid());
        entity.setSessionId(sessionSummary.getSessionId());
        entity.setStartIndex(sessionSummary.getLastSummary().getStartIndex());
        entity.setEndIndex(sessionSummary.getLastSummary().getEndIndex());
        entity.setContent(sessionSummary.getLastSummary().getContent());
        sessionSummaryDao.upsert(entity);
    }

}
