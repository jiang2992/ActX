package com.bin.actx.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 元数据构建器
 *
 * @author Bin
 * @since 2026/5/12 15:20
 */
public class MetadataBuilder<K, V> {

    private final Map<K, V> metadata = new HashMap<>();

    public static <K, V> MetadataBuilder<K, V> of(K key, V value) {
        return new MetadataBuilder<K, V>().key(key, value);
    }

    public MetadataBuilder<K, V> key(K key, V value) {
        metadata.put(key, value);
        return this;
    }

    public Map<K, V> build() {
        return metadata;
    }

}
