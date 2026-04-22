package com.spy.copywritingaiagentserver.file;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    public void deleteObject(String key) {
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(cosClientConfig.getBucket(), key);
        cosClient.deleteObject(deleteObjectRequest);
    }
}
