/*
 * Copyright 2024 Orkes, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.orkes.conductor.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.Param;

import io.orkes.conductor.client.http.ApiCallback;
import io.orkes.conductor.client.http.ApiException;
import io.orkes.conductor.client.http.ApiResponse;
import io.orkes.conductor.client.http.OrkesAuthentication;
import io.orkes.conductor.client.http.Pair;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class exists to maintain backward compatibility and facilitate the migration for
 * users of orkes-conductor-client v2 to v3.
 */
@Deprecated
public final class ApiClient extends ConductorClient {

    public ApiClient(String rootUri, String keyId, String secret) {
        super(ConductorClient.builder()
                .basePath(rootUri)
                .addHeaderSupplier(new OrkesAuthentication(keyId, secret)));
    }

    public ApiClient(String rootUri) {
        super(rootUri);
    }

    @Deprecated
    public Call buildCall(
            String path,
            String method,
            List<Pair> pathParams,
            List<Pair> queryParams,
            Object body,
            Map<String, String> headers) {
        Request request = buildRequest(method, path, toParamList(pathParams), toParamList(queryParams), headers, body);
        return okHttpClient.newCall(request);
    }

    private List<Param> toParamList(List<Pair> pairList) {
        List<Param> params = new ArrayList<>();
        if (pairList != null) {
            params.addAll(pairList.stream()
                    .map(it -> new Param(it.getName(), it.getValue()))
                    .collect(Collectors.toList()));
        }

        return params;
    }

    /**
     * {@link #executeAsync(Call, Type, ApiCallback)}
     *
     * @param <T>      Type
     * @param call     An instance of the Call object
     * @param callback ApiCallback&lt;T&gt;
     */
    @Deprecated
    public <T> void executeAsync(Call call, ApiCallback<T> callback) {
        executeAsync(call, null, callback);
    }

    /**
     * Execute HTTP call asynchronously.
     *
     * @param <T>        Type
     * @param call       The callback to be executed when the API call finishes
     * @param returnType Return type
     * @param callback   ApiCallback
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public <T> void executeAsync(Call call, final Type returnType, final ApiCallback<T> callback) {
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                T result;
                try {
                    result = (T) handleResponse(response, returnType);
                } catch (ApiException e) {
                    callback.onFailure(e, response.code(), response.headers().toMultimap());
                    return;
                }
                callback.onSuccess(
                        result, response.code(), response.headers().toMultimap());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(new ApiException(e), 0, null);
            }
        });
    }

    @Deprecated
    public <T> ApiResponse<T> execute(Call call) throws ApiException {
        return execute(call, null);
    }

    /**
     * Execute HTTP call and deserialize the HTTP response body into the given return type.
     *
     * @param returnType The return type used to deserialize HTTP response body
     * @param <T>        The return type corresponding to (same with) returnType
     * @param call       Call
     * @return ApiResponse object containing response status, headers and data, which is a Java
     * object deserialized from response body and would be null when returnType is null.
     * @throws ApiException If fail to execute the call
     */
    @Deprecated
    public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
        try {
            Response response = call.execute();
            T data = handleResponse(response, returnType);
            return new ApiResponse<T>(response.code(), response.headers().toMultimap(), data);
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

}
