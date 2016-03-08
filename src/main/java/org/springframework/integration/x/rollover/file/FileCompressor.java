package org.springframework.integration.x.rollover.file;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * @author Markus Bukowski
 */
public class FileCompressor {

    private Logger logger = LoggerFactory.getLogger(FileCompressor.class);

    private String archivePrefix = "archive";

    @Value("${rollover.file.suffix.compress:.gz}")
    private String gzipExtention;

    @Value("${rollover.file.suffix.temp:.tmp}")
    private String tempExtention;

    @Async("fileCompressorExecutor")
    public void compressFile(String filePath) {
        try {
            logger.debug("Started compressing file {}", filePath);
            File file = new File(filePath);
            if (file != null) {
                gzipFile(file);
            }
            logger.debug("Finished compressing file {}", filePath);
        } catch (IOException e) {
            logger.error("Compression failed for: {}. Exception: {}", filePath, e);
        }

    }

    private File gzipFile(File sourceFile) throws IOException {

        if (!sourceFile.exists()) {
            logger.error("Source file doesn't exist: {}", sourceFile);
            return null;
        }

        InputStream is = new FileInputStream(sourceFile);
        File tmpCompressedFile = new File(sourceFile.getParentFile(),
                sourceFile.getName() + gzipExtention + tempExtention);

        OutputStream os = new GZIPOutputStream(new FileOutputStream(tmpCompressedFile));
        IOUtils.copy(is, os);
        is.close();
        os.close();

        if (!sourceFile.delete()) {
            throw new IOException("Can't delete file: " + sourceFile.getPath());
        }

        // Remove the .tmp suffix. (e.g. from filename.gz.tmp to filename.gz);
        File compressedFile = new File(tmpCompressedFile.getAbsolutePath().replace(tempExtention, ""));
        if (!tmpCompressedFile.renameTo(compressedFile)) {
            throw new IOException("Failed to remove .tmp from the name of: " + tmpCompressedFile);
        }

        return compressedFile;
    }

}
