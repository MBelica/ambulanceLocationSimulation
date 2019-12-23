package edu.kit.ksri.als.dataExchange;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Creates an output Excel file and provides the means to write data in this file. 
 * Each instance of this class is linked to exactly one Excel file. 
 * All instances of this class are referenced in a static ArrayList.
 * 
 * Already existing Excel files with the same name will be replaced.
 *
 */
public class ExportData {
	
	static ArrayList<ExportData> files = new ArrayList<ExportData>(); //static list of all created export files
	
	public String fileName; //name of the Excel file
	XSSFWorkbook workbook = new XSSFWorkbook(); //.xlsx workbook
	public HashMap<String,Integer> rowNums = new HashMap<String,Integer>(); // Saves the current row for each sheet. Thus, data is always written below existing data.

	/**
	 * Constructor for an ExportData instance with its own Excel workbook.
	 * If an instance linked to the same Excel file already exists, it is replaced.
	 * The creation of actual Excel files is executed in the method {@link exportAll()} which is called when the program is quit.
	 * @param fileName	Name of the Excel file. May also include information on the file path.
	 */
	public ExportData(String fileName) {
		this.fileName = fileName;
		files.add(this); //add this file to list of all export files
	}

	
	/**
	 * Writes data in a sheet of the Excel workbook of this ExportData instance.
	 * @param results	Results in format of an ArrayList of String arrays. 
	 * 					Each String array depicts one row in Excel with each field of the 
	 * 					array representing a column entry.
	 * @param sheetName	Name of the Excel sheet in which the results are written in.
	 */
	public void write(ArrayList<String[]> results, String sheetName) {		
		XSSFSheet sheet = workbook.getSheet(sheetName);	// get sheet in workbook
				
		if (sheet == null) { // if sheet does not exist
			sheet = workbook.createSheet(sheetName); // ...create new sheet
			rowNums.put(sheetName, 0); //...and initialize current row
			
			//write the respective title line depending on the type of data (which is specified by the name of the sheet)
			String[] titleLine = new String[0];
			if (sheetName.endsWith("location_samples")) {
				titleLine = new String[] {
					"graph","m","n","alpha","beta","base_seed","sample_id",
					"x","z","optimal_value","computation_time"
				};
			}
			else if (sheetName.endsWith("_assignment")) {
				titleLine = new String[] {
					"graph","solution","type","m","n","alpha","base_seed",
					"optimal_value","service_level","computation_time"
				};
			}
			else if (sheetName.endsWith("_assignment_detailed")) {
				titleLine = new String[] {
					"graph","solution","type","m","n","alpha","base_seed",
					"sample_id","i","j","w","y[i][j][w]"
				};
			}
			else if (sheetName.endsWith("_assignment_per_node")) {
				titleLine = new String[] {
					"graph","solution","type","m","n","alpha","base_seed",
					"node_name","total_assigned_demand"
				};
			}			
			else if (sheetName.endsWith("_assignment_samples")) {
				titleLine = new String[] {
					"graph","solution","type","m","n","alpha","base_seed",
					"sample_id","optimal_value","service_level","computation_time"
				};
			}
			else if (sheetName.endsWith("_graph_bounds")) {
				titleLine = new String[] {
					"graph","alpha","min_bases","max_ambulances"
				};
			}
			else if (sheetName.endsWith("_scenarios")) {
				titleLine = new String[] {
					"graph","m","n","base_seed","sample_id","scenario_id",
					"demand","demand_sum","probability"				
				};
			}
			else if (sheetName.endsWith("_location")){
				titleLine = new String[] {
					"graph","m","n","alpha","beta","base_seed","x","z",
					"#bases","#ambulances",
					"optimal_value","sample_average","computation_time"
				};
			}
			results.add(0, titleLine); // add a title line before the first row of actual data
		}
		
		// write data
		int rowNum = rowNums.get(sheetName);			//get current row of the sheet (continue writing where last writing left off)
		for (String[] result : results) {				//for all rows of the result data
			XSSFRow row = sheet.createRow(rowNum++);	//...create row
			int columnNum = 0;							//...set first column as start column
			for (String str : result) {					//for all fields (columns) of the row
				XSSFCell cell = row.createCell(columnNum++); //...create cell at right row and column
				cell.setCellValue(str);					//...and write data in it
			}
		}
		rowNums.replace(sheetName, rowNum); //update the current row for the sheet
	}
	
	
	/**
	 * Static method that creates Excel files from the workbooks of all ExportData instances.
	 * Existing files are replaced. Is called when the program is quit.
	 */
	public static void exportAll() {
		try {
            for (ExportData e : ExportData.files) {	//for all ExportData instances
            	
            	if (e.workbook.getNumberOfSheets()==0) continue; //skip this file if it has no content (empty files will not be written)
            	
            	//autosize (adjust column width to contents) for the first 6 columns 
            	for (int s = 0; s<e.workbook.getNumberOfSheets(); s++) {	//for all sheets of the workbook
            		XSSFSheet sheet = e.workbook.getSheetAt(s);
            		if (e.rowNums.get(sheet.getSheetName())==0) {			//if there is no content in the sheet
            			e.workbook.removeSheetAt(s);						//...remove sheet
            			continue;											//...and skip it for autosizing
            		}
            		for (int c=0; c<sheet.getRow(0).getLastCellNum(); c++) { //for all columns of the sheet
            			sheet.autoSizeColumn(c);							//autosize the column
            		}
            	}
            	
            	FileOutputStream out = new FileOutputStream(new File(e.fileName)); //create file (replace existing)
                e.workbook.write(out);	//write workbook in file
                out.close();			//close FileOutputStream
                e.workbook.close();		//close workbook
                System.out.println(e.fileName + " written successfully.");
            }  
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
}
