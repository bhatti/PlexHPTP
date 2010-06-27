package com.plexobject.hptp.client.jobs;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdScheduler;
import org.quartz.impl.StdSchedulerFactory;

import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.metrics.Timer;

public class JobScheduler {
    private static final Logger LOGGER = Logger.getLogger(JobScheduler.class);

    private StdScheduler scheduler;

    public JobScheduler() throws SchedulerException {
        try {
            scheduler = (StdScheduler) new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            createTables();
            scheduler = (StdScheduler) new StdSchedulerFactory().getScheduler();
        }
        scheduler.start();
    }

    @SuppressWarnings("unchecked")
    public void scheduleUploadToS3(String group, FileInfo file)
            throws SchedulerException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".scheduleUploadToS3");

        try {
            final Date time = new Date(System.currentTimeMillis() + 1000);
            final String jobName = "S3Upload for " + file;
            final SimpleTrigger trigger = new SimpleTrigger();
            trigger.setName("triggerToUploadS3" + group + "__" + file.getName()
                    + "__" + time.getTime());
            trigger.setStartTime(time);
            trigger.setRepeatCount(0);

            final JobDetail job = new JobDetail();
            job.setName(jobName);
            job.setJobClass(FileS3UploadJob.class);

            Map<String, Object> dataMap = job.getJobDataMap();
            dataMap.put("group", group);
            dataMap.put("file", file);
            try {
                scheduler.scheduleJob(job, trigger);
            } catch (ObjectAlreadyExistsException e) {
                scheduler.rescheduleJob(job.getName(), job.getGroup(), trigger);
            }
            LOGGER.info("Scheduled S3 upload job for " + file);
        } finally {
            timer.stop();
        }
    }

    private void createTables() {
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream(
                    "quartz.properties");
            if (in == null) {
                LOGGER.warn("!!!Failed to find quartz.properties");
                return;
            }
            Properties props = new Properties();
            props.load(in);
            in = getClass().getClassLoader().getResourceAsStream(
                    "quartz_schema.sql");
            if (in == null) {
                LOGGER.warn("!!!!sFailed to find quartz_schema.sql");
                return;
            }
            String schemaSql = IOUtils.toString(in);
            String driverName = props.getProperty(
                    "org.quartz.dataSource.qzDS.driver").trim();
            Class.forName(driverName);
            String dbUrl = props.getProperty("org.quartz.dataSource.qzDS.URL")
                    .trim();
            String dbgroup = props.getProperty(
                    "org.quartz.dataSource.qzDS.group").trim();
            String dbPassword = props.getProperty(
                    "org.quartz.dataSource.qzDS.password").trim();

            Connection conn = DriverManager.getConnection(dbUrl, dbgroup,
                    dbPassword);

            Statement stmt = conn.createStatement();
            stmt.executeUpdate(schemaSql);
            stmt.close();
            conn.close();
            LOGGER.info("Created tables " + schemaSql);
        } catch (Exception e) {
            LOGGER.error("Failed to create tables", e);
        }
    }

    public static void main(String[] args) {
        try {
            new JobScheduler().scheduleUploadToS3("shahbhat", new FileInfo(
                    new java.io.File("pom.xml")));
            LOGGER.info("Ending");
            Thread.currentThread().join();
        } catch (Exception e) {
            LOGGER.error("Failed to schedule", e);
        }
    }
}
