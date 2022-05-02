package com.yanzhenjie.andserver.register;

import android.content.Context;
import com.joas.posco_slow_charlcd.webservice.CORSInterceptor;
import com.yanzhenjie.andserver.framework.HandlerInterceptor;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This file was generated by AndServer automatically and you should NOT edit it. */
public final class InterceptorRegister implements OnRegister {
  private Map<String, List<HandlerInterceptor>> mMap;

  public InterceptorRegister() {
    this.mMap = new HashMap<>();
    List<HandlerInterceptor> defaultList = new ArrayList<>();
    defaultList.add(new CORSInterceptor());
    this.mMap.put("default", defaultList);
  }

  @Override
  public void onRegister(Context context, String group, Register register) {
    List<HandlerInterceptor> list = mMap.get(group);
    if(list != null && !list.isEmpty()) {
      for (HandlerInterceptor interceptor : list) {
        register.addInterceptor(interceptor);
      }
    }
  }
}
