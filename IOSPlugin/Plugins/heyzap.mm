#include <heyzap.h>
#import <Heyzap/Heyzap.h>
#include <stdlib.h>
#include <glog.h>

class GHeyzap;

@interface GHeyzapDelegate : NSObject<HZAdsDelegate>
{
    
}

@property (nonatomic, assign) GHeyzap *gh;

@end

class GHeyzap
{
public:
	GHeyzap()
	{
		gid_ = g_NextId();
        
        delegate_ = [[GHeyzapDelegate alloc] init];
        delegate_.gh = this;
	}

	~GHeyzap()
	{
        delegate_.gh = NULL;
        [delegate_ release];
        gevent_RemoveEventsWithGid(gid_);
	}
	
	void init(bool useAds, const char* appId)
	{
		[HeyzapSDK startHeyzapWithAppId: [NSString stringWithUTF8String:appId]];
        if(useAds)
        {
            [[HeyzapSDK sharedSDK] enableAds:delegate_];
        }
	}
	
	void showAd(gheyzap_Parameter *params)
	{
        if(params->value)
        {
            [[HeyzapSDK sharedSDK] showAd:[NSString stringWithUTF8String:params->value]];
        }
        else
        {
            [[HeyzapSDK sharedSDK] showAd];
        }
	}
	
	void hideAd()
	{
		
	}
	
	void submitScore(const char *score, const char *displayScore, const char *level)
	{
		HZScore *sc = [[HZScore alloc] init];
        sc.levelID = [NSString stringWithUTF8String:level];
        sc.relativeScore = [[NSString stringWithUTF8String:score] floatValue];
        sc.displayScore = [NSString stringWithUTF8String:displayScore];
        [[HeyzapSDK sharedSDK] submitScore:sc withCompletion:nil];
	}
	
	void showLeaderboard(const char* level)
	{
        if(level != NULL)
        {
            [[HeyzapSDK sharedSDK] openLeaderboardLevel:[NSString stringWithUTF8String:level]];
        }
        else
        {
            [[HeyzapSDK sharedSDK] openLeaderboard];
        }
	}
	
	void unlockAchievement(const char *achievement)
	{
		[[HeyzapSDK sharedSDK] unlockAchievementsWithIDs:@[[NSString stringWithUTF8String:achievement]] completion:nil];
	}
	
	void showAchievements()
	{
		[[HeyzapSDK sharedSDK] showAllAchievementsWithCompletion:nil];
	}
	
	void checkin(const char *message)
	{
        if(message != NULL)
        {
            [[HeyzapSDK sharedSDK] checkinWithMessage:[NSString stringWithUTF8String:message]];
        }
        else
        {
            [[HeyzapSDK sharedSDK] checkin];
        }
	}
	
	void onAdReceived()
	{
		gevent_EnqueueEvent(gid_, callback_s, GHEYZAP_AD_RECEIVED_EVENT, NULL, 1, this);
	}
	
	void onAdFailed()
	{
		gevent_EnqueueEvent(gid_, callback_s, GHEYZAP_AD_FAILED_EVENT, NULL, 1, this);
	}
	
	void onAdActionBegin()
	{
		gevent_EnqueueEvent(gid_, callback_s, GHEYZAP_AD_ACTION_BEGIN_EVENT, NULL, 1, this);
	}
	
	void onAdActionEnd()
	{
		gevent_EnqueueEvent(gid_, callback_s, GHEYZAP_AD_ACTION_END_EVENT, NULL, 1, this);
	}
	
	void onAdDismissed()
	{
		gevent_EnqueueEvent(gid_, callback_s, GHEYZAP_AD_DISMISSED_EVENT, NULL, 1, this);
	}
	
	g_id addCallback(gevent_Callback callback, void *udata)
	{
		return callbackList_.addCallback(callback, udata);
	}
	void removeCallback(gevent_Callback callback, void *udata)
	{
		callbackList_.removeCallback(callback, udata);
	}
	void removeCallbackWithGid(g_id gid)
	{
		callbackList_.removeCallbackWithGid(gid);
	}

private:
	static void callback_s(int type, void *event, void *udata)
	{
		((GHeyzap*)udata)->callback(type, event);
	}

	void callback(int type, void *event)
	{
		callbackList_.dispatchEvent(type, event);
	}

private:
	gevent_CallbackList callbackList_;
    GHeyzapDelegate *delegate_;

private:
	g_id gid_;
};


@implementation GHeyzapDelegate

@synthesize gh = gh_;

-(void)didShowAd
{
    if(gh_)
        gh_->onAdReceived();
}

-(void)didFailToReceiveAd
{
    if(gh_)
        gh_->onAdFailed();
}

-(void)didFailToShowAd:(NSError *)error
{
    if(gh_)
        gh_->onAdFailed();
}

-(void)didClickAd{
    if(gh_)
        gh_->onAdActionBegin();
}

-(void)didHideAd
{
    if(gh_)
        gh_->onAdDismissed();
}

-(void)didReceiveAd
{
  
}

@end

static GHeyzap *s_heyzap = NULL;

extern "C" {

void gheyzap_init()
{
	s_heyzap = new GHeyzap;
}

void gheyzap_cleanup()
{
	delete s_heyzap;
	s_heyzap = NULL;
}

void gheyzap_initialize(bool useAds, const char *appId)
{
	s_heyzap->init(useAds, appId);
}

void gheyzap_showAd(gheyzap_Parameter *params)
{
	s_heyzap->showAd(params);
}

void gheyzap_hideAd()
{
	s_heyzap->hideAd();
}

void gheyzap_submitScore(const char *score, const char *displayScore, const char *level)
{
	s_heyzap->submitScore(score, displayScore, level);
}

void gheyzap_showLeaderboard(const char* level)
{
	s_heyzap->showLeaderboard(level);
}

void gheyzap_unlockAchievement(const char *achievement)
{
	s_heyzap->unlockAchievement(achievement);
}

void gheyzap_showAchievements()
{
	s_heyzap->showAchievements();
}

void gheyzap_checkin(const char *message)
{
	s_heyzap->checkin(message);
}

g_id gheyzap_addCallback(gevent_Callback callback, void *udata)
{
	return s_heyzap->addCallback(callback, udata);
}

void gheyzap_removeCallback(gevent_Callback callback, void *udata)
{
	s_heyzap->removeCallback(callback, udata);
}

void gheyzap_removeCallbackWithGid(g_id gid)
{
	s_heyzap->removeCallbackWithGid(gid);
}

}
