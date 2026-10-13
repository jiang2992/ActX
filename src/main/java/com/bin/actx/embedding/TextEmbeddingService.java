package com.bin.actx.embedding;

import com.bin.actx.exception.EmbeddingException;
import com.bin.actx.model.EmbeddingModelManager;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Text embedding service
 *
 * @author Bin
 * @since 2026/9/4 15:37
 */
@Component
public class TextEmbeddingService {

    @Autowired
    private EmbeddingModelManager embeddingModelManager;

    public float[] embed(String text) {
        EmbeddingModel embeddingModel = embeddingModelManager.getFirstModel();
        if (embeddingModel == null) {
            throw new EmbeddingException("No embedding model available");
        }
        return embeddingModel.embed(text);
    }

}
