package com.heyzap.sdk.ads;

import java.util.ArrayList;

class AdCache {
    private ArrayList<AdModel> ads;
    
    public AdCache() {
        this.ads = new ArrayList<AdModel>();
    }
    
    public AdModel pop(int adUnit, String tag) {
        clean();
        
        tag = AdModel.normalizeTag(tag);
        
        for (AdModel ad : this.ads) {
            if (ad.getAdUnit() == adUnit && AdModel.normalizeTag(ad.getTag()).equals(tag)
                    && !ad.isExpired()) {
                this.ads.remove(ad);
                return ad;
            }
        }
        
        return null;
    }
    
    public AdModel pop(String impressionId) {
        clean();
        
        for (AdModel ad : this.ads) {
            if (ad.getImpressionId().equals(impressionId) && !ad.isExpired()) {
                this.ads.remove(ad);
                return ad;
            }
        }
        
        return null;
    }
    
    public AdModel peek(int adUnit, String tag) {
        clean();
        
        tag = AdModel.normalizeTag(tag);
        
        for (AdModel ad : this.ads) {
            if (ad.getAdUnit() == adUnit && AdModel.normalizeTag(ad.getTag()).equals(tag)
                    && !ad.isExpired()) {
                return ad;
            }
        }
        
        return null;
    }
    
    public AdModel peek(String impressionId) {
        clean();
        
        for (AdModel ad : this.ads) {
            if (ad.getImpressionId().equals(impressionId) && !ad.isExpired()) {
                return ad;
            }
        }
        
        return null;
    }
    
    public Boolean has(int adUnit, String tag) {
        clean();
        
        tag = AdModel.normalizeTag(tag);
        
        for (AdModel ad : this.ads) {
            if (ad.getAdUnit() == adUnit && AdModel.normalizeTag(ad.getTag()).equals(tag)
                    && !ad.isExpired()) {
                return true;
            }
        }
        
        return false;
    }
    
    public Boolean has(String impressionId) {
        clean();
        
        for (AdModel ad : this.ads) {
            if (ad.getImpressionId().equals(impressionId) && !ad.isExpired()) {
                return true;
            }
        }
        
        return false;
    }
    
    public void put(AdModel ad) {
        clean();
        this.ads.add(ad);
    }
    
    private void clean() {
        ArrayList<AdModel> clonedAds = (ArrayList<AdModel>) this.ads.clone();
        for (AdModel ad : clonedAds) {
            if (ad.isExpired()) {
                ads.remove(ad);
            }
        }
    }
    
    /* */
    protected synchronized static AdCache getInstance() {
        if (ref == null) {
            ref = new AdCache();
        }
        
        return ref;
    }
    
    @Override
    public Object clone() {
        return null;
    }
    
    private static volatile AdCache ref;
}
