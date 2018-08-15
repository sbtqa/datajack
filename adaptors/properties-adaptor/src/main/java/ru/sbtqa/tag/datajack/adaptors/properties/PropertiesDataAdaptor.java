package ru.sbtqa.tag.datajack.adaptors.properties;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.adaptors.json.JsonDataAdaptor;
import ru.sbtqa.tag.datajack.exceptions.CollectionNotfoundException;
import ru.sbtqa.tag.datajack.exceptions.DataException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import static java.io.File.separator;

public class PropertiesDataAdaptor extends JsonDataAdaptor implements TestDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesDataAdaptor.class);

    /**
     * Create PropertiesDataAdaptor instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName properties file name
     * @throws DataException if file not found in testDataFolder
     */
    public PropertiesDataAdaptor(String testDataFolder, String collectionName) throws DataException {
        super(testDataFolder, collectionName, "properties");
    }

    /**
     * Create PropertiesDataAdaptor instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName properties file name
     * @param extension      custom file extension
     * @throws DataException if file not found in testDataFolder
     */
    public PropertiesDataAdaptor(String testDataFolder, String collectionName, String extension) throws DataException {
        super(testDataFolder, collectionName, extension);
    }

    private PropertiesDataAdaptor(String testDataFolder, BasicDBObject obj, String collectionName, String extension) {
        super(testDataFolder, obj, collectionName, extension);
    }

    private PropertiesDataAdaptor(String testDataFolder, BasicDBObject obj, String collectionName, String way, String extension) {
        super(testDataFolder, obj, collectionName, way, extension);
    }

    @Override
    protected <T extends JsonDataAdaptor> T privateInit(String testDataFolder, BasicDBObject obj, String collectionName, String way) {
        return (T) new PropertiesDataAdaptor(testDataFolder, obj, collectionName, way, extension);
    }

    @Override
    protected <T extends JsonDataAdaptor> T privateInit(String testDataFolder, BasicDBObject obj, String collectionName) {
        return (T) new PropertiesDataAdaptor(testDataFolder, obj, collectionName, extension);
    }

    @Override
    protected String readFile(String testDataFolder, String collectionName) throws CollectionNotfoundException {
        String json;
        try {
            Properties properties = getProperties(testDataFolder + separator + collectionName + "." + extension);
            json = new PropertiesToJsonConverter().parseToJson(properties);

        } catch (DataException ex) {
            throw new CollectionNotfoundException(String.format("File %s.%s not found in %s",
                    collectionName, extension, testDataFolder), ex);
        }
        return json;
    }

    private Properties getProperties(String path) throws DataException {
        Properties properties;
        properties = new Properties();
        LOG.debug("Loading properties from {}", path);
        File file = new File(path);
        try (FileInputStream streamFromResources = new FileInputStream(file)) {
            InputStreamReader isr = new InputStreamReader(streamFromResources, "UTF-8");
            properties.load(isr);
        } catch (IOException | NullPointerException e) {
            throw new CollectionNotfoundException("Failed to access file", e);
        }
        return properties;
    }

}
