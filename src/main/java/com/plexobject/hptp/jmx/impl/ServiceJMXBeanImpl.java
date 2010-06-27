package com.plexobject.hptp.jmx.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import com.plexobject.hptp.jmx.ServiceJMXBean;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.util.TimeUtils;

public class ServiceJMXBeanImpl extends NotificationBroadcasterSupport
        implements ServiceJMXBean, NotificationListener {
    private static final Logger LOGGER = Logger
            .getLogger(ServiceJMXBeanImpl.class);
    private Map<String, String> properties = new ConcurrentHashMap<String, String>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final String serviceName;
    private AtomicLong totalErrors;
    private AtomicLong totalRequests;

    private AtomicLong sequenceNumber;
    private String state;

    public ServiceJMXBeanImpl(final String serviceName) {
        this.serviceName = serviceName;
        this.totalErrors = new AtomicLong();
        this.totalRequests = new AtomicLong();
        this.sequenceNumber = new AtomicLong();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.plexobject.hptp.jmx.ServiceJMXBean#getAverageElapsedTimeInNanoSecs
     * ()
     */
    public double getAverageElapsedTimeInNanoSecs() {
        return Metric.getMetric(getServiceName())
                .getAverageDurationInNanoSecs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.plexobject.hptp.jmx.ServiceJMXBean#getProperty(java.lang.String
     * )
     */
    public String getProperty(final String name) {
        return properties.get(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.plexobject.hptp.jmx.ServiceJMXBean#setProperty(java.lang.String
     * , java.lang.String)
     */
    public void setProperty(final String name, final String value) {
        final String oldValue = properties.put(name, value);
        final Notification notification = new AttributeChangeNotification(this,
                sequenceNumber.incrementAndGet(), TimeUtils
                        .getCurrentTimeMillis(), name + " changed", name,
                "String", oldValue, value);
        sendNotification(notification);
        handleNotification(notification, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.plexobject.hptp.jmx.ServiceJMXBean#getServiceName()
     */
    public String getServiceName() {
        return serviceName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.plexobject.hptp.jmx.ServiceJMXBean#getTotalDurationInNanoSecs()
     */
    public long getTotalDurationInNanoSecs() {
        return Metric.getMetric(getServiceName()).getTotalDurationInNanoSecs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.plexobject.hptp.jmx.ServiceJMXBean#getTotalErrors()
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }

    public void incrementError() {
        final long oldErrors = totalErrors.getAndIncrement();
        final Notification notification = new AttributeChangeNotification(this,
                sequenceNumber.incrementAndGet(), TimeUtils
                        .getCurrentTimeMillis(), "Errors changed", "Errors",
                "long", oldErrors, oldErrors + 1);
        sendNotification(notification);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.plexobject.hptp.jmx.ServiceJMXBean#getTotalRequests()
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    public void incrementRequests() {
        final long oldRequests = totalRequests.getAndIncrement();
        final Notification notification = new AttributeChangeNotification(this,
                sequenceNumber.incrementAndGet(), TimeUtils
                        .getCurrentTimeMillis(), "Requests changed",
                "Requests", "long", oldRequests, oldRequests + 1);
        sendNotification(notification);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name,
                description);

        return new MBeanNotificationInfo[] { info };
    }

    public String getState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ServiceJMXBeanImpl)) {
            return false;
        }
        ServiceJMXBeanImpl rhs = (ServiceJMXBeanImpl) object;
        return new EqualsBuilder().append(this.serviceName, rhs.serviceName)
                .isEquals();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(786529047, 1924536713).append(
                this.serviceName).toHashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("serviceName", this.serviceName).append("totalErrors",
                        this.totalErrors).append("totalRequests",
                        this.totalRequests).append("totalRequests",
                        this.totalRequests).append("state", this.state).append(
                        "properties", this.properties).toString();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        pcs.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        pcs.removePropertyChangeListener(pcl);

    }

    public void handleNotification(Notification notification, Object handback) {
        LOGGER.info("Received notification: ClassName: "
                + notification.getClass().getName() + ", Source: "
                + notification.getSource() + ", Type: "
                + notification.getType() + ", tMessage: "
                + notification.getMessage());
        if (notification instanceof AttributeChangeNotification) {
            AttributeChangeNotification acn = (AttributeChangeNotification) notification;
            pcs.firePropertyChange(acn.getAttributeName(), acn.getOldValue(),
                    acn.getNewValue());

        }
    }
}
