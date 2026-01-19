package com.planotech.plano.helper;

import com.planotech.plano.model.Event;
import com.planotech.plano.model.EventMedia;
import com.planotech.plano.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EventMediaAsyncService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    FileUploader fileUploader;

    @Async
    public void uploadMediaAsync(
            Event event,
            MultiValueMap<String, MultipartFile> files) {
        System.out.println("uploadMediaAsync");
        System.out.println(files.toString());
        files.forEach((mediaType, file) -> {
            System.out.println(mediaType + " -> " + file.size());
        });
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        files.forEach((mediaType, multipartFiles) -> {
            if ("event".equalsIgnoreCase(mediaType)) {
                return;
            }

            CompletableFuture<Void> future =
                    fileUploader.handleFileUploadAsync(multipartFiles)
                            .thenAccept(urls -> {
                                System.out.println("aa urls = "+urls);
                                EventMedia media = new EventMedia();
                                media.setEvent(event);
                                media.setMediaType(mediaType);
                                media.setMediaUrls(urls);
                                event.getMediaList().add(media);
                            }).exceptionally(ex -> {
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
