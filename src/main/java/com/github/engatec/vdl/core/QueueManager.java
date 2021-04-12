package com.github.engatec.vdl.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.engatec.vdl.core.preferences.ConfigRegistry;
import com.github.engatec.vdl.model.DownloadStatus;
import com.github.engatec.vdl.model.QueueItem;
import com.github.engatec.vdl.model.preferences.wrapper.misc.QueueAutostartDownloadPref;
import com.github.engatec.vdl.worker.service.QueueItemDownloadService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueManager {

    private static final Logger LOGGER = LogManager.getLogger(QueueManager.class);

    public static final QueueManager INSTANCE = new QueueManager();

    private static final String FILENAME = "queue.vdl";

    private final ObservableList<QueueItem> queueItems = FXCollections.observableList(new LinkedList<>());
    private final Map<QueueItem, Service<?>> itemServiceMap = new HashMap<>();

    private QueueManager() {
        queueItems.addListener((ListChangeListener<QueueItem>) change -> {
            while (change.next()) {
                for (QueueItem removedItem : change.getRemoved()) {
                    Service<?> service = itemServiceMap.remove(removedItem);
                    if (service != null) {
                        service.cancel();
                    }
                }
            }
        });
    }

    public void addItem(QueueItem item) {
        Service<?> service = itemServiceMap.get(item);
        if (service != null && service.isRunning()) { // Ooops, item's being downloaded already
            return;
        }

        // Add most recent item rather than allow to have multiple same items in the queue
        queueItems.remove(item);
        queueItems.add(item);

        Boolean autoStartDownload = ConfigRegistry.get(QueueAutostartDownloadPref.class).getValue();
        if (autoStartDownload) {
            startDownload(item);
        }
    }

    public void removeItem(QueueItem item) {
        queueItems.remove(item);
    }

    public void removeFinished() {
        queueItems.removeIf(item -> item.getStatus() == DownloadStatus.FINISHED);
    }

    public void removeAll() {
        queueItems.clear();
    }

    public ObservableList<QueueItem> getQueueItems() {
        return queueItems;
    }

    public boolean hasItemsInProgress() {
        return queueItems.stream().anyMatch(it -> it.getStatus() == DownloadStatus.IN_PROGRESS);
    }

    public void persistQueue() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(ApplicationContext.CONFIG_PATH.resolve(FILENAME).toFile(), queueItems);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private void fixState(List<QueueItem> items) {
        for (QueueItem item : ListUtils.emptyIfNull(items)) {
            DownloadStatus status = item.getStatus();
            if (status == DownloadStatus.SCHEDULED) {
                item.setStatus(DownloadStatus.READY); // Should be safe to turn SCHEDULED into READY as it hadn't started when the app shut down
            }
            if (item.getProgress() < 0) {
                item.setProgress(0);
            }
        }
    }

    public void restoreQueue() {
        Path queueFilePath = ApplicationContext.CONFIG_PATH.resolve(FILENAME);
        if (!Files.exists(queueFilePath)) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<QueueItem> items = mapper.readValue(queueFilePath.toFile(), new TypeReference<>(){});
            fixState(items);
            queueItems.addAll(items);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    public void startDownload(QueueItem item) {
        itemServiceMap.computeIfAbsent(item, QueueItemDownloadService::new).start();
    }

    public void cancelDownload(QueueItem item) {
        Service<?> service = itemServiceMap.get(item);
        if (service == null) {
            LOGGER.warn("No service associated with the queue item");
        } else {
            service.cancel();
        }
    }

    public void resumeDownload(QueueItem item) {
        itemServiceMap.computeIfAbsent(item, QueueItemDownloadService::new).restart();
    }
}
