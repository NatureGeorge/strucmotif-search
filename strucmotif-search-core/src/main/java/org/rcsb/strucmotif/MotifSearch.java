package org.rcsb.strucmotif;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.rcsb.strucmotif.align.Alignment;
import org.rcsb.strucmotif.align.QuaternionAlignment;
import org.rcsb.strucmotif.core.InternalMotifSearch;
import org.rcsb.strucmotif.core.InternalMotifSearchImpl;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.MotifPrunerImpl;
import org.rcsb.strucmotif.core.TargetAssembler;
import org.rcsb.strucmotif.core.TargetAssemblerImpl;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.io.MessagePackCodec;
import org.rcsb.strucmotif.io.MinimizedMessagePackCodec;
import org.rcsb.strucmotif.io.read.AllPurposeReader;
import org.rcsb.strucmotif.io.read.AllPurposeReaderImpl;
import org.rcsb.strucmotif.io.read.FileSystemSelectionReaderImpl;
import org.rcsb.strucmotif.io.read.MongoSelectionReaderImpl;
import org.rcsb.strucmotif.io.read.RenumberedReader;
import org.rcsb.strucmotif.io.read.RenumberedReaderImpl;
import org.rcsb.strucmotif.io.read.SelectionReader;
import org.rcsb.strucmotif.io.write.RenumberedWriterImpl;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.rcsb.strucmotif.persistence.FileSystemInvertedIndexImpl;
import org.rcsb.strucmotif.persistence.FileSystemUpdateStateManagerImpl;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.MongoClientHolder;
import org.rcsb.strucmotif.persistence.MongoClientHolderImpl;
import org.rcsb.strucmotif.persistence.MongoInvertedIndexImpl;
import org.rcsb.strucmotif.persistence.MongoResidueDB;
import org.rcsb.strucmotif.persistence.MongoResidueDBImpl;
import org.rcsb.strucmotif.persistence.MongoTitleDB;
import org.rcsb.strucmotif.persistence.MongoTitleDBImpl;
import org.rcsb.strucmotif.persistence.MongoUpdateStateManagerImpl;
import org.rcsb.strucmotif.persistence.UpdateStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;

/**
 * The entry point to perform motif searches.
 */
public class MotifSearch {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearch.class);

    public static final double DISTANCE_CUTOFF;
    public static final double DISTANCE_CUTOFF_SQUARED;

    public static final Path DATA_ROOT;
    public static final Path ARCHIVE_PATH;
    public static final Path LOOKUP_PATH;
    public static final Path ARCHIVE_LIST;
    public static final Path INDEX_LIST;
    public static final Path RESIDUE_LIST;

    public static final String DB_URI;
    public static final boolean NO_DB;

    public static final int THREADS;
    public static final ForkJoinPool FORK_JOIN_POOL;

    public static final DecimalFormat DECIMAL_FORMAT;

    private static final MotifSearch INSTANCE = new MotifSearch();

    private final Injector injector;
    private final QueryBuilder queryBuilder;

    private MotifSearch() {
        AbstractModule module = new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(Alignment.class).to(QuaternionAlignment.class);
                bind(InternalMotifSearch.class).to(InternalMotifSearchImpl.class);
                bind(MotifPruner.class).to(MotifPrunerImpl.class);
                bind(TargetAssembler.class).to(TargetAssemblerImpl.class);
                bind(AllPurposeReader.class).to(AllPurposeReaderImpl.class);
                bind(RenumberedReader.class).to(RenumberedReaderImpl.class);
                bind(StructureWriter.class).to(RenumberedWriterImpl.class);
                bind(MessagePackCodec.class).to(MinimizedMessagePackCodec.class);

                if (NO_DB) {
                    bind(SelectionReader.class).to(FileSystemSelectionReaderImpl.class);
                    bind(InvertedIndex.class).to(FileSystemInvertedIndexImpl.class);
                    bind(UpdateStateManager.class).to(FileSystemUpdateStateManagerImpl.class);
                } else {
                    bind(SelectionReader.class).to(MongoSelectionReaderImpl.class);
                    bind(InvertedIndex.class).to(MongoInvertedIndexImpl.class);
                    bind(UpdateStateManager.class).to(MongoUpdateStateManagerImpl.class);
                    bind(MongoResidueDB.class).to(MongoResidueDBImpl.class);
                    bind(MongoTitleDB.class).to(MongoTitleDBImpl.class);
                    bind(MongoClientHolder.class).to(MongoClientHolderImpl.class);
                }
            }
        };

        this.injector = Guice.createInjector(module);
        this.queryBuilder = injector.getInstance(QueryBuilder.class);
    }

    public static <T> T getInstance(Class<T> type) {
        return INSTANCE.injector.getInstance(type);
    }

    public static QueryBuilder newQuery() {
        return INSTANCE.queryBuilder;
    }

    public static String format(Object object) {
        return DECIMAL_FORMAT.format(object);
    }

    // use static block to set config before instance is created
    static {
        logger.info("Setting motif search constants");
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties")) {
            Objects.requireNonNull(input, "Did not find property file: 'config.properties' on classpath");
            Properties prop = new Properties();
            prop.load(input);

            String threadValue = prop.getProperty("number.threads");
            THREADS = threadValue != null ? Integer.parseInt(threadValue) : Runtime.getRuntime().availableProcessors();
            logger.info("Will use {} threads", THREADS);
            FORK_JOIN_POOL = new ForkJoinPool(THREADS);

            // the cutoff up to which words are detected
            DISTANCE_CUTOFF = Double.parseDouble(prop.getProperty("distance.cutoff"));
            DISTANCE_CUTOFF_SQUARED = DISTANCE_CUTOFF * DISTANCE_CUTOFF;

            // the root path of all service data
            DATA_ROOT = Paths.get(prop.getProperty("path.root"));
            logger.info("Data root is {}", DATA_ROOT);
            // an optimized archive - minimal information, maximum IO performance for whole files
            ARCHIVE_PATH = DATA_ROOT.resolve("archive").resolve("bcif-renum");
            // the location of the lookup
            LOOKUP_PATH = DATA_ROOT.resolve("lookup");
            // keeps track of all files for which a reduced/optimized coordinate file exists
            ARCHIVE_LIST = DATA_ROOT.resolve("archive.list");
            // all structures currently present in the index (may be empty structures - processed but not containing any valid words)
            INDEX_LIST = DATA_ROOT.resolve("lookup.list");
            // all structures currently indexed in the component-DB
            RESIDUE_LIST = DATA_ROOT.resolve("component.list");

            NO_DB = Boolean.parseBoolean(prop.getProperty("no.db"));
            if (NO_DB) {
                DB_URI = null;
                logger.info("Falling back to file-system based implementation - enable MongoDB for improved performance");
            } else {
                DB_URI = prop.getProperty("db.connection.uri", null);
                logger.info("MongoDB connection is {}",
                        DB_URI == null ? "not specified - using local MongoDB server" :
                                DB_URI.replaceFirst("://.*@", "login"));
            }

            DECIMAL_FORMAT = new DecimalFormat(prop.getProperty("decimal.format"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
