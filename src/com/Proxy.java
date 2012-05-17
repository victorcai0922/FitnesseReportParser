package com;

/**
 * @author caiqiuliang
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public class Proxy {
	private StringBuffer sb = null;
	private String testCaseNumber = null;
	private String faileCaseNumber = null;
	private String testResult = null;
	private int passCount = 0;
	private int exceptionCount = 0;
	private int failCount = 0;
	private Calendar todaysDate = null;
	private Connection con;
	private Statement statement;
	private String testResultDir;
	private String recentlyResultDir;
	private String dirname;
	private String fitnessDir;
	private String testname;
	private String testformat;
	private String testmode;
	private String resultForSort;
	private String graphicresultdir;
	private String pietitle;
	private String projectname;
	private String databaseurl;
	private String username;
	private String password;
	private String projectid;

	public void paserConfigFile() {
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
		dirname = dateformat.format(new Date()).toString();
		todaysDate = new GregorianCalendar();
		Properties pp = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream("resource/config.properties");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			pp.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		testResultDir = pp.getProperty("TestResult");
		recentlyResultDir = pp.getProperty("RecentlyTestResultDir");
		fitnessDir = pp.getProperty("FitnesseDir");
		testname = pp.getProperty("TestsuitenameOrTestname");
		testmode = pp.getProperty("TestOrSuite");
		testformat = pp.getProperty("ExecuteFormat");
		graphicresultdir = pp.getProperty("graphicresultdir");
		try {
			pietitle = new String(pp.getProperty("pietitle").getBytes(
					"ISO8859-1"), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			projectname = new String(pp.getProperty("projectname").getBytes(
					"ISO8859-1"), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		databaseurl = pp.getProperty("databaseurl");
		username = pp.getProperty("username");
		password = pp.getProperty("password");
		projectid = pp.getProperty("projectid");
	}

	/**
	 * @param caseName
	 * @param mode
	 * @param format
	 */
	public void startFitnesse() {
		String cmd = String.format("java -jar fitnesse.jar -c %s?%s&format=%s",
				testname, testmode, testformat);
		Runtime rt = Runtime.getRuntime();
		File dir = new File(fitnessDir);
		try {
			Process ps = rt.exec(cmd, null, dir);
			sb = new StringBuffer();
			InputStream outAb = ps.getInputStream();
			readOutput(outAb, sb);
			System.out.println(new String(sb));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 
	 * @param input
	 * @param buffer
	 */
	private void readOutput(InputStream input, StringBuffer buffer) {
		try {
			int c;
			while ((c = input.read()) != -1)
				buffer.append((char) c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 */
	private void parseTestResult() {
		int start = sb.indexOf("-----Command Output-----");
		int end = sb.indexOf("-----Command Complete-----");
		int length = "-----Command Complete-----".length();
		testResult = sb.substring(start, end + length);

	}

	/**
	 * 
	 */
	private void getTestCaseNumber() {
		int start = testResult.indexOf("--------");
		int startlength = "--------".length();
		int end = testResult.indexOf("Tests");
		int endlength = "Tests".length();
		testCaseNumber = testResult.substring(start + startlength, end
				+ endlength);
	}

	/**
	 * 
	 */
	private void getFailedCaseNumber() {
		int start = testResult.indexOf("Tests,");
		int startlength = "Tests,".length();
		int end = testResult.indexOf("Failures");
		int endlength = "Failures".length();
		faileCaseNumber = testResult.substring(start + startlength, end
				+ endlength);
		faileCaseNumber = faileCaseNumber.trim();

	}

	/**
	 * 
	 */
	public void putTestResultToFile() {
		paserConfigFile();
		parseTestResult();
		getTestCaseNumber();
		getFailedCaseNumber();
		parseTestReportForSort();
		buildGraphicTestReport(pietitle, projectname, graphicresultdir);
		try {
			File filedir = new File(testResultDir);
			if (!filedir.exists()) {
				if (!filedir.mkdir())
					throw new Exception("");
			}
			String filename = testResultDir + "Test" + dirname + ".txt";
			filename = filename.replaceAll(" ", "");
			File file = new File(filename);
			if (!file.exists()) {
				if (!file.createNewFile())
					throw new Exception("");
			}
			FileWriter fw = new FileWriter(filename);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(resultForSort);
			pw.println(testCaseNumber);
			pw.println(faileCaseNumber);
			pw.close();
			fw.close();
			File testResult[] = filedir.listFiles();
			FileWrapper[] fileWrappers = new FileWrapper[testResult.length];
			for (int i = 0; i < testResult.length; i++) {
				fileWrappers[i] = new FileWrapper(testResult[i]);
			}
			Arrays.sort(fileWrappers);
			try {
				File recentlyReport = new File(recentlyResultDir);
				if (!recentlyReport.exists()) {
					if (!recentlyReport.mkdir()) {
						throw new Exception("");
					}
				}
				String oldPath = fileWrappers[testResult.length - 1].getFile()
						.getPath();
				System.out.println(oldPath);
				String newPath = recentlyResultDir
						+ fileWrappers[testResult.length - 1].getFile()
								.getName();
				copyFile(oldPath, newPath);
			} catch (Exception e) {

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @author victor.cai
	 */
	class FileWrapper implements Comparable {
		/** File */
		private File file;
		public FileWrapper(File file) {
			this.file = file;
		}
		public int compareTo(Object obj) {
			assert obj instanceof FileWrapper;
			FileWrapper castObj = (FileWrapper) obj;
			if ((this.file.lastModified() - castObj.getFile().lastModified()) > 0) {
				return 1;
			} else if ((this.file.lastModified() - castObj.getFile()
					.lastModified()) < 0) {
				return -1;
			} else {
				return 0;
			}
		}

		public File getFile() {
			return this.file;
		}
	}

	/**
	 *
	 */
	public void getTestReport() {
		try {
			File filedir = new File(recentlyResultDir);
			if (!filedir.exists()) {
				if (!filedir.mkdir()) {
					throw new Exception("");
				}
			}

		} catch (Exception e) {

		}
	}

	/**
	 * 
	 * 
	 * @param oldPath
	 * @param newPath
	 */
	public void copyFile(String oldPath, String newPath) {
		try {
			int length = 4097152;
			FileInputStream in = new FileInputStream(oldPath);
			FileOutputStream out = new FileOutputStream(newPath);
			FileChannel inC = in.getChannel();
			FileChannel outC = out.getChannel();
			ByteBuffer b = null;
			while (true) {
				if (inC.position() == inC.size()) {
					inC.close();
					outC.close();
					return;
				}
				if ((inC.size() - inC.position()) < length) {
					length = (int) (inC.size() - inC.position());
				} else
					length = 2097152;
				b = ByteBuffer.allocateDirect(length);
				inC.read(b);
				b.flip();
				outC.write(b);
				outC.force(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * 
	 * @param path
	 */

	public void deleteFile(String path) {
		File file = new File(path);
		String tmpList[] = file.list();
		File tmp = null;
		for (int i = 0; i < tmpList.length; i++) {
			if (path.endsWith(File.separator)) {
				tmp = new File(path + tmpList[i]);
			} else {
				tmp = new File(path + File.separator + tmpList[i]);
			}
			tmp.delete();
		}
	}

	/**
	 *
	 */
	public void parseTestReportForSort() {
		int start = testResult
				.indexOf("Starting Test System: slim using fitnesse.slim.SlimService.");
		int startlength = "Starting Test System: slim using fitnesse.slim.SlimService."
				.length();
		int end = testResult.indexOf("--------");
		resultForSort = testResult.substring(start + startlength + 1, end - 1);
		System.out.println(resultForSort);
		InputStream inputStream = new ByteArrayInputStream(
				resultForSort.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(
				inputStream));
		try {
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				String firstchar = line.substring(0, 1);
				if (firstchar.equals(".")) {
					passCount++;
				} else if (firstchar.equals("F")) {
					failCount++;
				} else if (firstchar.equals("X")) {
					exceptionCount++;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 
	 */
	public void buildGraphicTestReport(String pietitle, String testname,
			String graphicresultdir) {
		PieDataset dataset = getDataset();
		JFreeChart chart = ChartFactory.createPieChart3D(pietitle, dataset,
				true, true, false);
		PiePlot3D plot = (PiePlot3D) chart.getPlot();
		plot.setSectionPaint("Cases Passed", Color.green);
		plot.setSectionPaint("Cases Failed", Color.red);
		plot.setSectionPaint("Cases Exception", Color.yellow);
		plot.setSectionOutlinesVisible(true);
		plot.setForegroundAlpha(1.0f);
		plot.setBaseSectionOutlinePaint(Color.black);
		plot.setBaseSectionOutlineStroke(new BasicStroke(1));
		plot.setIgnoreNullValues(true);
		plot.setIgnoreZeroValues(true);
		plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
				"{0}={1}({2})", NumberFormat.getNumberInstance(),
				new DecimalFormat("0.00%")));
		plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator(
				"{0}={1}({2})"));
		plot.setBackgroundPaint(Color.white);
		plot.setForegroundAlpha(1.0f);
		plot.setCircular(true);
		Font font = new Font("黑体", Font.CENTER_BASELINE, 20);
		TextTitle title = new TextTitle(testname);
		title.setFont(font);
		chart.setTitle(title);
		chart.getLegend().setItemFont(font);
		plot.setLabelFont(font);
		try {
			File filedir = new File(graphicresultdir);
			if (!filedir.exists()) {
				if (!filedir.mkdir())
					throw new Exception("");
			}
			FileOutputStream fos_jpg = null;
			fos_jpg = new FileOutputStream(graphicresultdir + "\\" + testname
					+ dirname + ".jpg");
			ChartUtilities.writeChartAsJPEG(fos_jpg, 0.99f, chart, 840, 680,
					null);
			fos_jpg.close();

			File testResult[] = filedir.listFiles();
			FileWrapper[] fileWrappers = new FileWrapper[testResult.length];
			for (int i = 0; i < testResult.length; i++) {
				fileWrappers[i] = new FileWrapper(testResult[i]);
			}
			Arrays.sort(fileWrappers);
			File recentlyReport = new File(recentlyResultDir);
			if (!recentlyReport.exists()) {
				if (!recentlyReport.mkdir()) {
					throw new Exception("");
				}
			}
			String oldPath = fileWrappers[testResult.length - 1].getFile()
					.getPath();
			System.out.println(oldPath);
			String newPath = recentlyResultDir
					+ fileWrappers[testResult.length - 1].getFile().getName();
			System.out.println(newPath);
			deleteFile(recentlyResultDir);
			copyFile(oldPath, newPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	private PieDataset getDataset() {
		DefaultPieDataset dataset = new DefaultPieDataset();
		dataset.setValue("Cases Passed", new Double(passCount));
		dataset.setValue("Cases Failed", new Double(failCount));
		dataset.setValue("Cases Exception", new Double(exceptionCount));
		return dataset;
	}

	public void insertTestDataIntoDb() {
		Date date = todaysDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String builddate = sdf.format(date);
		int casesum = passCount + failCount + exceptionCount;
		if (statement != null) {
			String sql = String
					.format("insert into buildresult(projectid,passes,failures,exceptions,casesum,builddate) values('%s',%d,%d,%d,%d,'%s')",
							projectid, passCount, failCount, exceptionCount,
							casesum, builddate);
			try {
				int count = statement.executeUpdate(sql);
				System.out.println(count);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void connectToDatabase() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			con = DriverManager.getConnection(databaseurl, username, password);
			statement = con.createStatement();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	public void queryTestDataFromDb() {

	}

	public static void main(String[] args) {
		Proxy pr = new Proxy();
		pr.paserConfigFile();
//		pr.startFitnesse();
//		pr.putTestResultToFile();
		pr.connectToDatabase();
		pr.insertTestDataIntoDb();
	}

}
