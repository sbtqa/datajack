package ru.sbtqa.tag.datajack.adaptors;

import com.mongodb.BasicDBObject;
import java.io.File;
import java.io.IOException;
import static java.lang.String.format;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.BasicBSONObject;
import ru.sbtqa.tag.datajack.TestDataObject;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesExeption;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.DataParseException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.FileNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.GeneratorException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelDataObjectAdaptor extends AbstractDataObjectAdaptor implements TestDataObject {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelDataObjectAdaptor.class);
    private final XSSFWorkbook workBook;
    private final String sheetName;
    private final String dataFileName;
    private XSSFFormulaEvaluator evaluator;

    /**
     * Constructs ExcellDataObjectAdaptor object Collection = an Excell work
     * book sheet
     *
     * @param dataFilePath path to an Excel file
     * @param sheetName sheet name
     * @throws ru.sbtqa.tag.datajack.exceptions.DataException TODO
     */
    public ExcelDataObjectAdaptor(String dataFilePath, String sheetName) throws DataException {
        File file = FileUtils.getFile(dataFilePath + ".xlsx");
        if (null == file) {
            throw new FileNotFoundException(format("Could not find data file: '%s'", dataFilePath));
        }
        this.dataFileName = file.getName().replace(".xlsx", "");
        try {
            this.workBook = new XSSFWorkbook(file);
        } catch (IOException | InvalidFormatException ex) {
            throw new DataParseException("Could not parse \"" + file + "\"", ex);
        }
        this.sheetName = sheetName;
        this.basicObj = parseCollection();
        this.evaluator = workBook.getCreationHelper().createFormulaEvaluator();
    }

    private ExcelDataObjectAdaptor(String dataFileName, XSSFWorkbook workBook, String sheetName) {
        this.dataFileName = dataFileName;
        this.workBook = workBook;
        this.sheetName = sheetName;
        this.basicObj = parseCollection();
    }

    private ExcelDataObjectAdaptor(String dataFileName, XSSFWorkbook workBook,
            BasicDBObject obj, String sheetName, String way) {
        this.dataFileName = dataFileName;
        this.workBook = workBook;
        this.basicObj = obj;
        this.sheetName = sheetName;
        this.way = way;
    }

    @Override
    public TestDataObject get(String key) throws DataException {
        this.way = key;        
        ExcelDataObjectAdaptor tdo;

        if (key.contains(".")) {
            String[] keys = key.split("[.]");
            StringBuilder partialBuilt = new StringBuilder();
            BasicDBObject basicO = this.basicObj;
            for (String partialKey : keys) {
                partialBuilt.append(partialKey);
                if (!(basicO.get(partialKey) instanceof BasicDBObject)) {
                    if (basicO.get(partialKey) instanceof String && !partialBuilt.toString().equals(key)) {
                        throw new FieldNotFoundException(format("Field '%s' in '%s' object on sheet '%s' "
                                + "is not an object. Cannot find any nested fields inside it",
                                partialKey, partialBuilt.toString().replace("." + partialKey, ""), this.sheetName));
                    }
                    if (null == basicO.get(partialKey)) {
                        throw new FieldNotFoundException(format("Sheet '%s' doesn't contain '%s' field on path '%s'",
                                this.sheetName, partialKey, partialBuilt.toString()));
                    }
                    break;
                }
                basicO = (BasicDBObject) basicO.get(partialKey);
                partialBuilt.append(".");
            }

            tdo = new ExcelDataObjectAdaptor(this.dataFileName, this.workBook, basicO, this.sheetName, this.way);
            tdo.applyGenerator(this.callback);
            tdo.setRootObj(this.rootObj, this.sheetName + "." + key);
            return tdo;
        }
        if (!basicObj.containsField(key)) {
            throw new FieldNotFoundException(format("Sheet '%s' doesn't contain '%s' field in path '%s'",
                    this.sheetName, key, this.path));
        }
        Object result = this.basicObj.get(key);
        if (!(result instanceof BasicDBObject)) {
            result = new BasicDBObject(key, result);
        }
        tdo = new ExcelDataObjectAdaptor(this.dataFileName, this.workBook, (BasicDBObject) result, this.sheetName, this.way);
        tdo.applyGenerator(this.callback);
        
        String rootObjValue;
        if (this.path != null) {
            rootObjValue = this.path + "." + key;
        } else {
            rootObjValue = this.sheetName + "." + key;
        }
        tdo.setRootObj(this.rootObj, rootObjValue);
        return tdo;
    }

    @Override
    public ExcelDataObjectAdaptor fromCollection(String collName) throws DataException {
        ExcelDataObjectAdaptor newObj = new ExcelDataObjectAdaptor(this.dataFileName, this.workBook, collName);
        newObj.applyGenerator(this.callback);
        return newObj;
    }

    @Override
    public String getValue() throws DataException {
        try {
            return this.getReference().getValue();
        } catch (ReferenceException e) {
            LOG.debug("Reference not found", e);
            String result = this.basicObj.getString("value");
            if (result == null) {
                if (this.way.contains(".")) {
                    this.way = this.way.split("[.]")[this.way.split("[.]").length - 1];
                }
                result = this.basicObj.getString(this.way);
            }

            if (this.callback != null) {
                CallbackData generatorParams = new CallbackData(String.join(".", this.dataFileName, this.path), result);
                Object callbackResult = null;
                try {
                    callbackResult = callback.newInstance().call(generatorParams);
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new GeneratorException("Could not initialize callback", ex);
                }
                if (callbackResult instanceof Exception) {
                    throw (GeneratorException) callbackResult;
                } else {
                    result = (String) callbackResult;
                }
            }
            return result;
        }
    }

    @Override
    public TestDataObject getReference() throws DataException {
        if (null != this.basicObj.get("value") && !(this.basicObj.get("value") instanceof String)
                && ((BasicDBObject) this.basicObj.get("value")).containsField("sheetName")
                && ((BasicDBObject) this.basicObj.get("value")).containsField("path")) {
            if (this.rootObj == null) {
                this.rootObj = this.basicObj;
            } else {
                String rootJson = this.rootObj.toJson();
                String baseJson = this.basicObj.toJson();
                if (rootJson.equals(baseJson)) {
                    throw new CyclicReferencesExeption("Cyclic references in database:\n" + rootJson);
                }
            }
            String referencedCollection = ((BasicBSONObject) this.basicObj.get("value")).getString("sheetName");
            this.path = ((BasicBSONObject) this.basicObj.get("value")).getString("path");
            ExcelDataObjectAdaptor reference = this.fromCollection(referencedCollection);
            reference.setRootObj(this.rootObj, referencedCollection + "." + this.path);
            return reference.get(this.path);
        } else {
            throw new ReferenceException(String.format("There is no reference in '%s'. Collection '%s'",
                    this.path, this.sheetName));
        }
    }

    @Override
    public void applyGenerator(Class<? extends GeneratorCallback> callback) {
        this.callback = callback;
    }

    @Override
    public String toString() {
        if (this.basicObj == null) {
            return "";
        }
        return this.basicObj.toString();
    }

    private void setRootObj(BasicDBObject obj, String path) {
        this.rootObj = obj;
        this.path = path;
    }

    /**
     * Get all rows in current sheet. Skip header it there is one
     *
     * @return List of rows
     */
    private List<XSSFRow> getRows() {
        List<XSSFRow> effectiveRows = new ArrayList<>();
        int firstRowNumber = 0;
        XSSFSheet sheet = workBook.getSheet(sheetName);
        if ("Описание"
                .equals(sheet.getRow(sheet.getFirstRowNum()).cellIterator().next().getStringCellValue())) {
            // Check if there is a header on the current shit. If so, skip it
            firstRowNumber++;
        }
        for (int i = firstRowNumber; i <= sheet.getLastRowNum(); i++) {
            effectiveRows.add(sheet.getRow(i));
        }
        return effectiveRows;
    }

    private BasicDBObject parseCollection() {
        BasicDBObject resultObj = new BasicDBObject();
        String currentObjName = "";
        List<XSSFRow> rows = getRows();
        for (XSSFRow row : rows) {
            if (!currentObjName.isEmpty()) { // means we are currently parsing an object
                if (isDelimiter(row)) {
                    currentObjName = "";
                } else if (isObjectDeclarator(row)) {
                    currentObjName = row.cellIterator().next().getStringCellValue().trim();
                    resultObj.append(currentObjName, getObjectDeclaration(row));
                } else // parse a row and append it to object
                if (isSimpleKeyValueMap(row)) {
                    Map<String, String> map = getSimpleKeyValueMap(row);
                    ((BasicDBObject) resultObj.get(currentObjName))
                            .append(map.keySet().iterator().next(), map.get(map.keySet().iterator().next()));
                } else {
                    Map<String, BasicDBObject> map = getObjectMappedToName(row);
                    ((BasicDBObject) resultObj.get(currentObjName))
                            .append(map.keySet().iterator().next(), map.get(map.keySet().iterator().next()));
                }
            } else if (isObjectDeclarator(row)) {
                currentObjName = row.cellIterator().next().getStringCellValue().trim();
                resultObj.append(currentObjName, getObjectDeclaration(row));
            } else if (isSimpleKeyValueMap(row)) {
                Map<String, String> map = getSimpleKeyValueMap(row);
                resultObj.append(map.keySet().iterator().next(), map.get(map.keySet().iterator().next()));
            } else {
                Map<String, BasicDBObject> map = getObjectMappedToName(row);
                resultObj.append(map.keySet().iterator().next(), map.get(map.keySet().iterator().next()));
            }
        }
        return resultObj;
    }

    /**
     * Return true if given field declares an object (has only first cell with
     * value set)
     *
     * @param row row for checking
     * @return boolean result
     */
    private boolean isObjectDeclarator(XSSFRow row) {
        return (null != row.getCell(0, Row.RETURN_BLANK_AS_NULL)
                && null == row.getCell(1, Row.RETURN_BLANK_AS_NULL)
                && null == row.getCell(2, Row.RETURN_BLANK_AS_NULL));
    }

    /**
     * Return true if given field is a separator (all cells are empty)
     *
     * @param row row for checking
     * @return boolean result
     */
    private boolean isDelimiter(XSSFRow row) {
        return (null == row.getCell(0, Row.RETURN_BLANK_AS_NULL)
                && null == row.getCell(1, Row.RETURN_BLANK_AS_NULL)
                && null == row.getCell(2, Row.RETURN_BLANK_AS_NULL));
    }

    private boolean isSimpleKeyValueMap(XSSFRow row) {
        return (!hasComment(row) && !isLink(row));
    }

    private Map<String, String> getSimpleKeyValueMap(XSSFRow row) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(getCellValue(row.getCell(1)).trim(), getCellValue(row.getCell(2)).trim());
        return map;
    }

    private boolean hasComment(XSSFRow row) {
        return (null != row.getCell(3) && !row.getCell(3).toString().isEmpty());
    }

    private String getComment(XSSFRow row) {
        return row.getCell(3).toString().trim();
    }

    private String getName(XSSFRow row) {
        return row.getCell(1).toString().trim();
    }

    private boolean isLink(XSSFRow row) {
        return getCellValue(row.getCell(2)).contains("link:");
    }

    private BasicDBObject getLink(XSSFRow row) {
        String linkPath = getCellValue(row.getCell(2)).replace("link:", "");
        BasicDBObject link = new BasicDBObject();
        String[] fullPathDelimited = linkPath.split("[.]", 2);
        // Link to another sheetName (sheet)
        link.append("sheetName", fullPathDelimited[0]);
        link.append("path", fullPathDelimited[1]);
        return link;
    }

    /**
     * Parse a row that declares an object and return empty BasicDBObject In
     * case if there is a comment present, append it to the object
     *
     * @param declaringRow row that declares an object
     * @return TODO
     */
    private BasicDBObject getObjectDeclaration(XSSFRow declaringRow) {
        BasicDBObject obj = new BasicDBObject();
        if (hasComment(declaringRow)) {
            obj.append("comment", getComment(declaringRow));
        }
        return obj;
    }

    /**
     * Parse a row that has comments/generators/links. If there is no
     * description, use key as an object name Object
     *
     * @param row row to parse
     * @return map objName -> object
     */
    private Map<String, BasicDBObject> getObjectMappedToName(XSSFRow row) {
        Map<String, BasicDBObject> objectMappedToName = new LinkedHashMap<>();
        String name = getName(row);
        BasicDBObject pureObject = new BasicDBObject();
        if (hasComment(row)) {
            pureObject.append("comment", getComment(row));
        }
        if (isLink(row)) {
            pureObject.append("value", getLink(row));
        } else {
            Map<String, String> map = getSimpleKeyValueMap(row);
            pureObject.append(map.keySet().iterator().next(), map.get(map.keySet().iterator().next()));
        }
        objectMappedToName.put(name, pureObject);
        return objectMappedToName;
    }

    /**
     * Get value from Cell Read data/formated date, result of formula/excel
     * function;
     *
     * @param cell data cell
     * @return value of cell {java.lang.String}
     */
    private String getCellValue(Cell cell) {
        if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            String value = "";
            try {
                value = cell.getRichStringCellValue().getString();
            } catch (Exception e) {
                LOG.debug("Failed to get raw cell value, now trying to get typified", e);
                switch (evaluator.evaluateFormulaCell(cell)) {
                    case Cell.CELL_TYPE_BOOLEAN:
                        value = String.valueOf(cell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            value = new DataFormatter().formatCellValue(cell, evaluator);
                        } else {
                            value = String.valueOf(cell.getNumericCellValue());
                        }
                        break;
                    case Cell.CELL_TYPE_STRING:
                        value = cell.getStringCellValue();
                        break;
                    case Cell.CELL_TYPE_BLANK:
                        break;
                    case Cell.CELL_TYPE_ERROR:
                        value = String.valueOf(cell.getErrorCellValue());
                        break;
                }
            }
            return value;
        }

        String value = cell.toString();

        //Replace suffix zeros
        if (value.endsWith(".0")) {
            try {
                value = String.format("%d", (long) Double.parseDouble(value));

            } catch (NumberFormatException e) {
                LOG.debug("Skiped replacing suffix zeroes. Value {} is not a number", value, e);
            }
        }

        return value;
    }
}
