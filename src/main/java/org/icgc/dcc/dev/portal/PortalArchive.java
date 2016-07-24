package org.icgc.dcc.dev.portal;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PortalArchive {
  
  /**
   * Configuration.
   */
  final URL archiveUrl;
  
  @SneakyThrows
  public void extract(File outputDir) {
    @Cleanup
    val tar = openTar();

    ArchiveEntry tarEntry;
    while ((tarEntry = tar.getNextEntry()) != null) {
      val fileName = normalizeFileName(tarEntry);
      val file = new File(outputDir, fileName);
      if (tarEntry.isDirectory()) {
        continue;
      }

      val dir = file.getParentFile();
      if (!dir.exists()) {
        checkState(dir.mkdirs(), "Could not make dir %s", dir);
      }

      log.info("Extracting {}...", file);
      extractFile(tar, file);
    }
  }

  private TarArchiveInputStream openTar() throws IOException {
    return new TarArchiveInputStream(new GZIPInputStream(archiveUrl.openStream()));
  }

  private static void extractFile(TarArchiveInputStream tar, File file) throws FileNotFoundException, IOException {
    @Cleanup
    val output = new FileOutputStream(file);
    copy(tar, output);
  }

  private static String normalizeFileName(ArchiveEntry tarEntry) {
    // Strip staring directory with version from path
    return tarEntry.getName().replaceFirst("^[^/]+/", "");
  }
  
}