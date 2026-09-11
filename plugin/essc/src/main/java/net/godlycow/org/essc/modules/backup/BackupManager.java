package net.godlycow.org.essc.modules.backup;

import net.godlycow.org.essc.EssentialsC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final EssentialsC plugin;
    private final File backupFolder;

    public BackupManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) backupFolder.mkdirs();
    }

    public void createAsync(java.util.function.Consumer<String> onSuccess,
                            java.util.function.Consumer<String> onFailure) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                String fileName = "backup-" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".zip";
                File zipFile = new File(backupFolder, fileName);

                zip(plugin.getDataFolder(), zipFile);

                pruneOldBackups();

                plugin.getServer().getGlobalRegionScheduler().run(plugin, task1 -> onSuccess.accept(fileName));
            } catch (IOException e) {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task1 -> onFailure.accept(e.getMessage()));
            }
        });
    }

    public void createSync(java.util.function.Consumer<String> onSuccess,
                           java.util.function.Consumer<String> onFailure) {
        try {
            String fileName = "backup-" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".zip";
            File zipFile = new File(backupFolder, fileName);
            zip(plugin.getDataFolder(), zipFile);
            pruneOldBackups();
            onSuccess.accept(fileName);
        } catch (IOException e) {
            onFailure.accept(e.getMessage());
        }
    }

    public List<File> listBackups() {
        File[] files = backupFolder.listFiles(
                f -> f.isFile() && f.getName().endsWith(".zip")
        );
        if (files == null) return List.of();

        List<File> list = new ArrayList<>(Arrays.asList(files));
        list.sort(Comparator.comparingLong(File::lastModified).reversed());
        return list;
    }

    public boolean delete(String fileName) {
        File file = new File(backupFolder, fileName);
        if (!file.exists() || !file.getParentFile().equals(backupFolder)) return false;
        return file.delete();
    }

    private void zip(File sourceFolder, File dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            zipFolder(sourceFolder, sourceFolder, zos);
        }
    }

    private void zipFolder(File root, File current, ZipOutputStream zos) throws IOException {
        File[] contents = current.listFiles();
        if (contents == null) return;

        for (File file : contents) {
            if (file.equals(backupFolder)) continue;

            String relativePath = root.toURI().relativize(file.toURI()).getPath();

            if (file.isDirectory()) {
                zos.putNextEntry(new ZipEntry(relativePath + "/"));
                zos.closeEntry();
                zipFolder(root, file, zos);
            } else {
                zos.putNextEntry(new ZipEntry(relativePath));
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }

    private void pruneOldBackups() {
        int keepLast = plugin.getConfigManager().getBackupKeepLast();
        if (keepLast <= 0) return;

        List<File> backups = listBackups();
        if (backups.size() <= keepLast) return;

        List<File> toDelete = backups.subList(keepLast, backups.size());
        for (File file : toDelete) {
            if (file.delete()) {
                plugin.debug("[Backup] Pruned old backup: " + file.getName());
            }
        }
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}