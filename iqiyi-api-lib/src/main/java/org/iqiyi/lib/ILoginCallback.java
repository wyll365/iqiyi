package org.iqiyi.lib;

import java.util.Map;

public interface ILoginCallback {
    void onError(Exception e);
    void onSuccess(Map<String,String> cookie);
}
