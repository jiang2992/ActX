package com.bin.actx.storage.local.repository;

import com.bin.actx.message.SessionCollection;
import com.bin.actx.message.SessionInfo;
import com.bin.actx.storage.local.dao.SessionDao;
import com.bin.actx.storage.local.entity.SessionEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 会话存储
 *
 * @author Bin
 * @since 2026/5/13 16:39
 */
@Component
public class SessionRepository {

    @Autowired
    private SessionDao sessionDao;

    public SessionCollection getUserSessions(String userId, String type) {
        List<SessionEntity> sessionEntityList = sessionDao.findByUserId(userId, type);
        List<SessionInfo> sessionInfoList = sessionEntityList.stream()
                .map(sessionEntity -> new SessionInfo(sessionEntity.getUserId(), sessionEntity.getId(), sessionEntity.getName(),
                        sessionEntity.getLastTokenTotal(),
                        sessionEntity.getLastMessageSort(), sessionEntity.getLastMessageTime())).toList();
        return new SessionCollection(userId, sessionInfoList);
    }

    public SessionInfo getSession(String sessionId) {
        SessionEntity sessionEntity = sessionDao.findById(sessionId);
        if (sessionEntity == null) {
            return null;
        }
        return new SessionInfo(sessionEntity.getUserId(), sessionEntity.getId(), sessionEntity.getName(), sessionEntity.getLastTokenTotal(),
                sessionEntity.getLastMessageSort(), sessionEntity.getLastMessageTime());
    }

    public SessionInfo getLatestSession(String userId, String type) {
        SessionEntity sessionEntity = sessionDao.findLatestByUserId(userId, type);
        if (sessionEntity == null) {
            return null;
        }
        return new SessionInfo(sessionEntity.getUserId(), sessionEntity.getId(), sessionEntity.getName(), sessionEntity.getLastTokenTotal(),
                sessionEntity.getLastMessageSort(), sessionEntity.getLastMessageTime());
    }

    public boolean containsSession(String sessionId) {
        return sessionDao.findById(sessionId) != null;
    }

    public boolean addSession(String type, SessionInfo chatSessionInfo) {
        if (containsSession(chatSessionInfo.getSessionId())) {
            return false;
        }
        SessionEntity sessionEntity = new SessionEntity();
        sessionEntity.setId(chatSessionInfo.getSessionId());
        sessionEntity.setUserId(chatSessionInfo.getUserId());
        sessionEntity.setName(chatSessionInfo.getName());
        sessionEntity.setType(type);
        sessionEntity.setLastTokenTotal(0);
        sessionEntity.setLastMessageSort(0);
        sessionDao.upsert(sessionEntity);
        return true;
    }

    public void deleteSession(String sessionId) {
        sessionDao.softDelete(sessionId);
    }

}
