package ru.sbtqa.tag.datajack.providers.properties;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.exceptions.CollectionNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.providers.AbstractDataProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import static com.mongodb.BasicDBObject.parse;
import static java.io.File.separator;

public class PropertiesDataProvider extends AbstractDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesDataProvider.class);
    private static final String DEFAULT_EXTENSION = "properties";
    private final String extension;
    private String testDataFolder;

    /**
     * Create PropertiesDataProvider instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName properties file name
     * @throws DataException if file not found in testDataFolder
     */
    public PropertiesDataProvider(String testDataFolder, String collectionName) throws DataException {
        this.extension = DEFAULT_EXTENSION;
        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObject = parsed;
        this.collectionName = collectionName;
    }

    /**
     * Create PropertiesDataProvider instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName properties file name
     * @param extension custom file extension
     * @throws DataException if file not found in testDataFolder
     */
    public PropertiesDataProvider(String testDataFolder, String collectionName, String extension) throws DataException {
        this.extension = extension;

        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObject = parsed;
        this.collectionName = collectionName;
    }

    private PropertiesDataProvider(String testDataFolder, BasicDBObject obj, String collectionName, String extension) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObject = obj;
        this.collectionName = collectionName;
    }

    private PropertiesDataProvider(String testDataFolder, BasicDBObject obj, String collectionName, String way, String extension) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObject = obj;
        this.way = way;
        this.collectionName = collectionName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertiesDataProvider createInstance(String collectionName) throws DataException {
        return new PropertiesDataProvider(testDataFolder, collectionName, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertiesDataProvider createInstance(BasicDBObject obj, String collectionName, String way) {
        return new PropertiesDataProvider(testDataFolder, obj, collectionName, way, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertiesDataProvider createInstance(BasicDBObject obj, String collectionName) {
        return new PropertiesDataProvider(testDataFolder, obj, collectionName, extension);
    }

    @Override
    public TestDataProvider fromCollection(String collName) throws DataException {
        String json = readFile(this.testDataFolder, collName);
        BasicDBObject parsed = parse(json);
        AbstractDataProvider dataProvider = createInstance(parsed, collName);
        dataProvider.applyGenerator(this.callback);
        return dataProvider;
    }

    private String readFile(String testDataFolder, String collectionName) throws CollectionNotFoundException {
        String json;
        try {
            File targetFile = new File(testDataFolder + separator + collectionName + "." + this.extension);
            this.testDataFolder = targetFile.getPath()
                    .substring(0, targetFile.getPath().lastIndexOf(File.separator) + 1);

            Properties properties = getProperties(targetFile);
            json = new PropertiesToJsonConverter().parseToJson(properties);

        } catch (DataException ex) {
            throw new CollectionNotFoundException(String.format("File %s.%s not found in %s",
                    collectionName, extension, testDataFolder), ex);
        }
        return json;
    }

    private Properties getProperties(File file) throws DataException {
        Properties properties;
        properties = new Properties();
        LOG.debug("Loading properties from {}", file.getPath());
        try (FileInputStream streamFromResources = new FileInputStream(file)) {
            InputStreamReader isr = new InputStreamReader(streamFromResources, "UTF-8");
            properties.load(isr);
        } catch (IOException | NullPointerException e) {
            throw new CollectionNotFoundException("Failed to access file", e);
        }
        return properties;
    }

}
