package com.plexobject.hptp.domain;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import com.plexobject.hptp.util.Md5Utils;
import com.plexobject.hptp.util.MimeUtils;
import com.plexobject.hptp.util.UnitUtils;

public class FileInfo implements Comparator<FileInfo>, Serializable {
    private static final long serialVersionUID = 1L;
    private String path;
    private String md5;
    private long byteSize;
    private Date lastModified;
    private String contentType;
    private long bytesTransferred;
    private Date transferStarted;
    private Date transferEnded;
    private long offset;
    private TransferStatus status = TransferStatus.WAITING;
    private Future<FileInfo> md5Future;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public FileInfo() {
    }

    public FileInfo(final File file) {
        this.path = file.getAbsolutePath();
        this.byteSize = file.length();
        this.lastModified = new Date(file.lastModified());
        this.contentType = MimeUtils.getContentType(getFile());
        md5Future = Md5Utils.calculateMd5Async(this);
    }

    /**
     * @return the name
     */
    public String getName() {
        return getFile().getName();
    }

    /**
     * @return the file size in bytes
     */
    public long getByteSize() {
        return byteSize;
    }

    public File getFile() {
        return new File(path);
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *            the path to set
     */
    public void setPath(String path) {
        this.pcs.firePropertyChange("path", this.path, this.path = path);
    }

    /**
     * @param size
     *            the size to set
     */
    public void setByteSize(long byteSize) {
        this.pcs.firePropertyChange("byteSize", this.byteSize,
                this.byteSize = byteSize);
    }

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified
     *            the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.pcs.firePropertyChange("lastModified", this.lastModified,
                this.lastModified = lastModified);
    }

    public void setBytesTransferred(long bytesTransferred) {
        this.pcs.firePropertyChange("bytesTransferred", this.bytesTransferred,
                this.bytesTransferred = bytesTransferred);
        if (bytesTransferred + offset < byteSize
                && status == TransferStatus.WAITING) {
            status = TransferStatus.INPROGRESS;
        }
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public int getPercentCompleted() {
        if (bytesTransferred > 0) {
            return (int) (100 * (bytesTransferred + offset) / byteSize);
        } else {
            return 0;
        }
    }

    public void setTransferStarted(Date transferStarted) {
        status = TransferStatus.INPROGRESS;
        this.pcs.firePropertyChange("transferStarted", this.transferStarted,
                this.transferStarted = transferStarted);
    }

    public Date getTransferStarted() {
        return transferStarted;
    }

    public Date getTransferEnded() {
        return transferEnded;
    }

    public void setContentType(String contentType) {
        this.pcs.firePropertyChange("contentType", this.contentType,
                this.contentType = contentType);
    }

    public String getContentType() {
        return contentType;
    }

    public void setOffset(long offset) {
        this.pcs
                .firePropertyChange("offset", this.offset, this.offset = offset);
    }

    public long getOffset() {
        return offset;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("path", path);
        json.put("byteSize", byteSize);
        json.put("lastModified", lastModified.getTime());
        json.put("contentType", contentType);
        json.put("md5", getMd5());
        json.put("offset", offset);

        if (bytesTransferred > 0) {
            json.put("bytesTransferred", bytesTransferred);
        }
        if (transferStarted != null) {
            json.put("transferStarted", transferStarted.getTime());
        }
        if (transferEnded != null) {
            json.put("transferEnded", transferEnded.getTime());
        }
        json.put("status", status.name());
        return json;
    }

    public static FileInfo parse(final JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("null json");
        }
        try {
            FileInfo info = new FileInfo();
            copyFromJson(json, info);
            return info;
        } catch (JSONException e) {
            throw new RuntimeException("Invalid json object [" + json + "]", e);
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeUTF(toJson().toString());
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        String json = ois.readUTF();
        copyFromJson(JSONObject.fromObject(json), this);
    }

    private static void copyFromJson(final JSONObject json, FileInfo info) {
        info.setPath(json.getString("path"));

        info.setByteSize(json.getInt("byteSize"));
        info.setLastModified(new Date(json.getLong("lastModified")));
        info.setContentType(json.getString("contentType"));
        info.setMd5(json.getString("md5"));
        info.setOffset(json.getLong("offset"));

        if (json.has("bytesTransferred")) {
            info.setBytesTransferred(json.getLong("bytesTransferred"));
        }
        if (json.has("transferStarted")) {
            info.setTransferStarted(new Date(json.getLong("transferStarted")));
        }
        if (json.has("transferEnded")) {
            info.setTransferEnded(new Date(json.getLong("transferEnded")));
        }
        info.setStatus(TransferStatus.valueOf(json.getString("status")));
    }

    public TransferStatus getStatus() {
        return status;
    }

    public boolean isCancelled() {
        return status == TransferStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED
                || status == TransferStatus.ALREADY_COMPLETED;
    }

    public boolean isFailed() {
        return status == TransferStatus.FAILED;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        if (isCancelled()) {
            return "Transfer cancelled...";
        } else if (isFailed()) {
            return "Transmission error...";
        } else if (isAlreadyCompleted()) {
            return "Already uploaded...";
        } else if (isCompleted()) {
            return String.format("transferred %s in %s @%s/sec", UnitUtils
                    .formatFileSize(bytesTransferred), UnitUtils
                    .formatMilliTime(getTransferElapsedTime()), UnitUtils
                    .formatFileSize(getBytesPerSec()));
        } else if (inProgress()) {
            return String.format("transferred %s in %s @%s/sec, ERT %s",
                    UnitUtils.formatFileSize(bytesTransferred), UnitUtils
                            .formatMilliTime(getTransferElapsedTime()),
                    UnitUtils.formatFileSize(getBytesPerSec()), UnitUtils
                            .formatMilliTime(getRemaingTimeInMillis()));
        } else {
            return "Waiting to upload...";
        }
    }

    public String getDetailedDescription() {
        return "path " + path + ", status " + status + ", size " + byteSize
                + ", offset " + offset + ", modified " + lastModified
                + ", md5 " + getMd5() + ", transferred "
                + bytesTransferred + ", transfer started " + transferStarted
                + ", transfer ended " + transferEnded + ", transfer-elapsed "
                + getRemaingTimeInMillis() + ", bytes/sec " + getBytesPerSec()
                + ", remainig time " + getRemaingTimeInMillis()
                + ", % complete " + getPercentCompleted();
    }

    private boolean inProgress() {
        return status == TransferStatus.INPROGRESS;
    }

    private boolean isAlreadyCompleted() {
        return status == TransferStatus.ALREADY_COMPLETED;
    }

    public void setFailed() {
        setStatus(TransferStatus.FAILED);
        setTransferEnded(new Date());
    }

    public void setCancelled() {
        setStatus(TransferStatus.CANCELLED);
        setTransferEnded(new Date());
    }

    public void setAlreadyCompleted() {
        setStatus(TransferStatus.ALREADY_COMPLETED);
        setTransferEnded(new Date());
    }

    public void setCompleted() {
        setStatus(TransferStatus.COMPLETED);
        setTransferEnded(new Date());
    }

    public long getTransferElapsedTime() {
        if (transferStarted != null) {
            long started = transferStarted.getTime();
            long ended = transferEnded != null ? transferEnded.getTime()
                    : System.currentTimeMillis();
            return ended - started;
        } else {
            return -1;
        }
    }

    public double getRemaingTimeInMillis() {
        if (transferStarted != null && bytesTransferred > 0) {
            return (byteSize - bytesTransferred - offset) * 1000
                    / getBytesPerSec();
        } else {
            return -1;
        }
    }

    public double getBytesPerSec() {
        if (transferStarted != null && bytesTransferred > 0) {
            return bytesTransferred * 1000.0 / getTransferElapsedTime();
        } else {
            return -1;
        }
    }

    public int compare(FileInfo info1, FileInfo info2) {
        return info1.path.compareTo(info2.path);
    }

    public boolean equals(Object o) {
        if (o instanceof FileInfo == false) {
            return false;
        }
        FileInfo other = (FileInfo) o;
        return path.equals(other.path);
    }

    public String toString() {
        return getFile().getName() + " [" + getMd5() + "/" + byteSize + "/"
                + status + "]";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    void setTransferEnded(Date transferEnded) {
        if (md5 == null) {
            Md5Utils.calculateMd5Async(this);
        }
        this.pcs.firePropertyChange("transferEnded", this.transferEnded,
                this.transferEnded = transferEnded);
    }

    void setStatus(TransferStatus status) {
        this.pcs
                .firePropertyChange("status", this.status, this.status = status);
    }

    public void setMd5(String md5) {
        this.pcs.firePropertyChange("md5", this.md5, this.md5 = md5);
    }

    public String getMd5() {
        if (md5 == null && md5Future != null) {
            try {
                md5Future.get();
                md5Future = null;
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to calculate md5", e);
            }
        }
        return md5;
    }
}
