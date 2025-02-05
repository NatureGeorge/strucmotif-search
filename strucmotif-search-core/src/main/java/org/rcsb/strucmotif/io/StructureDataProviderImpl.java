package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.InMemoryStrategy;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.math.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of a structure data provider.
 */
@Service
public class StructureDataProviderImpl implements StructureDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureDataProviderImpl.class);
    private final StructureReader structureReader;
    private final StructureWriter renumberedStructureWriter;
    private final MotifSearchConfig motifSearchConfig;
    private final String dataSource;
    private final Path renumberedPath;
    private final String extension;
    private boolean paths;
    private boolean caching;
    // keys must be upper-case
    private Map<String, Structure> structureCache;

    /**
     * Construct a structure provider.
     * @param structureReader the reader
     * @param structureWriter the writer
     * @param motifSearchConfig the config
     */
    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader,
                                     StructureWriter structureWriter,
                                     MotifSearchConfig motifSearchConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = structureWriter;
        this.motifSearchConfig = motifSearchConfig;
        this.dataSource = motifSearchConfig.getDataSource();
        this.renumberedPath = Paths.get(motifSearchConfig.getRootPath()).resolve(MotifSearchConfig.RENUMBERED_DIRECTORY);
        this.extension = motifSearchConfig.isRenumberedGzip() ? ".bcif.gz" : ".bcif";

        logger.info("BinaryCIF data source is {} - CIF fetch URL: {} - precision: {} - gzipping: {}",
                motifSearchConfig.getDataSource(),
                motifSearchConfig.getCifFetchUrl(),
                motifSearchConfig.getRenumberedCoordinatePrecision(),
                motifSearchConfig.isRenumberedGzip());

        this.paths = false;
        this.caching = false;
    }

    private void ensureRenumberedPathExists() {
        try {
            Files.createDirectories(renumberedPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String prepareUri(String raw, String structureIdentifier) {
        String pdbId = structureIdentifier.toLowerCase();
        String PDBID = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String MIDDLE = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", MIDDLE)
                .replace("{id}", pdbId)
                .replace("{ID}", PDBID);
    }

    private URL getCifFetchUrl(String structureIdentifier) {
        try {
            return new URL(prepareUri(motifSearchConfig.getCifFetchUrl(), structureIdentifier));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getOriginalStructurePath(String structureIdentifier) {
        return Paths.get(prepareUri(dataSource, structureIdentifier));
    }

    private Path getRenumberedStructurePath(String structureIdentifier) {
        return renumberedPath.resolve(structureIdentifier + extension);
    }

    private InputStream getRenumberedInputStream(String structureIdentifier) {
        try {
            return Files.newInputStream(getRenumberedStructurePath(structureIdentifier));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void initializeRenumberedStructureCache() throws IOException {
        InMemoryStrategy strategy = motifSearchConfig.getInMemoryStrategy();
        if (strategy == InMemoryStrategy.OFF) {
            logger.info("Structure data will be read from file-system");
            return;
        }

        if (strategy == InMemoryStrategy.HEAP) {
            logger.info("Structure data will be kept in memory - start loading...");

            this.caching = true;
            List<Path> paths = Files.walk(renumberedPath)
                    .parallel()
                    .filter(path -> !Files.isDirectory(path))
                    .collect(Collectors.toList());
            long start = System.nanoTime();
            this.structureCache = new HashMap<>();

            Partition<Path> partitions = new Partition<>(paths, motifSearchConfig.getUpdateChunkSize());
            logger.info("Formed {} partitions of {} structures",
                    partitions.size(),
                    motifSearchConfig.getUpdateChunkSize()); // TODO maybe this should be a different param

            for (int i = 0; i < partitions.size(); i++) {
                String partitionContext = (i + 1) + " / " + partitions.size();

                List<Path> partition = partitions.get(i);
                logger.info("[{}] Start loading partition", partitionContext);

                // this will run on strucmotif-instances only: let's ignore thread-parameter
                Map<String, Structure> buffer = partition.parallelStream()
                        .map(this::loadRenumberedStructure)
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

                this.structureCache.putAll(buffer);
            }

            long time = (System.nanoTime() - start) / 1000 / 1000 / 1000;
            long atoms = structureCache.values()
                    .stream()
                    .mapToLong(Structure::getAtomCount)
                    .sum();

            logger.info("Done caching structure data in {} seconds - {} atoms in {} structures held in memory", time, atoms, structureCache.size());
        }
    }

    private Pair<String, Structure> loadRenumberedStructure(Path path) {
        try {
            String pdbId = path.toFile().getName().split("\\.")[0];
            Structure structure = readFromInputStream(Files.newInputStream(path));
            return new Pair<>(pdbId, structure);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream getOriginalInputStream(String structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            if (Files.exists(originalPath)) {
                return Files.newInputStream(originalPath);
            } else {
                return getCifFetchUrl(structureIdentifier).openStream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        return structureReader.readFromInputStream(inputStream);
    }

    @Override
    public Structure readRenumbered(String structureIdentifier) {
        if (caching) {
            return structureCache.get(structureIdentifier);
        }
        return readFromInputStream(getRenumberedInputStream(structureIdentifier));
    }

    @Override
    public Structure readOriginal(String structureIdentifier) {
        return readFromInputStream(getOriginalInputStream(structureIdentifier));
    }

    @Override
    public Structure readSome(String structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            return readFromInputStream(Files.newInputStream(originalPath));
        } catch (IOException e1) {
            try {
                Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
                return readFromInputStream(Files.newInputStream(renumberedPath));
            } catch (IOException e2) {
                try {
                    return readFromInputStream(getCifFetchUrl(structureIdentifier).openStream());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Override
    public void writeRenumbered(String structureIdentifier, MmCifFile mmCifFile) {
        if (!paths) {
            ensureRenumberedPathExists();
            this.paths = true;
        }
        renumberedStructureWriter.write(mmCifFile, getRenumberedStructurePath(structureIdentifier));
    }

    @Override
    public void deleteRenumbered(String structureIdentifier) {
        try {
            Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
            if (Files.exists(renumberedPath)) {
                Files.delete(getRenumberedStructurePath(structureIdentifier));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
