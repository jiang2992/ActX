package com.bin.actx.file;

import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.exception.FileDeleteException;
import com.bin.actx.exception.FileSaveException;
import com.bin.actx.message.attachment.AttachmentPreview;
import com.bin.actx.message.attachment.MediaAttachmentPreview;
import com.bin.actx.message.attachment.TextAttachmentPreview;
import com.bin.actx.storage.local.dao.AttachmentDao;
import com.bin.actx.storage.local.entity.AttachmentEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.jiang.tools.file.FileUtils;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentManager {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AttachmentDao attachmentDao;

    @Autowired
    private FileManager fileManager;

    /**
     * 保存附件
     *
     * @param multipartFile 上传的附件
     * @return 附件信息
     */
    public AttachmentInfo saveAttachment(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        try {
            // 生成唯一文件ID
            String fileId = IdUtils.uuid();

            // 获取原始文件名和扩展名
            String originalFilename = multipartFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            // 构建存储路径
            String fileName = fileId + "." + extension;
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path attachmentPath = rootPath.resolve("attachment/").normalize();
            Path fullPath = attachmentPath.resolve(fileName).normalize();

            // 确保目录存在
            Files.createDirectories(attachmentPath);

            // 保存文件
            Files.copy(multipartFile.getInputStream(), fullPath);

            // 创建文件信息对象
            AttachmentInfo attachmentInfo = new AttachmentInfo();
            attachmentInfo.setId(fileId);
            attachmentInfo.setFileName(originalFilename != null ? originalFilename : fileName);
            attachmentInfo.setPath(rootPath.relativize(fullPath).toString());
            attachmentInfo.setSize(multipartFile.getSize());
            attachmentInfo.setExtension(extension);
            attachmentInfo.setIsTextFile(FileUtils.isTextFile(fullPath));

            // 保存到存储中
            AttachmentEntity attachmentEntity = new AttachmentEntity();
            attachmentEntity.setId(fileId);
            attachmentEntity.setFileName(attachmentInfo.getFileName());
            attachmentEntity.setPath(attachmentInfo.getPath());
            attachmentEntity.setSize(multipartFile.getSize());
            attachmentEntity.setExtension(attachmentInfo.getExtension());
            attachmentEntity.setIsTextFile(attachmentInfo.getIsTextFile());
            attachmentDao.upsert(attachmentEntity);

            return attachmentInfo;
        } catch (IOException e) {
            throw new FileSaveException("File saving failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return false;
        }

        try {
            // 从存储中获取文件信息
            AttachmentEntity attachmentEntity = attachmentDao.findById(fileId);
            if (attachmentEntity == null) {
                return false;
            }

            // 从存储中删除记录
            attachmentDao.delete(fileId);

            // 删除物理文件
            Path filePath = Paths.get(appProperties.getFileStorageDir()).resolve(attachmentEntity.getPath());
            Files.deleteIfExists(filePath);

            return true;
        } catch (IOException e) {
            throw new FileDeleteException("File deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取附件信息
     *
     * @param fileId 附件ID
     * @return 附件信息，如果不存在则返回null
     */
    public AttachmentInfo getAttachment(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return null;
        }

        AttachmentEntity attachmentEntity = attachmentDao.findById(fileId);
        if (attachmentEntity == null) {
            return null;
        }

        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.setId(attachmentEntity.getId());
        attachmentInfo.setFileName(attachmentEntity.getFileName());
        attachmentInfo.setPath(attachmentEntity.getPath());
        attachmentInfo.setSize(attachmentEntity.getSize());
        attachmentInfo.setExtension(attachmentEntity.getExtension());
        attachmentInfo.setIsTextFile(attachmentEntity.getIsTextFile());
        return attachmentInfo;
    }

    public AttachmentPreview toAttachmentPreview(AttachmentInfo attachmentInfo) {
        if (attachmentInfo.getIsTextFile()) {
            TextAttachmentPreview textAttachmentPreview = new TextAttachmentPreview();
            textAttachmentPreview.setName(attachmentInfo.getFileName());
            textAttachmentPreview.setPath(attachmentInfo.getPath());

            FileContent fileContent = fileManager.readFileContent(attachmentInfo.getPath(), 0, 50, 10000);
            textAttachmentPreview.setIsAll(fileContent.getIsEnd());
            textAttachmentPreview.setContent(String.join("\n", fileContent.getContent()));

            return textAttachmentPreview;
        } else {
            MediaAttachmentPreview mediaAttachmentPreview = new MediaAttachmentPreview();
            mediaAttachmentPreview.setName(attachmentInfo.getFileName());
            mediaAttachmentPreview.setPath(attachmentInfo.getPath());
            List<Media> mediaList = fileManager.getMediaList(Collections.singletonList(attachmentInfo.getPath()));
            mediaAttachmentPreview.setMedia(mediaList.get(0));
            return mediaAttachmentPreview;
        }
    }

}
