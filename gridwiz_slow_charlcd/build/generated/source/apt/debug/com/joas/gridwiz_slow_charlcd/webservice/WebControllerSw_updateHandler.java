package com.joas.gridwiz_slow_charlcd.webservice;

import android.content.Context;
import com.yanzhenjie.andserver.error.ParamMissingException;
import com.yanzhenjie.andserver.error.ParamValidateException;
import com.yanzhenjie.andserver.framework.MessageConverter;
import com.yanzhenjie.andserver.framework.handler.MappingHandler;
import com.yanzhenjie.andserver.framework.view.ObjectView;
import com.yanzhenjie.andserver.framework.view.View;
import com.yanzhenjie.andserver.http.HttpMethod;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.RequestBody;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.http.multipart.MultipartRequest;
import com.yanzhenjie.andserver.mapping.Addition;
import com.yanzhenjie.andserver.mapping.Mapping;
import com.yanzhenjie.andserver.util.StringUtils;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Map;

/**
 * This file was generated by AndServer automatically and you should NOT edit it. */
public final class WebControllerSw_updateHandler extends MappingHandler {
  private Object mHost;

  public WebControllerSw_updateHandler(Object host, Mapping mapping, Addition addition,
      boolean isRest) {
    super(host, mapping, addition, isRest);
    this.mHost = host;
  }

  @Override
  public View handle(HttpRequest request, HttpResponse response) throws IOException {
    Context context = (Context)request.getAttribute(HttpRequest.ANDROID_CONTEXT);
    String httpPath = request.getPath();
    HttpMethod httpMethod = request.getMethod();

    Object converterObj = request.getAttribute(HttpRequest.HTTP_MESSAGE_CONVERTER);
    MessageConverter converter = null;
    if (converterObj != null && converterObj instanceof MessageConverter) {
      converter = (MessageConverter)converterObj;
    }

    MultipartRequest multiRequest = null;
    if (request instanceof MultipartRequest) {
      multiRequest = (MultipartRequest) request;
    }

    RequestBody requestBody = null;
    if (httpMethod.allowBody()) {
      requestBody = request.getBody();
    }

    Map<String, String> pathMap = getPathVariable(httpPath);

    /** ---------- Building Parameters ---------- **/ 

    MultipartFile param0 = null;
    if (multiRequest != null) {
      param0 = multiRequest.getFile("file");
    }
    if (param0 == null) {
      throw new ParamMissingException("file");
    }

    String param1Str = request.getParameter("token");
    if (StringUtils.isEmpty(param1Str)) {
      throw new ParamMissingException("token");
    }
    String param1 = null;
    try {
      param1 = String.valueOf(param1Str);
    } catch (Throwable e) {
      throw new ParamValidateException(e);
    }

    Object o = null;
    try {
      o = ((WebController)mHost).sw_update(param0, param1);
    }
    catch (Throwable e) {
      throw e;
    }
    return new ObjectView(true, o);
  }
}
