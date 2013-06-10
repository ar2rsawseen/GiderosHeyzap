#include <heyzap.h>
#include <jni.h>
#include <stdlib.h>
#include <glog.h>

extern "C" {
JavaVM *g_getJavaVM();
JNIEnv *g_getJNIEnv();
}

class GHeyzap
{
public:
	GHeyzap()
	{
		gid_ = g_NextId();
		
		JNIEnv *env = g_getJNIEnv();

		jclass localClass = env->FindClass("com/giderosmobile/android/plugins/heyzap/GHeyzap");
		cls_ = (jclass)env->NewGlobalRef(localClass);
		env->DeleteLocalRef(localClass);
		
		jclass class_sparse = env->FindClass("android/util/SparseArray");
		clsSparse = static_cast<jclass>(env->NewGlobalRef(class_sparse));
		env->DeleteLocalRef(class_sparse);

		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "init", "(J)V"), (jlong)this);
	}

	~GHeyzap()
	{
		JNIEnv *env = g_getJNIEnv();

		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "cleanup", "()V"));
		
		env->DeleteGlobalRef(cls_);
		env->DeleteGlobalRef(clsSparse);
		
		gevent_RemoveEventsWithGid(gid_);
	}
	
	void init(bool useAds, const char *appId)
	{
		JNIEnv *env = g_getJNIEnv();
		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "initialize", "(Z)V"), (jboolean)useAds);
	}
	
	void showAd(gheyzap_Parameter *params)
	{
		JNIEnv *env = g_getJNIEnv();
		
		//create Java object
		jobject jparams = env->NewObject(clsSparse, env->GetMethodID(clsSparse, "<init>", "()V"));
		int i = 0;
		while (params->value)
		{
			jstring jVal = env->NewStringUTF(params->value);
			env->CallVoidMethod(jparams, env->GetMethodID(clsSparse, "put", "(ILjava/lang/Object;)V"), (jint)i, jVal);
			env->DeleteLocalRef(jVal);
			++params;
			i++;
		}
		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "showAd", "(Ljava/lang/Object;)V"), jparams);
		env->DeleteLocalRef(jparams);
	}
	
	void hideAd()
	{
		JNIEnv *env = g_getJNIEnv();
		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "hideAd", "()V"));
	}
	
	void submitScore(const char *score, const char *displayScore, const char *level)
	{
		JNIEnv *env = g_getJNIEnv();
		jstring jscore = env->NewStringUTF(score);
		jstring jdisplayScore = env->NewStringUTF(displayScore);
		jstring jlevel = env->NewStringUTF(level);
		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "submitScore", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"), jscore, jdisplayScore, jlevel);
		env->DeleteLocalRef(jscore);
		env->DeleteLocalRef(jdisplayScore);
		env->DeleteLocalRef(jlevel);
	}
	
	void showLeaderboard(const char* level)
	{
		JNIEnv *env = g_getJNIEnv();
		if(level != NULL)
		{
			jstring jlevel = env->NewStringUTF(level);
			env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "showLeaderboard", "(Ljava/lang/String;)V"), jlevel);
			env->DeleteLocalRef(jlevel);
		}
		else
		{
			env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "showLeaderboard", "()V"));
		}
	}
	
	void unlockAchievement(const char *achievement)
	{
		JNIEnv *env = g_getJNIEnv();
		jstring jachievement = env->NewStringUTF(achievement);
		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "unlockAchievement", "(Ljava/lang/String;)V"), jachievement);
		env->DeleteLocalRef(jachievement);
	}
	
	void showAchievements()
	{
		JNIEnv *env = g_getJNIEnv();
		env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "showAchievements", "()V"));
	}
	
	void checkin(const char *message)
	{
		JNIEnv *env = g_getJNIEnv();
		if(message != NULL)
		{
			jstring jmessage = env->NewStringUTF(message);
			env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "checkin", "(Ljava/lang/String;)V"), jmessage);
			env->DeleteLocalRef(jmessage);
		}
		else
		{
			env->CallStaticVoidMethod(cls_, env->GetStaticMethodID(cls_, "checkin", "()V"));
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

private:
	jclass cls_;
	jclass clsSparse;
	g_id gid_;
};

extern "C" {

void Java_com_giderosmobile_android_plugins_heyzap_GHeyzap_onAdReceived(JNIEnv *env, jclass clz, jlong data)
{
	((GHeyzap*)data)->onAdReceived();
}

void Java_com_giderosmobile_android_plugins_heyzap_GHeyzap_onAdFailed(JNIEnv *env, jclass clz, jlong data)
{
	((GHeyzap*)data)->onAdFailed();
}

void Java_com_giderosmobile_android_plugins_heyzap_GHeyzap_onAdActionBegin(JNIEnv *env, jclass clz, jlong data)
{
	((GHeyzap*)data)->onAdActionBegin();
}

void Java_com_giderosmobile_android_plugins_heyzap_GHeyzap_onAdActionEnd(JNIEnv *env, jclass clz, jlong data)
{
	((GHeyzap*)data)->onAdActionEnd();
}

void Java_com_giderosmobile_android_plugins_heyzap_GHeyzap_onAdDismissed(JNIEnv *env, jclass clz, jlong data)
{
	((GHeyzap*)data)->onAdDismissed();
}

}

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
