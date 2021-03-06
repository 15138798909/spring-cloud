package com.maple.common.minio.http;

import com.maple.common.minio.util.MinioClientUtils;
import com.maple.common.minio.vo.MinioItem;
import com.maple.common.minio.vo.MinioObject;
import io.minio.messages.Bucket;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MinioEndpoint 配置
 *
 * @author zhua
 * @date 2019/8/16
 */
@RestController
@RequestMapping("${minio.endpoint.name:/minio}")
@AllArgsConstructor
public class MinioEndpoint {

    private MinioClientUtils minioClientUtils;

    @GetMapping("/test")
    public String test() {
        return "success";
    }

    /**
     *
     * Bucket Endpoints
     */
    @SneakyThrows
    @PostMapping("/bucket/{bucketName}")
    public Bucket createBucker(@PathVariable String bucketName) {

        minioClientUtils.createBucket(bucketName);
        return minioClientUtils.getBucket(bucketName).get();

    }

    @SneakyThrows
    @GetMapping("/bucket")
    public List<Bucket> getBuckets() {
        return minioClientUtils.getAllBuckets();
    }

    @SneakyThrows
    @GetMapping("/bucket/{bucketName}")
    public Bucket getBucket(@PathVariable String bucketName) {
        return minioClientUtils.getBucket(bucketName).orElseThrow(() -> new IllegalArgumentException("Bucket Name not found!"));
    }

    @SneakyThrows
    @DeleteMapping("/bucket/{bucketName}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteBucket(@PathVariable String bucketName) {
        minioClientUtils.removeBucket(bucketName);
    }

    /**
     *
     * Object Endpoints
     */
    @SneakyThrows
    @PostMapping("/object/{bucketName}")
    public MinioObject createObject(@RequestBody MultipartFile object, @PathVariable String bucketName) {
        String name = object.getOriginalFilename();
        minioClientUtils.saveObject(bucketName, name, object.getInputStream(), object.getSize(), object.getContentType());
        return new MinioObject(minioClientUtils.getObjectInfo(bucketName, name));

    }

    @SneakyThrows
    @PostMapping("/object/{bucketName}/{objectName}")
    public MinioObject createObject(@RequestBody MultipartFile object, @PathVariable String bucketName, @PathVariable String objectName) {
        minioClientUtils.saveObject(bucketName, objectName, object.getInputStream(), object.getSize(), object.getContentType());
        return new MinioObject(minioClientUtils.getObjectInfo(bucketName, objectName));

    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}")
    public List<MinioItem> filterObject(@PathVariable String bucketName, @PathVariable String objectName) {

        return minioClientUtils.listFilesSwap(bucketName, objectName, true);

    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}/{expires}")
    public Map<String, Object> getObject(@PathVariable String bucketName, @PathVariable String objectName, @PathVariable Integer expires) {
        Map<String,Object> responseBody = new HashMap<>();
        // Put Object info
        responseBody.put("bucket" , bucketName);
        responseBody.put("object" , objectName);
        responseBody.put("url" , minioClientUtils.getObjectURL(bucketName, objectName, expires));
        responseBody.put("expires" ,  expires);
        return  responseBody;
    }

    @SneakyThrows
    @DeleteMapping("/object/{bucketName}/{objectName}/")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteObject(@PathVariable String bucketName, @PathVariable String objectName) {

        minioClientUtils.removeObject(bucketName, objectName);
    }

    @SneakyThrows
    @GetMapping("/download/{bucketName}/{objectName}")
    public void downloadObject(@PathVariable String bucketName, @PathVariable String objectName, HttpServletResponse response) {
        InputStream in = null;
        OutputStream out = null;
        try {
            int len = 0;
            byte[] buffer = new byte[1024];
            out = response.getOutputStream();
            response.reset();
            response.addHeader("Content-Disposition",
                    " attachment;filename=" + new String(objectName.getBytes(),"iso-8859-1"));
            response.setContentType("application/octet-stream");

            in = minioClientUtils.getObject(bucketName, objectName);
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null){
                try {
                    in.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
