package ru.sbtqa.tag.datajack.providers.properties;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jalokim.propertiestojson.resolvers.primitives.BooleanJsonTypeResolver;
import pl.jalokim.propertiestojson.resolvers.primitives.ObjectFromTextJsonTypeResolver;
import pl.jalokim.propertiestojson.resolvers.primitives.PrimitiveArrayJsonTypeResolver;
import pl.jalokim.propertiestojson.resolvers.primitives.StringJsonTypeResolver;
import pl.jalokim.propertiestojson.resolvers.primitives.adapter.PrimitiveJsonTypeResolverToNewApiAdapter;
import pl.jalokim.propertiestojson.resolvers.primitives.object.BooleanToJsonTypeConverter;
import pl.jalokim.propertiestojson.resolvers.primitives.object.ElementsToJsonTypeConverter;
import pl.jalokim.propertiestojson.resolvers.primitives.object.ObjectToJsonTypeConverter;
import pl.jalokim.propertiestojson.resolvers.primitives.object.StringToJsonTypeConverter;
import pl.jalokim.propertiestojson.resolvers.primitives.object.SuperObjectToJsonTypeConverter;
import pl.jalokim.propertiestojson.resolvers.primitives.string.TextToElementsResolver;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverterBuilder;
import pl.jalokim.propertiestojson.util.exception.ReadInputException;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.exceptions.CollectionNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesException;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;
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
    private static final String REF_TPL = "$ref";
    private final String extension;
    private String arrayDelimiter = ",";
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
     * @param extension      custom file extension
     * @param arrayDelimiter custom value array delimiter
     * @throws DataException if file not found in testDataFolder
     */
    public PropertiesDataProvider(String testDataFolder, String collectionName, String extension, String arrayDelimiter) throws DataException {
        this.extension = extension;
        this.arrayDelimiter = arrayDelimiter;
        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObject = parsed;
        this.collectionName = collectionName;
    }

    private PropertiesDataProvider(String testDataFolder, BasicDBObject obj, String collectionName, String extension, String arrayDelimiter) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObject = obj;
        this.collectionName = collectionName;
        this.arrayDelimiter=arrayDelimiter;
    }

    private PropertiesDataProvider(String testDataFolder, BasicDBObject obj, String collectionName, String way, String extension, String arrayDelimiter) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObject = obj;
        this.way = way;
        this.collectionName = collectionName;
        this.arrayDelimiter=arrayDelimiter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertiesDataProvider createInstance(String collectionName) throws DataException {
        return new PropertiesDataProvider(testDataFolder, collectionName, extension, arrayDelimiter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertiesDataProvider createInstance(BasicDBObject obj, String collectionName, String way) {
        return new PropertiesDataProvider(testDataFolder, obj, collectionName, way, extension, arrayDelimiter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertiesDataProvider createInstance(BasicDBObject obj, String collectionName) {
        return new PropertiesDataProvider(testDataFolder, obj, collectionName, extension, arrayDelimiter);
    }

    /**
     * {@inheritDoc}*
     */
    @Override
    public boolean isReference(BasicDBObject basicDBObject) {
        Object value = basicDBObject.get(REF_TPL);
        return value instanceof String;
    }

    /**
     * {@inheritDoc}*
     */
    @Override
    public TestDataProvider getReference() throws DataException {
        if (isReference(this.basicObject)) {
            if (this.rootObject == null) {
                this.rootObject = this.basicObject;
            } else {
                String rootJson = this.rootObject.toJson();
                String baseJson = this.basicObject.toJson();
                if (rootJson.equals(baseJson)) {
                    throw new CyclicReferencesException("Cyclic references in database:\n" + rootJson);
                }
            }
            String refValue = this.basicObject.getString(REF_TPL);
            String referencedCollection = refValue.contains(":") ? refValue.split(":")[0] : this.collectionName;
            String collectionPrefix = refValue.startsWith("/") ? "" : this.collectionName.substring(0, this.collectionName.lastIndexOf("/") + 1);
            this.path = refValue.contains(":") ? refValue.split(":")[1] : refValue;
            AbstractDataProvider reference = (AbstractDataProvider) this.fromCollection(collectionPrefix + referencedCollection);
            reference.setRootObject(this.rootObject, collectionPrefix + referencedCollection + "." + this.path);
            return reference.get(this.path);
        } else {
            throw new ReferenceException(String.format("There is no reference in \"%s\". Collection \"%s\"",
                    this.path, this.collectionName));
        }
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
            json = new PropertiesToJsonConverterBuilder()
                    .onlyCustomTextToObjectResolvers(new TextToElementsResolver(true, arrayDelimiter))
                    .build()
                    .convertPropertiesFromFileToJson(targetFile);

        } catch (ReadInputException ex) {
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
