package com.patent.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件服务接口（MinIO）
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file       文件
     * @param objectName 对象名称（路径）
     * @return 文件路径
     */
    String uploadFile(MultipartFile file, String objectName);

    /**
     * 获取文件流
     *
     * @param objectName 对象名称
     * @return 输入流
     */
    InputStream getFile(String objectName);

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    void deleteFile(String objectName);

    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @return 访问URL
     */
    String getFileUrl(String objectName);
}
