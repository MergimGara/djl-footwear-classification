package ch.zhaw.deeplearningjava.footwear;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lädt das trainierte Modell beim App-Start von Azure Blob Storage herunter.
 * Gleiche Strategie wie Projekt 1 (HikePlanner) — Modell liegt nicht im Git/Docker,
 * sondern in Azure Blob Storage und wird zur Laufzeit gezogen.
 */
public class BlobModelDownloader {

    private static final String CONTAINER_PREFIX = "footwear-model-";
    private static final String ENV_KEY = "AZURE_STORAGE_CONNECTION_STRING";

    public static void download() throws IOException {
        String connStr = System.getenv(ENV_KEY);
        if (connStr == null || connStr.isEmpty()) {
            // Lokal ohne Azure Blob — lokale models/ Dateien werden verwendet
            System.out.println("AZURE_STORAGE_CONNECTION_STRING nicht gesetzt — verwende lokale models/");
            return;
        }

        System.out.println("*** Lade Modell von Azure Blob Storage ***");
        BlobServiceClient client = new BlobServiceClientBuilder()
                .connectionString(connStr)
                .buildClient();

        // Neuestes Container mit höchstem Suffix finden (z.B. footwear-model-3)
        int latestSuffix = 0;
        for (BlobContainerItem container : client.listBlobContainers()) {
            String name = container.getName();
            if (name.startsWith(CONTAINER_PREFIX)) {
                try {
                    int suffix = Integer.parseInt(name.substring(CONTAINER_PREFIX.length()));
                    latestSuffix = Math.max(latestSuffix, suffix);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (latestSuffix == 0) {
            System.out.println("Kein Modell-Container in Azure Blob Storage gefunden.");
            return;
        }

        String containerName = CONTAINER_PREFIX + latestSuffix;
        System.out.println("Verwende Container: " + containerName);

        Path modelDir = Paths.get("models");
        Files.createDirectories(modelDir);

        BlobContainerClient containerClient = client.getBlobContainerClient(containerName);
        for (BlobItem blob : containerClient.listBlobs()) {
            Path target = modelDir.resolve(blob.getName());
            System.out.println("Downloade: " + blob.getName());
            containerClient.getBlobClient(blob.getName())
                    .downloadToFile(target.toString(), true);
        }

        System.out.println("Modell-Download abgeschlossen.");
    }
}
