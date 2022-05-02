package com.joas.posco_slow_charlcd.webservice;

import android.content.Context;
import com.yanzhenjie.andserver.framework.MessageConverter;
import com.yanzhenjie.andserver.framework.handler.MappingHandler;
import com.yanzhenjie.andserver.framework.view.ObjectView;
import com.yanzhenjie.andserver.framework.view.View;
import com.yanzhenjie.andserver.http.HttpMethod;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.RequestBody;
import com.yanzhenjie.andserver.http.multipart.MultipartRequest;
import com.yanzhenjie.andserver.mapping.Addition;
import com.yanzhenjie.andserver.mapping.Mapping;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.util.Map;

/**
 * This file was generated by AndServer automatically and you should NOT edit it. */
public final class WebControllerPostHandler extends MappingHandler {
  private Object mHost;

  public WebControllerPostHandler(Object host, Mapping mapping, Addition addition, boolean isRest) {
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

    Object o = null;
    try {
      o = ((WebController)mHost).post(request, response);
    }
    catch (Throwable e) {
      throw e;
    }
    return new ObjectView(true, o);
  }
}
