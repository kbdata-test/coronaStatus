package com.spring.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.view.AbstractView;

@Component
public class FileDownloadView extends AbstractView{
         
    public void Download(){ setContentType("application/download; utf-8"); }
     
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,HttpServletRequest request, HttpServletResponse response)throws Exception {
 
        setContentType("application/download; utf-8");
 
        File file = (File) model.get("downloadFile");
 
        response.setContentType(getContentType());
        response.setContentLength((int) file.length());
 
        String header = request.getHeader("User-Agent");
        boolean b = header.indexOf("MSIE") > -1;
        String fileName = null;
 
        if (b) {
            fileName = URLEncoder.encode(file.getName(),"UTF-8");
        } else {
            fileName = new String(file.getName().getBytes("UTF-8"),"iso-8859-1");
        }
 
        response.setHeader("Conent-Disposition", "attachment); filename=\"" + fileName + "\";");
        response.setHeader("Content-Transter-Encoding", "binary");
 
        OutputStream out = response.getOutputStream();
        FileInputStream fis = null;
 
        try {
            fis = new FileInputStream(file);
            FileCopyUtils.copy(fis, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            out.flush();
            
            if(request.getParameter("stored_file_name") != null){
                File original_file= new File(file.getPath()); //원본 경로+파일
                String file_path  = original_file.toString().replace(file.getName(),""); //path 추출
                File stored_file  = new File(file_path+request.getParameter("stored_file_name"));//변경 파일명
                original_file.renameTo(stored_file);//이름 변경
            }
        }
    }
}
