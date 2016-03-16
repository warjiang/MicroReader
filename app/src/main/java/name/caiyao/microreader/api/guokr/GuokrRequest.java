package name.caiyao.microreader.api.guokr;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;

import name.caiyao.microreader.MicroApplication;
import name.caiyao.microreader.utils.NetWorkUtil;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by 蔡小木 on 2016/3/7 0007.
 */
public class GuokrRequest {
    public static String[] channel_key = {"hot", "frontier", "review", "interview", "visual", "brief", "fact", "techb"};
    public static String[] channel_title = {"热点", "前沿", "评论", "专访", "视觉", "速读", "谣言粉碎机", "商业科技"};
    public static String science_channel_url = "http://www.guokr.com";
    public static HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(HttpLoggingInterceptor.Logger.DEFAULT).setLevel(HttpLoggingInterceptor.Level.BODY);
    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            if (NetWorkUtil.isNetWorkAvaliable(MicroApplication.getContext())) {
                int maxAge = 60; // 在线缓存在1分钟内可读取
                Logger.i("在线缓存！");
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                Logger.i("离线缓存！");
                Request request = chain.request();
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
                originalResponse = chain.proceed(request);
                int maxStale = 60 * 60 * 24 * 28; // 离线时缓存保存4周
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
        }
    };

    static File httpCacheDirectory = new File(MicroApplication.getContext().getCacheDir(), "guokrCache");
    static int cacheSize = 10 * 1024 * 1024; // 10 MiB
    static Cache cache = new Cache(httpCacheDirectory, cacheSize);

    static OkHttpClient client = new OkHttpClient.Builder()
            .addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
            .addInterceptor(interceptor)
            .cache(cache)
            .build();

    private static GuokrApi guokrApi = null;
    public static GuokrApi getGuokrApi() {
        if (guokrApi == null) {
            guokrApi = new Retrofit.Builder()
                    .baseUrl(science_channel_url)
                    .client(client)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(GuokrApi.class);
        }
        return guokrApi;
    }

}
