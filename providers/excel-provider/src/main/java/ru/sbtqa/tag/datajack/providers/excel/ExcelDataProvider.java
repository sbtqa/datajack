package ru.sbtqa.tag.datajack.providers.excel;

import com.mongodb.BasicDBObject;
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.datajack.exceptions.*;
import ru.sbtqa.tag.datajack.providers.AbstractDataProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class ExcelDataProvider extends AbstractDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelDataProvider.class);
    private static final String DEFAULT_EXTENSION = "xlsx";
    private static final String REF_TPL = "$ref:";
    private final XSSFWorkbook workBook;
    private final String dataFileName;
    private XSSFFormulaEvaluator evaluator;

    /**
     * Constructs ExcelDataProvider object Collection = an Excel work book
     * sheet
     *
     * @param dataFilePath path to an Excel file
     * @param collectionName sheet name
     * @throws DataException if file not found
     */
    public ExcelDataProvider(String dataFilePath, String collectionName) throws DataException {
        File file = FileUtils.getFile(dataFilePath + "." + DEFAULT_EXTENSION);
        if (null == file) {
            throw new FileNotFoundException(format("Could not find data file: '%s'", dataFilePath));
        }
        this.dataFileName = file.getName().replace(DEFAULT_EXTENSION, "");
        try {
            this.workBook = new XSSFWorkbook(file);
        } catch (IOException | InvalidFormatException ex) {
            throw new DataParseException("Could not parse \"" + file + "\"", ex);
        }
        this.collectionName = collectionName;
        this.basicObject = parseCollection();
        this.evaluator = workBook.getCreationHelper().createFormulaEvaluator();
    }

    private ExcelDataProvider(String dataFileName, XSSFWorkbook workBook, String collectionName) {
        this.dataFileName = dataFileName;
        this.workBook = workBook;
        this.collectionName = collectionName;
        this.basicObject = parseCollection();
    }

    private ExcelDataProvider(String dataFileName, XSSFWorkbook workBook,
                              BasicDBObject obj, String collectionName, String way) {
        this.dataFileName = dataFileName;
        this.workBook = workBook;
        this.basicObject = obj;
        this.collectionName = collectionName;
        this.way = way;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExcelDataProvider createInstance(BasicDBObject basicObject, String collectionName, String way) {
        return new ExcelDataProvider(dataFileName, workBook, basicObject, collectionName, way);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExcelDataProvider createInstance(BasicDBObject basicObject, String collectionName) {
        return new ExcelDataProvider(dataFileName, workBook, basicObject, collectionName, way);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExcelDataProvider createInstance(String collectionName) {
        return new ExcelDataProvider(dataFileName, workBook, collectionName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExcelDataProvider fromCollection(String collectionName) {
        ExcelDataProvider dataProvider = createInstance(collectionName);
        dataProvider.applyGenerator(this.callback);
        return dataProvider;
    }

    /**
     * Get all rows in current sheet. Skip header it there is one
     *
     * @return List of rows
     */
    private List<XSSFRow> getRows() {
        List<XSSFRow> effectiveRows = new ArrayList<>();
        int firstRowNumber = 0;
        XSSFSheet sheet = workBook.getSheet(collectionName);
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
        return null != row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                && null == row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                && null == row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    /**
     * Return true if given field is a separator (all cells are empty)
     *
     * @param row row for checking
     * @return boolean result
     */
    private boolean isDelimiter(XSSFRow row) {
        return null == row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                && null == row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                && null == row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    private boolean isSimpleKeyValueMap(XSSFRow row) {
        return !hasComment(row) && !isLink(row);
    }

    private Map<String, String> getSimpleKeyValueMap(XSSFRow row) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(getCellValue(row.getCell(1)).trim(), getCellValue(row.getCell(2)).trim());
        return map;
    }

    private boolean hasComment(XSSFRow row) {
        return null != row.getCell(3) && !row.getCell(3).toString().isEmpty();
    }

    private String getComment(XSSFRow row) {
        return row.getCell(3).toString().trim();
    }

    private String getName(XSSFRow row) {
        return row.getCell(1).toString().trim();
    }

    private boolean isLink(XSSFRow row) {
        return getCellValue(row.getCell(2)).startsWith(REF_TPL);
    }

    private BasicDBObject getLink(XSSFRow row) {
        String linkPath = getCellValue(row.getCell(2)).replace(REF_TPL, "");
        BasicDBObject link = new BasicDBObject();
        String[] fullPathDelimited = linkPath.split("[:]", 2);
        // Link to another sheetName (sheet)
        link.append(COLLECTION_TPL, fullPathDelimited[0]);
        link.append("path", fullPathDelimited[1]);
        return link;
    }

    /**
     * Parse a row that declares an object and return empty BasicDBObject In
     * case if there is a comment present, append it to the object
     *
     * @param declaringRow row that declares an object
     * @return {@link BasicDBObject}
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
     * Get value from Cell Read data/formatted date, result of formula/excel
     * function;
     *
     * @param cell data cell
     * @return value of cell
     */
    private String getCellValue(Cell cell) {
        //noinspection deprecation
        if (cell.getCellType() == CellType.FORMULA) {
            String value = "";
            try {
                value = cell.getRichStringCellValue().getString();
            } catch (Exception e) {
                LOG.debug("Failed to get raw cell value, now trying to get typified", e);
                switch (evaluator.evaluateFormulaCell(cell)) {
                    case BOOLEAN:
                        value = String.valueOf(cell.getBooleanCellValue());
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            value = new DataFormatter().formatCellValue(cell, evaluator);
                        } else {
                            value = String.valueOf(cell.getNumericCellValue());
                        }
                        break;
                    case ERROR:
                        value = String.valueOf(cell.getErrorCellValue());
                        break;
                    case STRING:
                        value = cell.getStringCellValue();
                        break;
                    case BLANK:
                    default:
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
                LOG.debug("Skipped replacing suffix zeroes. Value {} is not a number", value, e);
            }
        }

        return value;
    }

}
