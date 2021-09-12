package com.filling.client.standalone;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.filling.client.ClusterClient;
import com.filling.config.ApplicationProperties;
import com.filling.utils.Base64Utils;
import com.filling.web.rest.AccountResource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.shaded.okhttp3.*;

import java.io.File;
import java.util.Base64;
import java.util.Optional;

@Configuration
public class StandaloneClusterClient implements ClusterClient {

    private final Logger log = LoggerFactory.getLogger(StandaloneClusterClient.class);


    @Autowired
    ApplicationProperties flink;

    static JSONObject jarInfo;


    @Override
    public void init() {

        if (getLastFile() == null) {
            log.info("uploading fillingcore.... ");
            uploadJar();
            log.info("uploadJar success");
        }
        jarInfo = getLastFile();
        System.out.println("getLastFile: " + jarInfo.toJSONString());
    }

    @Override
    public Optional<String> submit(String jobText, Integer parallelism) {
        Optional<String> jobId = Optional.empty();
        String url = flink.getUrl() + "/jars/{id}/run?entry-class={entry-class}&parallelism={parallelism}&program-args={args}";
        url = url.replace("{id}", jarInfo.getString("id"));
        url = url.replace("{entry-class}", jarInfo.getJSONArray("entry").getJSONObject(0).getString("name"));
        url = url.replace("{parallelism}", parallelism.toString());
        url = url.replace("{args}", Base64Utils.encode(jobText));
        log.info("submit job url: {}", url);
        log.info("args is: {}", Base64Utils.encode(jobText));
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
            .url(url)
            .method("POST", body)
            .build();
        try(Response response = client.newCall(request).execute()) {
            String respStr = response.body().string();
            JSONObject resp = JSONObject.parseObject(respStr);
            if(StringUtils.isEmpty(resp.getString("errors"))) {
                log.info("submit success");
                log.info("submit result: {}",respStr);
                jobId = Optional.ofNullable(resp.getString("jobid"));
            } else {
                log.info("submit failed");
                new Exception(resp.getString("errors"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return jobId;
        }

    }

    @Override
    public Boolean cancel(String jobId) {
        Boolean result = true;
        String url = flink.getUrl() + "/jobs/{id}/yarn-cancel";
        url = url.replace("{id}", jobId);
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        try(Response response = client.newCall(request).execute()) {
            String respStr = response.body().string();
            log.info("Cancel success");
            log.info("Cancel result: {}",respStr);
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            return result;
        }
    }

    /**
     * 上传文件到集群
     *
     * @return
     */
    private String uploadJar() {
        String url = flink.getUrl() + "/jars/upload";
        File jarfile = new File(flink.getJar());

        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("jarfile", jarfile.getName(),
                    RequestBody.create(MediaType.parse("application/octet-stream"),
                        jarfile))
                .build();
            Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .build();
            Response response = client.newCall(request).execute();
            JSONObject jsonObject = JSONObject.parseObject(response.body().string());
            log.debug("response.body().string() {}", jsonObject.toJSONString());
            return jsonObject.getString("filename");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return 返回最新的一个jar包
     */
    private JSONObject getLastFile() {
        String url = flink.getUrl() + "/jars";
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        Request request = new Request.Builder()
            .url(url)
            .method("GET", null)
            .build();
        try (Response response = client.newCall(request).execute()) {

            JSONArray jsonArray = JSONObject.parseObject(response.body().string()).getJSONArray("files");

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                System.out.println(jsonObject.toJSONString());
                if ("calculation-core-1.0-SNAPSHOT.jar".equals(jsonObject.getString("name"))) {
                    return jsonObject;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
