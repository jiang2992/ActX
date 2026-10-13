package com.bin.actx.sandbox.store;

import com.alibaba.opensandbox.sandbox.domain.pool.IdleEntry;
import com.alibaba.opensandbox.sandbox.domain.pool.PoolStateStore;
import com.alibaba.opensandbox.sandbox.domain.pool.StoreCounters;
import com.alibaba.opensandbox.sandbox.domain.pool.TakeIdleResult;
import com.bin.actx.storage.local.dao.SandboxPoolStateDao;
import com.bin.actx.storage.local.entity.SandboxPoolStateEntity;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OpenSandbox状态管理Sqlite实现
 *
 * @author Bin
 * @since 2026/7/28 14:48
 */
@Component
public class SqlitePoolStateStore implements PoolStateStore {

    private final Duration defaultIdleTt = Duration.ofHours(24);
    private final Map<String, Duration> idleTtlByPool = new ConcurrentHashMap<>();

    @Autowired
    private SandboxPoolStateDao dao;

    @Nullable
    @Override
    public synchronized String tryTakeIdle(@NotNull String poolName) {
        LocalDateTime now = LocalDateTime.now();
        List<SandboxPoolStateEntity> sandboxList = dao.findNotExpiredByPoolName(poolName, now);
        if (sandboxList.isEmpty()) {
            return null;
        }

        // 获取并从数据库中删除最早的未过期沙箱
        SandboxPoolStateEntity sandbox = sandboxList.get(0);
        dao.deleteByIds(Collections.singleton(sandbox.getId()));

        return sandbox.getSandboxId();
    }

    @NotNull
    @Override
    public synchronized TakeIdleResult tryTakeIdle(@NotNull String poolName, @NotNull Duration minRemainingTtl) {
        if (minRemainingTtl.isNegative() || minRemainingTtl.isZero()) {
            return TakeIdleResult.of(this.tryTakeIdle(poolName));
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = LocalDateTime.now().plus(minRemainingTtl);
        List<SandboxPoolStateEntity> sandboxList = dao.findNotExpiredByPoolName(poolName, now);
        if (sandboxList.isEmpty()) {
            return TakeIdleResult.getEMPTY();
        }

        // 查找最早可用(TTL高于阈值)的沙箱以及所有存活但需要丢弃(TTL低于阈值)的沙箱
        List<String> discardedAliveSandboxIds = new ArrayList<>();
        List<String> removeIds = new ArrayList<>();
        String result = null;
        for (SandboxPoolStateEntity sandboxPoolStateEntity : sandboxList) {
            removeIds.add(sandboxPoolStateEntity.getId());
            if (sandboxPoolStateEntity.getExpireTime().isAfter(cutoff)) {
                result = sandboxPoolStateEntity.getSandboxId();
                break;
            }
            discardedAliveSandboxIds.add(sandboxPoolStateEntity.getSandboxId());
        }

        // 删除所有存活时间低于阈值的沙箱
        dao.deleteByIds(removeIds);

        return new TakeIdleResult(result, discardedAliveSandboxIds);
    }

    @Override
    @Transaction
    public synchronized void putIdle(@NotNull String poolName, @NotNull String sandboxId) {
        SandboxPoolStateEntity sandbox = dao.findBySandboxId(poolName, sandboxId);
        if (sandbox != null) {
            dao.deleteByIds(Collections.singleton(sandbox.getId()));
        }
        sandbox = new SandboxPoolStateEntity();
        sandbox.setId(IdUtils.uuid());
        sandbox.setPoolName(poolName);
        sandbox.setSandboxId(sandboxId);
        sandbox.setExpireTime(calculateExpireTime(poolName));
        dao.insert(sandbox);
    }

    @Override
    public void removeIdle(@NotNull String poolName, @NotNull String sandboxId) {
        SandboxPoolStateEntity sandbox = dao.findBySandboxId(poolName, sandboxId);
        if (sandbox != null) {
            dao.deleteByIds(Collections.singleton(sandbox.getId()));
        }
    }

    @Override
    public boolean tryAcquirePrimaryLock(@NotNull String poolName, @NotNull String ownerId, @NotNull Duration ttl) {
        // 获取锁，单节点无需处理
        return true;
    }

    @Override
    public boolean renewPrimaryLock(@NotNull String poolName, @NotNull String ownerId, @NotNull Duration ttl) {
        // 续期锁，单节点无需处理
        return true;
    }

    @Override
    public void releasePrimaryLock(@NotNull String poolName, @NotNull String ownerId) {
        // 释放锁，单节点无需处理
    }

    @Override
    @Transaction
    public void reapExpiredIdle(@NotNull String poolName, @NotNull Instant instant) {
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        List<SandboxPoolStateEntity> sandboxList = dao.findExpiredByPoolName(poolName, now);
        if (sandboxList.isEmpty()) {
            return;
        }
        dao.deleteByIds(sandboxList.stream().map(SandboxPoolStateEntity::getId).toList());
    }

    @NotNull
    @Override
    public synchronized List<String> reapExpiredIdle(@NotNull String poolName, @NotNull Instant instant,
            @NotNull Duration minRemainingTtl) {
        if (minRemainingTtl.isNegative() || minRemainingTtl.isZero()) {
            reapExpiredIdle(poolName, instant);
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plus(minRemainingTtl);
        List<SandboxPoolStateEntity> sandboxList = dao.findExpiredByPoolName(poolName, cutoff);

        if (sandboxList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> discardedAliveSandboxIds = new ArrayList<>();
        for (SandboxPoolStateEntity sandboxPoolStateEntity : sandboxList) {
            if (sandboxPoolStateEntity.getExpireTime().isAfter(now)) {
                discardedAliveSandboxIds.add(sandboxPoolStateEntity.getSandboxId());
                break;
            }
        }

        // 删除所有存活时间低于阈值的沙箱
        dao.deleteByIds(sandboxList.stream().map(SandboxPoolStateEntity::getId).toList());

        return discardedAliveSandboxIds;
    }

    @NotNull
    @Override
    public StoreCounters snapshotCounters(@NotNull String poolName) {
        LocalDateTime now = LocalDateTime.now();
        List<SandboxPoolStateEntity> sandboxList = dao.findNotExpiredByPoolName(poolName, now);
        return new StoreCounters(sandboxList.size());
    }

    @NotNull
    @Override
    public List<IdleEntry> snapshotIdleEntries(@NotNull String poolName) {
        LocalDateTime now = LocalDateTime.now();
        List<SandboxPoolStateEntity> sandboxList = dao.findNotExpiredByPoolName(poolName, now);
        return sandboxList.stream()
                .map(s -> new IdleEntry(s.getSandboxId(), s.getExpireTime().atZone(ZoneId.systemDefault()).toInstant()))
                .toList();
    }

    @Nullable
    @Override
    public Integer getMaxIdle(@NotNull String poolName) {
        // 单节点无需共享状态，无需实现
        return null;
    }

    @Override
    public void setMaxIdle(@NotNull String poolName, int maxIdle) {
        // 单节点无需共享状态，无需实现
    }

    private LocalDateTime calculateExpireTime(String poolName) {
        Duration ttl = idleTtlByPool.getOrDefault(poolName, defaultIdleTt);
        return LocalDateTime.now().plus(ttl);
    }

    @Override
    public void setIdleEntryTtl(@NotNull String poolName, @NotNull Duration idleTtl) {
        if (idleTtl.isNegative() || idleTtl.isZero()) {
            throw new IllegalArgumentException("idleTtl must be positive");
        }
        idleTtlByPool.put(poolName, idleTtl);
    }

}
