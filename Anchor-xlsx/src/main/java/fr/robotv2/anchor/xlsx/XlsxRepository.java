package fr.robotv2.anchor.xlsx;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class XlsxRepository<ID, T extends Identifiable<ID>> implements Repository<ID, T> {

    private final XlsxDatabase database;
    private final EntityMetadata metadata;
    private final Class<T> cls;

    public XlsxRepository(XlsxDatabase database, Class<T> cls) {
        this.database = database;
        this.metadata = MetadataProcessor.getMetadata(cls);
        this.cls = cls;
    }

    @Override
    public void save(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");

        performBatchOperation(workbook -> {
            Sheet sheet = getOrCreateSheet(workbook);
            List<String> columnOrder = ensureHeaderAndGetOrder(sheet);

            Map<ID, Integer> idToRow = indexById(sheet, columnOrder);
            ID id = entity.getId();
            Integer rowNum = idToRow.get(id);

            Row row =
                    rowNum != null ? sheet.getRow(rowNum)
                            : sheet.createRow(sheet.getLastRowNum() + 1);

            writeEntity(row, entity, columnOrder);
        });
    }

    @Override
    public void saveAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "Entities collection cannot be null");
        if (entities.isEmpty()) return;

        performBatchOperation(workbook -> {
            Sheet sheet = getOrCreateSheet(workbook);
            List<String> columnOrder = ensureHeaderAndGetOrder(sheet);
            Map<ID, Integer> idToRow = indexById(sheet, columnOrder);

            for (T entity : entities) {
                Objects.requireNonNull(entity, "Entity cannot be null");
                ID id = entity.getId();
                Integer rowNum = idToRow.get(id);

                Row row =
                        rowNum != null ? sheet.getRow(rowNum)
                                : sheet.createRow(sheet.getLastRowNum() + 1);

                writeEntity(row, entity, columnOrder);
            }
        });
    }

    @Override
    public void delete(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");

        File file = database.getFile();
        if (!file.exists()) return;

        performBatchOperation((workbook) -> {
            Sheet sheet = workbook.getSheet(metadata.getEntityName());
            if (sheet == null) return;
            List<String> columnOrder = ensureHeaderAndGetOrder(sheet);
            Map<ID, Integer> idToRow = indexById(sheet, columnOrder);
            Integer rowNum = idToRow.get(id);
            if (rowNum != null) removeRow(sheet, rowNum);
        });
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "Entities collection cannot be null");
        deleteAllById(entities.stream().map(T::getId).collect(Collectors.toList()));
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        Objects.requireNonNull(ids, "IDs collection cannot be null");
        if (ids.isEmpty()) return;

        File file = database.getFile();
        if (!file.exists()) return;

        performBatchOperation(workbook -> {
            Sheet sheet = workbook.getSheet(metadata.getEntityName());
            if (sheet == null) return;

            List<String> columnOrder = ensureHeaderAndGetOrder(sheet);
            Map<ID, Integer> idToRow = indexById(sheet, columnOrder);

            List<Integer> rowsToDelete = new ArrayList<>();
            for (ID id : ids) {
                Integer rowNum = idToRow.get(id);
                if (rowNum != null) rowsToDelete.add(rowNum);
            }

            rowsToDelete.sort(Collections.reverseOrder());
            for (Integer row : rowsToDelete) removeRow(sheet, row);
        });
    }

    @Override
    public Optional<T> findById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        File file = database.getFile();
        if (!file.exists()) return Optional.empty();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(metadata.getEntityName());
            if (sheet == null) return Optional.empty();
            List<String> columnOrder = ensureHeaderAndGetOrder(sheet);
            Map<ID, Integer> idToRow = indexById(sheet, columnOrder);
            Integer rowNum = idToRow.get(id);
            if (rowNum == null) return Optional.empty();
            return Optional.of(readEntity(sheet.getRow(rowNum), columnOrder));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to find entity by id: " + id, exception);
        }
    }

    @Override
    public List<T> findAll() {
        File file = database.getFile();
        if (!file.exists()) return Collections.emptyList();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(metadata.getEntityName());
            if (sheet == null) return Collections.emptyList();
            List<String> columnOrder = ensureHeaderAndGetOrder(sheet);
            List<T> entities = new ArrayList<>(sheet.getLastRowNum());
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) entities.add(readEntity(row, columnOrder));
            }
            return entities;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all entities", e);
        }
    }

    // ==================== Helper Methods (condensed) ====================

    private void performBatchOperation(WorkbookOperation operation) {
        File file = database.getFile();

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parentDir);
            }

            Workbook workbook;
            if (file.exists() && file.length() > 0) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = new XSSFWorkbook(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
            }

            try {
                operation.execute(workbook);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
            } finally {
                workbook.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform batch operation", e);
        }
    }

    private Sheet getOrCreateSheet(Workbook workbook) {
        String sheetName = metadata.getEntityName();
        Sheet sheet = workbook.getSheet(sheetName);
        return sheet != null ? sheet : workbook.createSheet(sheetName);
    }

    private List<String> ensureHeaderAndGetOrder(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null || header.getLastCellNum() <= 0) {
            header = sheet.createRow(0);
            List<String> cols = new ArrayList<>(metadata.getAllColumnNames());
            for (int i = 0; i < cols.size(); i++) {
                header.createCell(i).setCellValue(cols.get(i));
            }
        }

        List<String> columns = new ArrayList<>();
        header.forEach(cell -> columns.add(cell.getStringCellValue().toLowerCase()));
        return columns;
    }

    private void writeEntity(Row row, T entity, List<String> columnOrder) {
        Map<String, Object> values = metadata.extract(entity);
        for (int i = 0; i < columnOrder.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) cell = row.createCell(i);
            Object value = values.get(columnOrder.get(i));
            writeCell(cell, value);
        }
    }

    private T readEntity(Row row, List<String> columnOrder) {
        try {
            T entity = cls.getDeclaredConstructor().newInstance();
            for (int i = 0; i < columnOrder.size(); i++) {
                String col = columnOrder.get(i);
                FieldMetadata fm = metadata.getAllFields().get(col);
                if (fm == null) continue;
                Field field = fm.getField();
                Object value = readCell(row.getCell(i), field.getType());
                field.set(entity, value);
            }
            return entity;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read entity from row " + row.getRowNum(), exception);
        }
    }

    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof String s) {
            cell.setCellValue(s);
        } else if (value instanceof Integer i) {
            cell.setCellValue(i.doubleValue());
        } else if (value instanceof Long l) {
            cell.setCellValue(l.doubleValue());
        } else if (value instanceof Float f) {
            cell.setCellValue(f.doubleValue());
        } else if (value instanceof Double d) {
            cell.setCellValue(d);
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (value instanceof Date dt) {
            cell.setCellValue(dt);
        } else if (value instanceof LocalDateTime ldt) {
            cell.setCellValue(ldt);
        } else if (value instanceof LocalDate ld) {
            cell.setCellValue(ld);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private Object readCell(Cell cell, Class<?> targetType) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return convertString(cell.getStringCellValue(), targetType);
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    if (targetType == LocalDateTime.class) {
                        return cell.getLocalDateTimeCellValue();
                    } else if (targetType == LocalDate.class) {
                        return cell.getLocalDateTimeCellValue().toLocalDate();
                    } else if (targetType == Date.class) {
                        return cell.getDateCellValue();
                    }
                    return cell.getLocalDateTimeCellValue();
                }
                double n = cell.getNumericCellValue();
                if (targetType == Integer.class || targetType == int.class)
                    return (int) n;
                if (targetType == Long.class || targetType == long.class)
                    return (long) n;
                if (targetType == Float.class || targetType == float.class)
                    return (float) n;
                if (targetType == Double.class || targetType == double.class)
                    return n;
                if (targetType == String.class) return String.valueOf(n);
                return n;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    private Object convertString(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) return null;
        if (targetType == String.class) return value;

        try {
            if (targetType == Integer.class || targetType == int.class)
                return Integer.parseInt(value);
            if (targetType == Long.class || targetType == long.class)
                return Long.parseLong(value);
            if (targetType == Double.class || targetType == double.class)
                return Double.parseDouble(value);
            if (targetType == Float.class || targetType == float.class)
                return Float.parseFloat(value);
            if (targetType == Boolean.class || targetType == boolean.class)
                return Boolean.parseBoolean(value);
            if (targetType == LocalDateTime.class)
                return LocalDateTime.parse(value);
            if (targetType == LocalDate.class) return LocalDate.parse(value);
            if (targetType == Date.class) {
                LocalDateTime ldt = LocalDateTime.parse(value);
                return Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
            }
            if (Enum.class.isAssignableFrom(targetType)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
                return Enum.valueOf(enumType, value);
            }
            if (UUID.class.isAssignableFrom(targetType)) {
                return UUID.fromString(value);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new RuntimeException("Failed to convert '" + value + "' to " + targetType.getSimpleName(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<ID, Integer> indexById(Sheet sheet, List<String> columnOrder) {
        String idCol = metadata.getIdField().getColumnName().toLowerCase();
        int idIdx = columnOrder.indexOf(idCol);
        if (idIdx == -1) return Collections.emptyMap();
        Map<ID, Integer> map = new HashMap<>();
        Class<?> idType = metadata.getIdField().getField().getType();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Object cellValue = readCell(row.getCell(idIdx), idType);
            if (cellValue != null) {
                ID key = (ID) cellValue;
                map.put(key, i);
            }
        }

        return map;
    }

    private void removeRow(Sheet sheet, int rowIndex) {
        int lastRowNum = sheet.getLastRowNum();
        if (rowIndex >= 0 && rowIndex < lastRowNum) {
            sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
        } else if (rowIndex == lastRowNum) {
            Row removingRow = sheet.getRow(rowIndex);
            if (removingRow != null) sheet.removeRow(removingRow);
        }
    }

    @FunctionalInterface
    private interface WorkbookOperation {
        void execute(Workbook workbook) throws Exception;
    }
}
