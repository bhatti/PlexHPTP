package com.plexobject.hptp.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MimeUtils {
    private static final Map<String, String> EXTS_TO_MIME = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("323", "text/h323");
            put("acx", "application/internet-property-stream");
            put("ai", "application/postscript");
            put("aif", "audio/x-aiff");
            put("aifc", "audio/x-aiff");
            put("aiff", "audio/x-aiff");
            put("asf", "video/x-ms-asf");
            put("asr", "video/x-ms-asf");
            put("asx", "video/x-ms-asf");
            put("au", "audio/basic");
            put("avi", "video/x-msvideo");
            put("axs", "application/olescript");
            put("bas", "text/plain");
            put("bcpio", "application/x-bcpio");
            put("bin", "application/octet-stream");
            put("bmp", "image/bmp");
            put("c", "text/plain");
            put("cat", "application/vnd.ms-pkiseccat");
            put("cdf", "application/x-cdf");
            put("cer", "application/x-x509-ca-cert");
            put("class", "application/octet-stream");
            put("clp", "application/x-msclip");
            put("cmx", "image/x-cmx");
            put("cod", "image/cis-cod");
            put("cpio", "application/x-cpio");
            put("crd", "application/x-mscardfile");
            put("crl", "application/pkix-crl");
            put("crt", "application/x-x509-ca-cert");
            put("csh", "application/x-csh");
            put("css", "text/css");
            put("dcr", "application/x-director");
            put("der", "application/x-x509-ca-cert");
            put("dir", "application/x-director");
            put("dll", "application/x-msdownload");
            put("dms", "application/octet-stream");
            put("doc", "application/msword");
            put("dot", "application/msword");
            put("dvi", "application/x-dvi");
            put("dxr", "application/x-director");
            put("eps", "application/postscript");
            put("etx", "text/x-setext");
            put("evy", "application/envoy");
            put("exe", "application/octet-stream");
            put("fif", "application/fractals");
            put("flr", "x-world/x-vrml");
            put("gif", "image/gif");
            put("gtar", "application/x-gtar");
            put("gz", "application/x-gzip");
            put("h", "text/plain");
            put("hdf", "application/x-hdf");
            put("hlp", "application/winhlp");
            put("hqx", "application/mac-binhex40");
            put("hta", "application/hta");
            put("htc", "text/x-component");
            put("htm", "text/html");
            put("html", "text/html");
            put("htt", "text/webviewhtml");
            put("ico", "image/x-icon");
            put("ief", "image/ief");
            put("iii", "application/x-iphone");
            put("ins", "application/x-internet-signup");
            put("isp", "application/x-internet-signup");
            put("jfif", "image/pipeg");
            put("jpe", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("jpg", "image/jpeg");
            put("js", "application/x-javascript");
            put("latex", "application/x-latex");
            put("lha", "application/octet-stream");
            put("lsf", "video/x-la-asf");
            put("lsx", "video/x-la-asf");
            put("lzh", "application/octet-stream");
            put("m13", "application/x-msmediaview");
            put("m14", "application/x-msmediaview");
            put("m3u", "audio/x-mpegurl");
            put("man", "application/x-troff-man");
            put("mdb", "application/x-msaccess");
            put("me", "application/x-troff-me");
            put("mht", "message/rfc822");
            put("mhtml", "message/rfc822");
            put("mid", "audio/mid");
            put("mny", "application/x-msmoney");
            put("mov", "video/quicktime");
            put("movie", "video/x-sgi-movie");
            put("mp2", "video/mpeg");
            put("mp3", "audio/mpeg");
            put("mpa", "video/mpeg");
            put("mpe", "video/mpeg");
            put("mpeg", "video/mpeg");
            put("mpg", "video/mpeg");
            put("mpp", "application/vnd.ms-project");
            put("mpv2", "video/mpeg");
            put("ms", "application/x-troff-ms");
            put("mvb", "application/x-msmediaview");
            put("nws", "message/rfc822");
            put("oda", "application/oda");
            put("p10", "application/pkcs10");
            put("p12", "application/x-pkcs12");
            put("p7b", "application/x-pkcs7-certificates");
            put("p7c", "application/x-pkcs7-mime");
            put("p7m", "application/x-pkcs7-mime");
            put("p7r", "application/x-pkcs7-certreqresp");
            put("p7s", "application/x-pkcs7-signature");
            put("pbm", "image/x-portable-bitmap");
            put("pdf", "application/pdf");
            put("pfx", "application/x-pkcs12");
            put("pgm", "image/x-portable-graymap");
            put("pko", "application/ynd.ms-pkipko");
            put("pma", "application/x-perfmon");
            put("pmc", "application/x-perfmon");
            put("pml", "application/x-perfmon");
            put("pmr", "application/x-perfmon");
            put("pmw", "application/x-perfmon");
            put("pnm", "image/x-portable-anymap");
            put("pot,", "application/vnd.ms-powerpoint");
            put("ppm", "image/x-portable-pixmap");
            put("pps", "application/vnd.ms-powerpoint");
            put("ppt", "application/vnd.ms-powerpoint");
            put("prf", "application/pics-rules");
            put("ps", "application/postscript");
            put("pub", "application/x-mspublisher");
            put("qt", "video/quicktime");
            put("ra", "audio/x-pn-realaudio");
            put("ram", "audio/x-pn-realaudio");
            put("ras", "image/x-cmu-raster");
            put("rgb", "image/x-rgb");
            put("rmi", "audio/mid");
            put("roff", "application/x-troff");
            put("rtf", "application/rtf");
            put("rtx", "text/richtext");
            put("scd", "application/x-msschedule");
            put("sct", "text/scriptlet");
            put("setpay", "application/set-payment-initiation");
            put("setreg", "application/set-registration-initiation");
            put("sh", "application/x-sh");
            put("shar", "application/x-shar");
            put("sit", "application/x-stuffit");
            put("snd", "audio/basic");
            put("spc", "application/x-pkcs7-certificates");
            put("spl", "application/futuresplash");
            put("src", "application/x-wais-source");
            put("sst", "application/vnd.ms-pkicertstore");
            put("stl", "application/vnd.ms-pkistl");
            put("stm", "text/html");
            put("svg", "image/svg+xml");
            put("sv4cpio", "application/x-sv4cpio");
            put("sv4crc", "application/x-sv4crc");
            put("swf", "application/x-shockwave-flash");
            put("t", "application/x-troff");
            put("tar", "application/x-tar");
            put("tcl", "application/x-tcl");
            put("tex", "application/x-tex");
            put("texi", "application/x-texinfo");
            put("texinfo", "application/x-texinfo");
            put("tgz", "application/x-compressed");
            put("tif", "image/tiff");
            put("tiff", "image/tiff");
            put("tr", "application/x-troff");
            put("trm", "application/x-msterminal");
            put("tsv", "text/tab-separated-values");
            put("txt", "text/plain");
            put("uls", "text/iuls");
            put("ustar", "application/x-ustar");
            put("vcf", "text/x-vcard");
            put("vrml", "x-world/x-vrml");
            put("wav", "audio/x-wav");
            put("wcm", "application/vnd.ms-works");
            put("wdb", "application/vnd.ms-works");
            put("wks", "application/vnd.ms-works");
            put("wmf", "application/x-msmetafile");
            put("wps", "application/vnd.ms-works");
            put("wri", "application/x-mswrite");
            put("wrl", "x-world/x-vrml");
            put("wrz", "x-world/x-vrml");
            put("xaf", "x-world/x-vrml");
            put("xbm", "image/x-xbitmap");
            put("xla", "application/vnd.ms-excel");
            put("xlc", "application/vnd.ms-excel");
            put("xlm", "application/vnd.ms-excel");
            put("xls", "application/vnd.ms-excel");
            put("xlt", "application/vnd.ms-excel");
            put("xlw", "application/vnd.ms-excel");
            put("xof", "x-world/x-vrml");
            put("xpm", "image/x-xpixmap");
            put("xwd", "image/x-xwindowdump");
            put("z", "application/x-compress");
            put("zip", "application/zip");
        }
    };

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static String getContentType(File file) {
        String mimeType = null;
        String ext = getExtension(file);
        if (ext != null) {
            mimeType = EXTS_TO_MIME.get(ext.toLowerCase());
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
}
