package edu.kit.ksri.als.dataExchange;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Provides interface to a dedicated Excel file.
 * 
 * All instances of this class are referenced in a static ArrayList.
 *
 */
public class ImportData {

	static ArrayList<ImportData> files = new ArrayList<ImportData>(); //static list of all opened import files
	
	public File file;// import file
	FileInputStream fileInputStream; // FileInputStream for the import file

	public XSSFWorkbook workbook; // workbook of the import file
	public XSSFSheet sheet;
	public Iterator<Row> rowIterator;
	private Row currentRow;

	/**
	 * Creates interface to an import Excel file.
	 * @param fileName	Name of the import file.
	 */
	public ImportData(String fileName) {
		try {
			file = new File(fileName);
			fileInputStream = new FileInputStream(file); //create FileInputStream for import file
			workbook = new XSSFWorkbook(fileInputStream); //open Excel workbook of import file
			sheet = workbook.getSheetAt(0);//get first (and usually only) sheet of the Excel file

			rowIterator = sheet.iterator(); //create sheet iterator
			rowIterator.next(); //skip first row (which is the title row)

			files.add(this); //add import file to list of all opened files
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getNextRow() {
		currentRow = rowIterator.next();
	}

	public String getCellInCurrentRowAsString(int cellNumber) {
		return currentRow.getCell(cellNumber).getStringCellValue();
	}

	public double getCellInCurrentRowAsDouble(int cellNumber) {
		return currentRow.getCell(cellNumber).getNumericCellValue();
	}

	public boolean isCellInCurrentRowNotNull (int cellNumber) {
		return (currentRow.getCell(cellNumber) != null);
	}
	
	/**
	 * Static method that closes all opened import files. Is called when the program is quit.
	 */
	public static void closeAll() {
		for (ImportData importData : files) {
			try {
				importData.fileInputStream.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			};
		}
	}
}
