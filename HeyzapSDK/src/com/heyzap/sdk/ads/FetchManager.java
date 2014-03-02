package com.heyzap.sdk.ads;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.heyzap.internal.APIClient;
import com.heyzap.internal.APIResponseHandler;
import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

class FetchManager {
    
    protected interface OnFetchResponse {
        public void onFetchResponse(AdModel model, String tag, Throwable e);
    }
    
    public FetchManager() {
    }
    
    public void fetch(final Context context, final FetchRequest request,
            final FetchManager.OnFetchResponse responseHandler) {
        
        final FetchManager.OnFetchResponse callbackResponseHandler = new FetchManager.OnFetchResponse() {
            
            private Boolean sentResponse = false;
            
            @Override
            public void onFetchResponse(AdModel model, String tag, Throwable e) {
                if (sentResponse) {
                    return;
                }
                
                sentResponse = true;
                
                if (e != null) {
                    Logger.format("(FETCH FAILED) Error: %s", e);
                    if (Manager.displayListener != null) {
                        Manager.displayListener.onFailedToFetch(tag);
                    }
                } else if (model != null) {
                    Logger.format("(FETCH) %s", model);
                    AdCache.getInstance().put(model);
                    if (Manager.displayListener != null) {
                        Manager.displayListener.onAvailable(tag);
                    }
                } else {
                    // ????
                }
                
                // Pass it on
                if (responseHandler != null) {
                    responseHandler.onFetchResponse(model, tag, e);
                }
            }
        };
        
        if (!request.isValid()) {
            callbackResponseHandler.onFetchResponse(null, request.getTag(),
                    new Throwable("no_fill"));
            return;
        }
        
        request.incrementTries();
        
        APIClient.post(context, request.getUrl(), request.getParams(context),
                new APIResponseHandler() {
                    
                    @Override
                    public void onSuccess(final JSONObject response) {
                        Throwable throwable = null;
                        AdModel model = null;
                        
                        try {
                            if (!response.has("status")
                                    || response.isNull("status")
                                    || response.getInt("status") > 200) {
                                throw new Exception("bad_response");
                            }
                            
                            if (!response.has("impression_id")
                                    || response.isNull("impression_id")) {
                                throw new Exception("no_fill");
                            }
                            
                            if (!response.has("promoted_game_package")
                                    || response.isNull("promoted_game_package")
                                    || response.getString(
                                            "promoted_game_package").equals("")) {
                                throw new Exception("bad response");
                            }
                            
                            // Retry if package is already installed
                            String promotedGamePackage = response
                                    .getString("promoted_game_package");
                            if (Utils.packageIsInstalled(promotedGamePackage,
                                    Manager.applicationContext)) {
                                request.setRejectedImpressionId(response
                                        .getString("impression_id"));
                                FetchManager.this.fetch(context, request,
                                        responseHandler);
                                return;
                            }
                            
                            String adType = response.optString("creative_type",
                                    InterstitialModel.FORMAT);
                            
                            if (adType.equals(VideoModel.FORMAT)) {
                                model = new VideoModel(response);
                            } else {
                                model = new InterstitialModel(response);
                            }
                            
                            // Do all the post fetch stuff
                            model.setTag(request.getTag());
                            model.setAdUnit(request.getAdUnit());
                            model.doPostFetchActions(
                                    context,
                                    new AdModel.ModelPostFetchCompleteListener() {
                                        @Override
                                        public void onComplete(AdModel model,
                                                Throwable e) {
                                            callbackResponseHandler
                                                    .onFetchResponse(model,
                                                            request.getTag(), e);
                                        }
                                    });
                            
                        } catch (JSONException e) {
                            throwable = new Throwable("parse");
                        } catch (Exception e) {
                            if (!e.getMessage().equals("no_fill")
                                    && !e.getMessage().equals("bad_response")) {
                                e.printStackTrace();
                            }
                            
                            throwable = e;
                        } finally {
                            // We have somehow gotten here with no model and no
                            // error.
                            if (model == null && throwable == null) {
                                throwable = new Throwable("unknown");
                            }
                            
                            if (throwable != null) {
                                callbackResponseHandler.onFetchResponse(null,
                                        request.getTag(), throwable);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(final Throwable e) {
                        callbackResponseHandler.onFetchResponse(null,
                                request.getTag(), e);
                    }
                    
                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {
                        onFailure(e);
                    }
                    
                    @Override
                    public void onFailure(Throwable e, JSONArray errorResponse) {
                        onFailure(e);
                    }
                });
    }
    
    protected synchronized static FetchManager getInstance() {
        if (ref == null) {
            ref = new FetchManager();
        }
        
        return ref;
    }
    
    @Override
    public Object clone() {
        return null;
    }
    
    private static volatile FetchManager ref;
    
}
