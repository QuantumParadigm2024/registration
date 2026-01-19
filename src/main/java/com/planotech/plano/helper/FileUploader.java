package com.planotech.plano.helper;

import com.jcraft.jsch.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FileUploader {

    public static final int SFTP_PORT = 22;
    public static final String SFTP_USER = "dh_gmj3vr";
    public static final String SFTP_PASSWORD = "Srikrishna@0700";
    public static final String SFTP_HOST = "pdx1-shared-a2-03.dreamhost.com";
    public static final String SFTP_DIRECTORY = "/home/dh_gmj3vr/mantramatrix.in/documents/";
    public static final String BASE_URL = "https://mantramatrix.in/documents/";

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

//    public List<String> handleFileUpload(List<MultipartFile> files) {
//        List<String> filelist = Collections.synchronizedList(new ArrayList<>());
//        CompletableFuture[] futures = new CompletableFuture[files.size()];
//        for (int i = 0; i < files.size(); i++) {
//            MultipartFile file = files.get(i);
//            try {
//                byte[] fileBytes = file.getBytes();
//                String originalFilename = file.getOriginalFilename();
//                ;
//                assert originalFilename != null;
//                String uniqueFileName = generateUniqueFileName(originalFilename);
//
//                futures[i] = CompletableFuture.runAsync(() -> {
//                    String fileUrl = uploadFileViaSFTP(fileBytes, uniqueFileName);
//                    filelist.add(fileUrl);
//                }, executorService);
//            } catch (IOException e) {
//                throw new RuntimeException("File Not supported");
//            }
//        }
//        return filelist;
//    }

    public CompletableFuture<List<String>> handleFileUploadAsync(List<MultipartFile> files) {
        System.out.println("handleFileUploadAsync");
        files.forEach(file -> System.out.println(file.getOriginalFilename()));
        return CompletableFuture.supplyAsync(() -> {
            List<String> filelist = new ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    byte[] fileBytes = file.getBytes();
                    String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
                    String url = uploadFileViaSFTP(fileBytes, uniqueFileName);
                    filelist.add(url);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }

            return filelist;
        }, executorService);
    }

    private String uploadFileViaSFTP(byte[] fileBytes, String fileName) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            try (InputStream fileInputStream = new ByteArrayInputStream(fileBytes)) {
                sftpChannel.put(fileInputStream, SFTP_DIRECTORY + fileName);
            }
            return BASE_URL + fileName;
        } catch (JSchException | SftpException | IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (sftpChannel != null) {
                try {
                    sftpChannel.disconnect();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            if (session != null) {
                try {
                    session.disconnect();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6) + "_" + originalFilename;
    }

    public ResponseEntity<Map<String, Object>> deleteFile(String fileUrl) {
        Map<String, Object> response = new HashMap<>();
        String fileName = fileUrl.replace(BASE_URL, "");
        String remoteFilePath = SFTP_DIRECTORY + fileName;
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            sftpChannel.rm(remoteFilePath);
            response.put("code", 200);
            response.put("status", "success");
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok(response);

        } catch (JSchException | SftpException | RuntimeException e) {
            throw new RuntimeException(e.getMessage());

        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }


    public ResponseEntity<Map<String, Object>> deleteUrl(String fileurl) {
        Map<String, Object> response = new HashMap<>();
        String fileName = fileurl.replace(BASE_URL, "");
        String remoteFilePath = SFTP_DIRECTORY + fileName;
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.rm(remoteFilePath);

            response.put("code", 200);
            response.put("status", "success");
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok(response);

        } catch (JSchException | SftpException | RuntimeException e) {
            throw new RuntimeException(e.getMessage());

        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private static final List<String> VALID_EXTENSIONS = Arrays.asList(
            ".pdf", ".xls", ".xlsx", ".doc", ".docx", ".png", ".jpg", ".jpeg", ".svg", ".cdr",
            ".fbx", ".ai", ".psd", ".eps", ".tiff", ".gif", ".mp4", ".mov", ".mp3", ".wav",
            ".zip", ".rar", ".7z", ".proj", ".pr", ".ppt", ".pptx"
    );
}