package com.plexobject.hptp.client.jobs;

import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.plexobject.hptp.client.TransferClient;
import com.plexobject.hptp.client.s3.TransferClientS3;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.metrics.Timer;

public class FileS3UploadJob implements Job {
    private static final Logger LOGGER = Logger
            .getLogger(FileS3UploadJob.class);
    private final TransferClient uploadClient;

    public FileS3UploadJob() {
        this(new TransferClientS3());
    }

    public FileS3UploadJob(final TransferClient uploadClient) {
        this.uploadClient = uploadClient;
    }

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".execute");
        Map<?, ?> dataMap = context.getJobDetail().getJobDataMap();
        final FileInfo file = (FileInfo) dataMap.get("file");
        final String group = (String) dataMap.get("group");
        LOGGER.info("Executing upload to S3 for " + group + " -- " + file);

        try {
            uploadClient.upload(group, file, null);
        } catch (Throwable e) {
            LOGGER.error("Failed to upload " + file, e);
        } finally {
            timer.stop();
        }
    }
}
