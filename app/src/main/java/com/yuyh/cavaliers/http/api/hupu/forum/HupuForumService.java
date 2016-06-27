package com.yuyh.cavaliers.http.api.hupu.forum;

import android.text.TextUtils;
import android.util.Log;

import com.yuyh.cavaliers.BuildConfig;
import com.yuyh.cavaliers.http.api.RequestCallback;
import com.yuyh.cavaliers.http.bean.BaseData;
import com.yuyh.cavaliers.http.bean.forum.AttendStatusData;
import com.yuyh.cavaliers.http.bean.forum.ForumsData;
import com.yuyh.cavaliers.http.bean.forum.ThreadListData;
import com.yuyh.cavaliers.http.bean.forum.ThreadsSchemaInfoData;
import com.yuyh.cavaliers.http.okhttp.OkHttpHelper;
import com.yuyh.cavaliers.http.util.RequestHelper;
import com.yuyh.cavaliers.utils.SettingPrefUtils;
import com.yuyh.library.AppUtils;
import com.yuyh.library.utils.data.ACache;
import com.yuyh.library.utils.log.LogUtils;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author yuyh.
 * @date 16/6/25.
 */
public class HupuForumService {

    static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BuildConfig.HUPU_FORUM_SERVER)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpHelper.getAppClient())
            .build();

    public static HupuForumApi apiStr = retrofit.create(HupuForumApi.class);

    /**
     * 获取论坛板块列表
     *
     * @param cbk
     */
    public static void getAllForums(final RequestCallback<ForumsData> cbk) {
        final String key = "getAllForums";
        final ACache cache = ACache.get(AppUtils.getAppContext());
        Object obj = cache.getAsObject(key);
        if (obj != null) {
            ForumsData match = (ForumsData) obj;
            cbk.onSuccess(match);
            return;
        }
        Map<String, String> params = RequestHelper.getRequsetMap();
        String sign = RequestHelper.getRequestSign(params);

        Call<ForumsData> call = apiStr.getForums(sign, params);
        call.enqueue(new retrofit2.Callback<ForumsData>() {
            @Override
            public void onResponse(Call<ForumsData> call, retrofit2.Response<ForumsData> response) {
                ForumsData data = response.body();
                cbk.onSuccess(data);
                cache.put(key, data);
            }

            @Override
            public void onFailure(Call<ForumsData> call, Throwable t) {
                cbk.onFailure(t.getMessage());
                cache.remove(key);
            }
        });
    }

    /**
     * 获取论坛帖子列表
     *
     * @param fid      论坛id，通过getForums接口获取
     * @param lastTid  最后一篇帖子的id
     * @param limit    分页大小
     * @param lastTamp 时间戳
     * @param type     加载类型  1 按发帖时间排序  2 按回帖时间排序
     */
    public static void getForumPosts(String fid, String lastTid, int limit, String lastTamp, String type, final RequestCallback<ThreadListData> cbk) {
        Map<String, String> params = RequestHelper.getRequsetMap();
        params.put("fid", fid);
        params.put("lastTid", lastTid);
        params.put("limit", String.valueOf(limit));
        params.put("isHome", "1");
        params.put("stamp", lastTamp);
        params.put("password", "0");
        params.put("special", "0");
        params.put("type", type);
        String sign = RequestHelper.getRequestSign(params);

        Call<ThreadListData> call = apiStr.getForumInfosList(sign, params);
        call.enqueue(new retrofit2.Callback<ThreadListData>() {
            @Override
            public void onResponse(Call<ThreadListData> call, retrofit2.Response<ThreadListData> response) {
                ThreadListData data = response.body();
                cbk.onSuccess(data);
            }

            @Override
            public void onFailure(Call<ThreadListData> call, Throwable t) {
                cbk.onFailure(t.getMessage());
            }
        });
    }

    /**
     * 发新帖 (params 须带token信息)
     *
     * @param title   标题
     * @param content 内容
     * @param fid     论坛id
     */
    public static void addThread(String title, String content, String fid) {
        Map<String, String> params = RequestHelper.getRequsetMap();
        params.put("title", title);
        params.put("content", content);
        params.put("fid", fid);
        String sign = RequestHelper.getRequestSign(params);
        params.put("sign", sign);

        Call<Object> call = apiStr.addThread(params);
        call.enqueue(new retrofit2.Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, retrofit2.Response<Object> response) {
                LogUtils.i("----" + response.body());
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtils.e("-----" + t.getMessage());
            }
        });
    }

    /**
     * 评论或者回复
     *
     * @param tid 帖子id
     * @param fid 论坛id
     * @param pid 回复id（评论时为空，回复某条回复的为回复的id）
     * @param content 内容
     */
    public static void addReplyByApp(String tid, String fid, String pid, String content) {
        Map<String, String> params = RequestHelper.getRequsetMap();
        params.put("tid", tid);
        params.put("content", content);
        params.put("fid", fid);
        if (!TextUtils.isEmpty(pid)) {
            params.put("quotepid", pid);
            params.put("boardpw", "");
        }
        String sign = RequestHelper.getRequestSign(params);
        params.put("sign", sign);
        Log.d("groupApi", "gson.toJson(params):" + params);

        Call<BaseData> call = apiStr.addReplyByApp(params);
        call.enqueue(new Callback<BaseData>() {
            @Override
            public void onResponse(Call<BaseData> call, Response<BaseData> response) {
                LogUtils.i("----"+response.body()+"----"+response.code());
            }

            @Override
            public void onFailure(Call<BaseData> call, Throwable t) {
                LogUtils.e(t.getMessage());
            }
        });
    }

    /**
     * 获取帖子详情
     *
     * @param tid  帖子id
     * @param fid  论坛id
     * @param page 页数
     * @param pid  回复id
     */
    public static void getThreadInfo(String tid, String fid, int page, String pid, final RequestCallback<ThreadsSchemaInfoData> cbk) {
        Map<String, String> params = RequestHelper.getRequsetMap();
        if (!TextUtils.isEmpty(tid)) {
            params.put("tid", tid);
        }
        if (!TextUtils.isEmpty(fid)) {
            params.put("fid", fid);
        }
        params.put("page", page + "");
        if (!TextUtils.isEmpty(pid)) {
            params.put("pid", pid);
        }
        params.put("nopic", "0");
        String sign = RequestHelper.getRequestSign(params);

        Call<ThreadsSchemaInfoData> call = apiStr.getThreadInfo(sign, params);
        call.enqueue(new retrofit2.Callback<ThreadsSchemaInfoData>() {
            @Override
            public void onResponse(Call<ThreadsSchemaInfoData> call, retrofit2.Response<ThreadsSchemaInfoData> response) {
                ThreadsSchemaInfoData data = response.body();
                cbk.onSuccess(data);
            }

            @Override
            public void onFailure(Call<ThreadsSchemaInfoData> call, Throwable t) {
                cbk.onFailure(t.getMessage());
            }
        });
    }

    /**
     * 获取论坛关注状态
     *
     * @param fid 论坛id
     */
    public static void getAttentionStatus(String fid, final RequestCallback<AttendStatusData> cbk) {
        Map<String, String> params = RequestHelper.getRequsetMap();
        params.put("fid", fid);
        params.put("uid", SettingPrefUtils.getUid());
        String sign = RequestHelper.getRequestSign(params);

        Call<AttendStatusData> call = apiStr.getAttentionStatus(sign, params);
        call.enqueue(new retrofit2.Callback<AttendStatusData>() {
            @Override
            public void onResponse(Call<AttendStatusData> call, retrofit2.Response<AttendStatusData> response) {
                AttendStatusData data = response.body();
                cbk.onSuccess(data);
            }

            @Override
            public void onFailure(Call<AttendStatusData> call, Throwable t) {
                cbk.onFailure(t.getMessage());
            }
        });
    }
}
