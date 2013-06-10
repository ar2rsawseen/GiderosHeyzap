#ifndef HEYZAP_H
#define HEYZAP_H

#include <gglobal.h>
#include <gevent.h>

enum
{
	GHEYZAP_AD_RECEIVED_EVENT,
	GHEYZAP_AD_FAILED_EVENT,
	GHEYZAP_AD_ACTION_BEGIN_EVENT,
	GHEYZAP_AD_ACTION_END_EVENT,
	GHEYZAP_AD_DISMISSED_EVENT,
};

typedef struct gheyzap_Parameter
{
    const char *value;
} gheyzap_Parameter;

#ifdef __cplusplus
extern "C" {
#endif

G_API void gheyzap_init();
G_API void gheyzap_cleanup();

G_API void gheyzap_initialize(bool useAds, const char *appId);

G_API void gheyzap_showAd(gheyzap_Parameter *params);
G_API void gheyzap_hideAd();

G_API void gheyzap_submitScore(const char *score, const char *displayScore, const char *level);
G_API void gheyzap_showLeaderboard(const char* level);
G_API void gheyzap_unlockAchievement(const char *achievement);
G_API void gheyzap_showAchievements();
G_API void gheyzap_checkin(const char *message);

G_API g_id gheyzap_addCallback(gevent_Callback callback, void *udata);
G_API void gheyzap_removeCallback(gevent_Callback callback, void *udata);
G_API void gheyzap_removeCallbackWithGid(g_id gid);

#ifdef __cplusplus
}
#endif

#endif