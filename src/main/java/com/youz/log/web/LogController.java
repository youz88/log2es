package com.youz.log.web;

import com.youz.log.util.U;
import com.youz.log.util.json.JsonResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
public class LogController {

    @PostMapping("/upload")
    public JsonResult upload(MultipartFile file){
        U.assertException(file.isEmpty(),"上传失败，请选择文件");
        String fileName = file.getOriginalFilename();
        System.out.println(fileName);
        return JsonResult.success("上传成功");
    }

}
