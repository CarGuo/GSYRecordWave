package com.shuyu.waveview;

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

/**
 * Created by shuyu on 2016/12/19.
 */

public class Manager {

    private static Manager mInstance;

    private HttpProxyCacheServer mProxy; //视频代理


    public static synchronized Manager newInstance() {
        if (mInstance == null) {
            mInstance = new Manager();
        }
        return mInstance;
    }

    public HttpProxyCacheServer getProxy(Context context) {
        if (mProxy == null) {
            mProxy = newProxy(context);
        }
        return mProxy;
    }

    /**
     * 创建缓存代理服务
     */
    private HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer(context.getApplicationContext());
    }

}
