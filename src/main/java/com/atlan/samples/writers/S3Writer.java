/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.writers;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3Writer {

    private static final Logger log = LoggerFactory.getLogger(S3Writer.class);

    private S3Client s3Client;

    public S3Writer(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void putExcelFile(ByteArrayOutputStream outputStream, String bucket, String key) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Length", Integer.toString(outputStream.size()));
        metadata.put("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        putFile(metadata, outputStream, bucket, key);
    }

    public void putFile(Map<String, String> metadata, ByteArrayOutputStream outputStream, String bucket, String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .metadata(metadata)
                .build();
        log.info("Writing to: {}/{}", bucket, key);
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputStream.toByteArray()));
        } catch (AwsServiceException e) {
            log.error("Unable to create the S3 object.", e);
            System.exit(1);
        }
    }
}
