package com.planotech.plano.helper;

import com.planotech.plano.model.Event;
import com.planotech.plano.model.EventMedia;
import com.planotech.plano.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EventMediaAsyncService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    FileUploader fileUploader;

    private static final Logger log =
            LoggerFactory.getLogger(EventMediaAsyncService.class);

    @Async
    public void uploadMediaAsync(
            Event event,
            MultiValueMap<String, MultipartFile> files) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        files.forEach((mediaType, multipartFiles) -> {
            if ("event".equalsIgnoreCase(mediaType)) {
                return;
            }

            List<UploadFileData> safeFiles = multipartFiles.stream()
                    .map(file -> {
                        try {
                            return new UploadFileData(
                                    file.getOriginalFilename(),
                                    file.getBytes()
                            );
                        } catch (IOException e) {
                            throw new RuntimeException("File read failed", e);
                        }
                    })
                    .toList();

            CompletableFuture<Void> future =
                    fileUploader.handleFileUploadAsync(safeFiles)
                            .thenAccept(urls -> {
                                log.info("URLs received in Event service: {}", urls);
                                EventMedia media = new EventMedia();
                                media.setEvent(event);
                                media.setMediaType(mediaType);
                                media.setMediaUrls(urls);
                                event.getMediaList().add(media);
                            }).exceptionally(ex -> {
                                log.error("Async upload failed", ex);
                                throw new RuntimeException(ex.getMessage());
                            });

            futures.add(future);
        });

        CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() ->
                {
                    System.out.println(event);
//                    eventRepository.save(event)
                })
                .exceptionally(ex -> {
                    throw new RuntimeException(ex.getMessage());
                });
    }
}
