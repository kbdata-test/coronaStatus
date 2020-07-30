package com.spring.sample;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.ModelAndView;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

/**
 * Handles requests for the application home page.
 */
@Controller
@ContextConfiguration(locations= {"file:src/min/webapp/WEB-INF/spring/**/root-context.xml"})
public class HomeController {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	@Inject 
    private DataSource ds;
	
	@Inject
    private SqlSessionFactory sqlFactory;
	
	static XSSFRow row;
	
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    @SuppressWarnings("unused")
	private static final String CREDENTIALS_FILE_PATH = "\\credentials.json";
    
    @RequestMapping(value = "/dbTest.do", method = RequestMethod.GET)
    public void testConnection() throws Exception {
        try(Connection con = ds.getConnection()) {
            System.out.println("Connection : " + con + "\n");
            System.out.println("Mybatis Connection -------" + sqlFactory);
            try(SqlSession sqlSession = sqlFactory.openSession() ) {
                System.out.println("세션 연결");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@ResponseBody
	@RequestMapping(value = "/main.do", method = RequestMethod.GET, produces="text/plain;charset=UTF-8")
	public String home(HttpServletRequest request) {
		String clientId = "GJAbTuVCLtjXI2I3hIpQ"; //애플리케이션 클라이언트 아이디값"
        String clientSecret = "USWYbmtpVS"; //애플리케이션 클라이언트 시크릿값"

        String text = null;
        try {
        	String localSearch = request.getParameter("localSearch");
            text = URLEncoder.encode(localSearch, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new RuntimeException("검색어 인코딩 실패",e);
        }
        
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://192.168.62.55");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        String apiURL = "https://openapi.naver.com/v1/search/local?query=" + text + "&display=100&start=1&sort=random"; // json 결과
        //String apiURL = "https://openapi.naver.com/v1/search/local.xml?query="+ text + "&display=10&start=1&sort=random"; // xml 결과

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);
        String responseBody = get(apiURL,requestHeaders);
		
        System.out.println(responseBody);
		return responseBody;
	}
	
	private static String get(String apiUrl, Map<String, String> requestHeaders) {
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 에러 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }
	
	private static HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch(MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch(IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }
	
	private static String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);

        try(BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch(IOException e) {
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }
	
	@ResponseBody
	@RequestMapping(value = "/totalCorona.do", method = RequestMethod.GET, produces="text/plain;charset=UTF-8")
	public String totalCorona(HttpServletRequest request) {
		String apiKey = "98138b3b3cdd13084777fd0bd6adc6432";
		
        String apiURL = "http://api.corona-19.kr/korea/?serviceKey=" + apiKey ; // xml 결과

        Map<String, String> requestHeaders = new HashMap<>();
        String responseBody = get(apiURL,requestHeaders);
		
		return responseBody;
	}
	
	@SuppressWarnings("unchecked")
	@ResponseBody
	@RequestMapping(value = "/regionCorona.do", method = RequestMethod.GET, produces="text/plain;charset=UTF-8")
	public String regionCorona(HttpServletRequest request) throws ParseException {
		String apiKey = "98138b3b3cdd13084777fd0bd6adc6432";
		
        String apiURL = "http://api.corona-19.kr/korea/country/new/?serviceKey=" + apiKey ; // xml 결과

        Map<String, String> requestHeaders = new HashMap<>();
        String responseBody = get(apiURL,requestHeaders);
        
        String apiURL1 = "http://api.corona-19.kr/korea/?serviceKey=" + apiKey ; // xml 결과
        Map<String, String> requestHeaders1 = new HashMap<>();
        String responseBody1 = get(apiURL1,requestHeaders1);
        JSONParser jsonParse1 = new JSONParser(); 
        JSONObject jsonObj1 = (JSONObject) jsonParse1.parse(responseBody1);
        String updateTime = (String) jsonObj1.get("updateTime");
        
        JSONParser jsonParse = new JSONParser(); 
        
        //JSONParse에 json데이터를 넣어 파싱한 다음 JSONObject로 변환한다. 
        JSONObject jsonObj = (JSONObject) jsonParse.parse(responseBody);
        JSONObject seoul = (JSONObject) jsonObj.get("seoul");
        JSONObject busan = (JSONObject) jsonObj.get("busan");
        JSONObject daegu = (JSONObject) jsonObj.get("daegu");
        JSONObject incheon = (JSONObject) jsonObj.get("incheon");
        JSONObject gwangju = (JSONObject) jsonObj.get("gwangju");
        JSONObject daejeon = (JSONObject) jsonObj.get("daejeon");
        JSONObject ulsan = (JSONObject) jsonObj.get("ulsan");
        JSONObject sejong = (JSONObject) jsonObj.get("sejong");
        JSONObject gyeonggi = (JSONObject) jsonObj.get("gyeonggi");
        JSONObject gangwon = (JSONObject) jsonObj.get("gangwon");
        JSONObject chungbuk = (JSONObject) jsonObj.get("chungbuk");
        JSONObject chungnam = (JSONObject) jsonObj.get("chungnam");
        JSONObject jeonbuk = (JSONObject) jsonObj.get("jeonbuk");
        JSONObject jeonnam = (JSONObject) jsonObj.get("jeonnam");
        JSONObject gyeongbuk = (JSONObject) jsonObj.get("gyeongbuk");
        JSONObject gyeongnam = (JSONObject) jsonObj.get("gyeongnam");
        JSONObject jeju = (JSONObject) jsonObj.get("jeju");
        JSONObject quarantine = (JSONObject) jsonObj.get("quarantine");
        
        jsonObj.remove("resultMessage");
        jsonObj.remove("resultCode");
        jsonObj.remove("korea");
        
        seoul.put("lat", "37.566535");
        seoul.put("lng", "126.9779692");
        busan.put("lat", "35.1795543");
        busan.put("lng", "129.0756416");
        daegu.put("lat", "35.8714354");
        daegu.put("lng", "128.601445");
        incheon.put("lat", "37.4562557");
        incheon.put("lng", "126.7052062");
        gwangju.put("lat", "35.1595454");
        gwangju.put("lng", "126.8526012");
        daejeon.put("lat", "36.3504119");
        daejeon.put("lng", "127.3845475");
        ulsan.put("lat", "35.5383773");
        ulsan.put("lng", "129.3113596");
        sejong.put("lat", "36.4800984");
        sejong.put("lng", "127.2890354");
        gyeonggi.put("lat", "37.41379999999999");
        gyeonggi.put("lng", "127.5183");
        gangwon.put("lat", "37.8228");
        gangwon.put("lng", "128.1555");
        chungbuk.put("lat", "36.8");
        chungbuk.put("lng", "127.7");
        chungnam.put("lat", "36.5184");
        chungnam.put("lng", "126.8");
        jeonbuk.put("lat", "35.71750000000001");
        jeonbuk.put("lng", "127.153");
        jeonnam.put("lat", "34.8679");
        jeonnam.put("lng", "126.991");
        gyeongbuk.put("lat", "36.4919");
        gyeongbuk.put("lng", "128.8889");
        gyeongnam.put("lat", "35.4606");
        gyeongnam.put("lng", "128.2132");
        jeju.put("lat", "33.4890113");
        jeju.put("lng", "126.4983023");
        responseBody = jsonObj.toJSONString();
        @SuppressWarnings("resource")
		XSSFWorkbook xlsWb = new XSSFWorkbook();
        Sheet sheet1 = xlsWb.createSheet("코로나 현황");
        
        CellStyle cellStyle = xlsWb.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER); //가운데 정렬
        
        Row row = null;
//        Cell cell = null;
//
//        sheet1.addMergedRegion(new CellRangeAddress(0,0,0,7)); //셀 병합
//        sheet1.addMergedRegion(new CellRangeAddress(1,1,0,1)); //셀 병합
//        
//        //정렬
//        row = sheet1.createRow(0);
//        cell = row.createCell(0);
//        cell.setCellValue("코로나 지역별 현황");
//        row.createCell(1);
//        row.createCell(2);
//        row.createCell(3);
//        row.createCell(4);
//        row.createCell(5);
//        row.createCell(6);
//        row.createCell(7);
//        cell.setCellStyle(cellStyle);
//        
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date time = new Date();
//        String today = format.format(time);
//        
//        row = sheet1.createRow(1);
//        cell = row.createCell(0);
//        cell.setCellValue(today);
//        row.createCell(1);
        
        row = sheet1.createRow(0);
        row.createCell(0).setCellValue("완치자수");
        row.createCell(1).setCellValue("신규확진환자수");
        row.createCell(2).setCellValue("사망자");
        row.createCell(3).setCellValue("전일대비증감-해외유입");
        row.createCell(4).setCellValue("전일대비증감-지역발생");
        row.createCell(5).setCellValue("발생률");
        row.createCell(6).setCellValue("시도명(지역명)");
        row.createCell(7).setCellValue("확진환자수");
        row.createCell(8).setCellValue("날짜");
        sheet1.setColumnWidth(0, 3000);
        sheet1.setColumnWidth(1, 4000);
        sheet1.setColumnWidth(2, 2500);
        sheet1.setColumnWidth(3, 5500);
        sheet1.setColumnWidth(4, 5500);
        sheet1.setColumnWidth(5, 2500);
        sheet1.setColumnWidth(6, 4000);
        sheet1.setColumnWidth(7, 3000);
        
        regionExcelCellvalue(seoul, row, 1, sheet1, updateTime);
        regionExcelCellvalue(busan, row, 2, sheet1, updateTime);
        regionExcelCellvalue(daegu, row, 3, sheet1, updateTime);
        regionExcelCellvalue(incheon, row, 4, sheet1, updateTime);
        regionExcelCellvalue(gwangju, row, 5, sheet1, updateTime);
        regionExcelCellvalue(daejeon, row, 6, sheet1, updateTime);
        regionExcelCellvalue(ulsan, row, 7, sheet1, updateTime);
        regionExcelCellvalue(sejong, row, 8, sheet1, updateTime);
        regionExcelCellvalue(gyeonggi, row, 9, sheet1, updateTime);
        regionExcelCellvalue(gangwon, row, 10, sheet1, updateTime);
        regionExcelCellvalue(chungbuk, row, 11, sheet1, updateTime);
        regionExcelCellvalue(chungnam, row, 12, sheet1, updateTime);
        regionExcelCellvalue(jeonbuk, row, 13, sheet1, updateTime);
        regionExcelCellvalue(jeonnam, row, 14, sheet1, updateTime);
        regionExcelCellvalue(gyeongbuk, row, 15, sheet1, updateTime);
        regionExcelCellvalue(gyeongnam, row, 16, sheet1, updateTime);
        regionExcelCellvalue(jeju, row, 17, sheet1, updateTime);
        regionExcelCellvalue(quarantine, row, 18, sheet1, updateTime);
        
        //로컬폴더에 파일 생성
        HttpSession session = request.getSession(); 
        String root_path = session.getServletContext().getRealPath("");
    	String filePath = root_path + "\\excel.xlsx";
        fileCreate(filePath, xlsWb);
        
		return responseBody;
	}
	
	public void regionExcelCellvalue(JSONObject region, Row row, int index, Sheet sheet1, String updateTime) {
		row = sheet1.createRow(index);
		row.createCell(0).setCellValue((String) region.get("recovered"));
        row.createCell(1).setCellValue((String) region.get("newCase"));
        row.createCell(2).setCellValue((String) region.get("death"));
        row.createCell(3).setCellValue((String) region.get("newFcase"));
        row.createCell(4).setCellValue((String) region.get("newCcase"));
        row.createCell(5).setCellValue((String) region.get("percentage") + "%");
        row.createCell(6).setCellValue((String) region.get("countryName"));
        row.createCell(7).setCellValue((String) region.get("totalCase"));
        row.createCell(8).setCellValue(updateTime);
	}
	
	public boolean fileDel(String filePath) {
		File file = new File(filePath); 
		if(file.exists()) { 
			if(file.delete()) { 
				System.out.println("파일삭제 성공"); 
			} else { 
				System.out.println("파일삭제 실패");
			} 
		} else { 
			System.out.println("파일이 존재하지 않습니다."); 
		}
		return true;
	}
	
	public boolean fileCreate(String filePath, XSSFWorkbook xlsWb) {
		// 출력 파일 위치및 파일명 설정
        FileOutputStream fos;
        File file=null;
        try {
        	//if(fileDel(filePath)) {
            	file = new File(filePath);
            	
    			fos = new FileOutputStream(file);
    			xlsWb.write(fos);
                 
    			fos.close();
                System.out.println("파일생성 완료");
				
                return true;
			//} else { 
                //System.out.println("파일생성 실패"); return false; 
        	//}
        } catch(Exception e) {
            e.printStackTrace();
        }
		return true;
	}
	
	// 파일 다운로드 하는 메소드
    @RequestMapping(value = "/fileDownload.do", method = RequestMethod.GET)
    public ModelAndView fileDownload(HttpServletRequest request) throws Exception {
    	HttpSession session = request.getSession(); 
    	String root_path = session.getServletContext().getRealPath("");
        // 전체 경로를 인자로 넣어 파일 객체를 생성
        File downloadFile = new File(root_path + "\\excel.xlsx");
        
        //Path target = Paths.get("C:/temp/excel.xlsx"); // 파일 저장 경로 
		//URL url = new URL("http://192.168.62.55:8080/sample/excel.xlsx"); 
		//InputStream in = url.openStream(); 
		//Files.copy(in, target); //
		//in.close();
 
        // 생성된 객체 파일과 view들을 인자로 넣어 새 ModelAndView 객체를 생성하며 파일을 다운로드
        // (자동 rendering 해줌)
        return new ModelAndView("fileDownloadView", "downloadFile", downloadFile);
    }
    
    @RequestMapping(value="/download.do", produces="text/plain;charset=UTF-8")
    public void fileDownload(HttpServletRequest request, HttpServletResponse response) {
    	HttpSession session = request.getSession(); 
    	String root_path = session.getServletContext().getRealPath("");
    	String fileName = "excel.xlsx";

        File file = null;
        InputStream is = null;
        OutputStream os = null;
     
        String mimetype = "application/x-msdownload";
        response.setContentType(mimetype);
     
        try {
            setDisposition(fileName, request, response);
      
            file = new File(root_path + "\\excel.xlsx");
            is = new FileInputStream(file);
            os = response.getOutputStream();
      
            byte b[] = new byte[(int) file.length()];
            int leng = 0;
      
            while((leng = is.read(b)) > 0){
                os.write(b,0,leng);
            }
      
            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void setDisposition(String filename, HttpServletRequest request, HttpServletResponse response) throws Exception {
	    String browser = getBrowser(request);
	    String dispositionPrefix = "attachment; filename=";
	    String encodedFilename = null;
	 
	    if(browser.equals("MSIE")) {
	        encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
	    } else if(browser.equals("Firefox")) {
	        encodedFilename = "\"" + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
	    } else if(browser.equals("Opera")) {
	        encodedFilename = "\"" + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
	    } else if(browser.equals("Chrome")) {
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < filename.length(); i++) {
	        	char c = filename.charAt(i);
	        	if (c > '~') {
	        		sb.append(URLEncoder.encode("" + c, "UTF-8"));
	        	} else {
	        		sb.append(c);
	        	}
	        }
	        encodedFilename = sb.toString();
	    } else {
	        throw new IOException("Not supported browser");
	    }
	    response.setHeader("Content-Disposition", dispositionPrefix + encodedFilename);
	 
	    if("Opera".equals(browser)) {
	        response.setContentType("application/octet-stream;charset=UTF-8");
	    }
	}
    
    private String getBrowser(HttpServletRequest request) {
        String header = request.getHeader("User-Agent");
        if(header.indexOf("MSIE") > -1) {
             return "MSIE";
        } else if(header.indexOf("Chrome") > -1) {
             return "Chrome";
        } else if(header.indexOf("Opera") > -1) {
             return "Opera";
        } else if(header.indexOf("Firefox") > -1) {
             return "Firefox";
        } else if(header.indexOf("Mozilla") > -1) {
             if(header.indexOf("Firefox") > -1) {
                  return "Firefox";
             } else {
                  return "MSIE";
             }
        }
        return "MSIE";
   }
    
    // 뷰의 요청 경로 지정
    @RequestMapping(value = "/home.do", method = RequestMethod.GET, produces="text/plain;charset=UTF-8") 
    public String home(Model model) throws IOException { 
    	Document doc = Jsoup.connect("https://www.worldometers.info/coronavirus/").get();        
        Elements tables = doc.select("#main_table_countries_today");
        Elements th = tables.select("thead").select("tr").select("th");
        for(int i = 0; i < th.size(); i++) {
        	th.get(1).text("국가");
        	th.get(2).text("전체 확진자");
        	th.get(3).text("추가 확진자");
        	th.get(4).text("전체 사망자");
        	th.get(5).text("추가 사망자");
        	th.get(6).text("전체 완치자");
        	th.get(7).text("추가 완치자");
        	th.get(8).text("치료중");
        	th.get(9).attr("style", "display:none;");
        	th.get(10).text("100만명당 발생자");
        	th.get(11).text("100만명당 사망자");
        	th.get(12).attr("style", "display:none;");
        	th.get(13).attr("style", "display:none;");
        	th.get(14).text("인구");
        	th.get(15).attr("style", "display:none;");
        	th.get(16).attr("style", "display:none;");
        	th.get(17).attr("style", "display:none;");
        	th.get(18).attr("style", "display:none;");
        	System.out.println(th.get(i).text());
        	model.addAttribute("data",tables.toString());
        }
    	return "home"; // 뷰 파일 리턴 
    }
    
    /**
	 * Simply selects the home view to render by returning its name.
     * @throws IOException 
	 */
	@ResponseBody
	@RequestMapping(value = "/worldCorona.do", method = RequestMethod.GET, produces="text/plain;charset=UTF-8")
	public String worldCorona(HttpServletRequest request) throws IOException {
        Document doc = Jsoup.connect("https://www.worldometers.info/coronavirus/").get();        
        Elements tables = doc.select("#main_table_countries_today");
        Elements th = tables.select("thead").select("tr").select("th");
        for(int i = 0; i < th.size(); i++) {
        	th.get(1).text("국가");
        	th.get(2).text("전체 확진자");
        	th.get(3).text("추가 확진자");
        	th.get(4).text("전체 사망자");
        	th.get(5).text("추가 사망자");
        	th.get(6).text("전체 완치자");
        	th.get(7).text("추가 완치자");
        	th.get(8).text("치료중");
        	th.get(9).attr("style", "display:none;");
        	th.get(10).text("100만명당 발생자");
        	th.get(11).text("100만명당 사망자");
        	th.get(12).attr("style", "display:none;");
        	th.get(13).attr("style", "display:none;");
        	th.get(14).text("인구");
        	th.get(15).attr("style", "display:none;");
        	th.get(16).attr("style", "display:none;");
        	th.get(17).attr("style", "display:none;");
        	th.get(18).attr("style", "display:none;");
        	System.out.println(th.get(i).text());
        }
        
        return tables.toString();
	}
	
	/**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    @SuppressWarnings("unused")
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, HttpServletRequest request) throws IOException {
    	HttpSession session = request.getSession(); 
        String root_path = session.getServletContext().getRealPath("");
    	String filePath = root_path + "credentials.json";
        // Load client secrets.
        InputStream in = new FileInputStream(filePath);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + filePath);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
	
	@RequestMapping(value = "/googleDrive.do", method = RequestMethod.GET)
	public void googleDrive(HttpServletRequest request) throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT, request))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(20)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (com.google.api.services.drive.model.File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }
//        HttpSession session = request.getSession(); 
//        String root_path = session.getServletContext().getRealPath("");
//    	String filePath = root_path + "image/jpeg";
//    	com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
//        fileMetadata.setName("My Report");
//        //fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
//
//        java.io.File file = new java.io.File(filePath);
//        FileContent mediaContent = new FileContent("image/jpeg", file);
//        com.google.api.services.drive.model.File file1 = service.files().create(fileMetadata, mediaContent)
//            .setFields("id")
//            .execute();
//        System.out.println("File ID: " + file1.getId());
	}
	
}
