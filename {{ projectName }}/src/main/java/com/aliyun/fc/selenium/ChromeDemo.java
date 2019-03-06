package com.aliyun.fc.selenium;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.FunctionInitializer;
import com.aliyun.fc.runtime.StreamRequestHandler;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class ChromeDemo implements StreamRequestHandler, FunctionInitializer {

    public void initialize(Context context) throws IOException {

        Instant start = Instant.now();

        try (TarArchiveInputStream in =
                     new TarArchiveInputStream(
                             new BrotliCompressorInputStream(
                                     new BufferedInputStream(
                                             new FileInputStream("chromedriver.tar.br"))))) {

            TarArchiveEntry entry;
            while ((entry = in.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File file = new File("/tmp/bin", entry.getName());
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                System.out.println("extract file to " + file.getAbsolutePath());

                try (FileOutputStream out = new FileOutputStream(file)) {
                    IOUtils.copy(in, out);
                }

                Files.setPosixFilePermissions(file.getCanonicalFile().toPath(),
                        getPosixFilePermission(entry.getMode()));
            }
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();

        System.out.println("Extract binary elapsed: " + timeElapsed + "ms");


    }

    public void handleRequest(InputStream inputStream,
                              OutputStream outputStream,
                              Context context) throws IOException {

        System.setProperty("webdriver.chrome.driver", "/tmp/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/tmp/bin/headless-chromium");
        options.addArguments("--disable-extensions"); // disabling extensions
        options.addArguments("--disable-gpu"); // applicable to windows os only
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);

        driver.get("https://ide.fc.aliyun.com");

        outputStream.write(("Page title is: " + driver.getTitle() + "\n").getBytes());

        driver.quit();

    }

    private Set<PosixFilePermission> getPosixFilePermission(int permissions) {

        Set<PosixFilePermission> filePermissions = new HashSet<>();

        // using bitwise operations check the file permissions in decimal
        // numeric system
        // e.g. 100100 AND 100, we will have for result 100
        if ((permissions & 256) > 0) {
            filePermissions.add(PosixFilePermission.OWNER_READ);
        }
        if ((permissions & 128) > 0) {
            filePermissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((permissions & 64) > 0) {
            filePermissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((permissions & 32) > 0) {
            filePermissions.add(PosixFilePermission.GROUP_READ);
        }
        if ((permissions & 16) > 0) {
            filePermissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((permissions & 8) > 0) {
            filePermissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((permissions & 4) > 0) {
            filePermissions.add(PosixFilePermission.OTHERS_READ);
        }
        if ((permissions & 2) > 0) {
            filePermissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((permissions & 1) > 0) {
            filePermissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }

        return filePermissions;
    }


}
