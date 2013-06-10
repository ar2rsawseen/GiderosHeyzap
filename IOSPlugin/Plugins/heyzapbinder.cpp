#include "heyzap.h"
#include "gideros.h"
#include <glog.h>
#include <map>
#include <string>

// some Lua helper functions
#ifndef abs_index
#define abs_index(L, i) ((i) > 0 || (i) <= LUA_REGISTRYINDEX ? (i) : lua_gettop(L) + (i) + 1)
#endif

static void luaL_newweaktable(lua_State *L, const char *mode)
{
	lua_newtable(L);			// create table for instance list
	lua_pushstring(L, mode);
	lua_setfield(L, -2, "__mode");	  // set as weak-value table
	lua_pushvalue(L, -1);             // duplicate table
	lua_setmetatable(L, -2);          // set itself as metatable
}

static void luaL_rawgetptr(lua_State *L, int idx, void *ptr)
{
	idx = abs_index(L, idx);
	lua_pushlightuserdata(L, ptr);
	lua_rawget(L, idx);
}

static void luaL_rawsetptr(lua_State *L, int idx, void *ptr)
{
	idx = abs_index(L, idx);
	lua_pushlightuserdata(L, ptr);
	lua_insert(L, -2);
	lua_rawset(L, idx);
}

static std::map<std::string, std::string> tableToMap(lua_State *L, int index)
{
    luaL_checktype(L, index, LUA_TTABLE);
    
    std::map<std::string, std::string> result;
    
    int t = abs_index(L, index);
    
	lua_pushnil(L);
	while (lua_next(L, t) != 0)
	{
		lua_pushvalue(L, -2);
        std::string key = luaL_checkstring(L, -1);
		lua_pop(L, 1);
		
        std::string value = luaL_checkstring(L, -1);
		
		result[key] = value;
		
		lua_pop(L, 1);
	}
    
    return result;
}

static const char *AD_RECEIVED = "adReceived";
static const char *AD_FAILED = "adFailed";
static const char *AD_ACTION_BEGIN = "adActionBegin";
static const char *AD_ACTION_END = "adActionEnd";
static const char *AD_DISMISSED = "adDismissed";

static char keyWeak = ' ';

class Heyzap : public GEventDispatcherProxy
{
public:
    Heyzap(lua_State *L) : L(L)
    {
        gheyzap_init();
		gheyzap_addCallback(callback_s, this);		
    }
    
    ~Heyzap()
    {
		gheyzap_removeCallback(callback_s, this);
		gheyzap_cleanup();
    }
	
	void init(bool useAds, const char *appId)
	{
		gheyzap_initialize(useAds, appId);
	}
	
	void showAd(gheyzap_Parameter *params)
	{
		gheyzap_showAd(params);
	}
	
	void hideAd()
	{
		gheyzap_hideAd();
	}
	
	void submitScore(const char *score, const char *displayScore, const char *level)
	{
		gheyzap_submitScore(score, displayScore, level);
	}
	
	void showLeaderboard(const char *level)
	{
		gheyzap_showLeaderboard(level);
	}
	
	void unlockAchievement(const char *achievement)
	{
		gheyzap_unlockAchievement(achievement);
	}
	
	void showAchievements()
	{
		gheyzap_showAchievements();
	}
	
	void checkin(const char *message)
	{
		gheyzap_checkin(message);
	}
	
private:
	static void callback_s(int type, void *event, void *udata)
	{
		static_cast<Heyzap*>(udata)->callback(type, event);
	}
	
	void callback(int type, void *event)
	{
        dispatchEvent(type, event);
	}
	
	void dispatchEvent(int type, void *event)
	{
        luaL_rawgetptr(L, LUA_REGISTRYINDEX, &keyWeak);
        luaL_rawgetptr(L, -1, this);
		
        if (lua_isnil(L, -1))
        {
            lua_pop(L, 2);
            return;
        }
        
        lua_getfield(L, -1, "dispatchEvent");
		
        lua_pushvalue(L, -2);
        
        lua_getglobal(L, "Event");
        lua_getfield(L, -1, "new");
        lua_remove(L, -2);
        
        switch (type)
        {
			case GHEYZAP_AD_RECEIVED_EVENT:
                lua_pushstring(L, AD_RECEIVED);
                break;
			case GHEYZAP_AD_FAILED_EVENT:
                lua_pushstring(L, AD_FAILED);
                break;
			case GHEYZAP_AD_ACTION_BEGIN_EVENT:
                lua_pushstring(L, AD_ACTION_BEGIN);
                break;
			case GHEYZAP_AD_ACTION_END_EVENT:
                lua_pushstring(L, AD_ACTION_END);
                break;
			case GHEYZAP_AD_DISMISSED_EVENT:
                lua_pushstring(L, AD_DISMISSED);
                break;
        }

        lua_call(L, 1, 1);

		lua_call(L, 2, 0);
		
		lua_pop(L, 2);
	}

private:
    lua_State *L;
    bool initialized_;
};

static int destruct(lua_State* L)
{
	void *ptr = *(void**)lua_touserdata(L, 1);
	GReferenced* object = static_cast<GReferenced*>(ptr);
	Heyzap *heyzap = static_cast<Heyzap*>(object->proxy());
	
	heyzap->unref();
	
	return 0;
}

static Heyzap *getInstance(lua_State* L, int index)
{
	GReferenced *object = static_cast<GReferenced*>(g_getInstance(L, "Heyzap", index));
	Heyzap *heyzap = static_cast<Heyzap*>(object->proxy());
    
	return heyzap;
}

static int init(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
    bool useAds = lua_toboolean(L, 2);
	const char *appId = NULL;
	if(!lua_isnoneornil(L, 3))
	{
		appId = luaL_checkstring(L, 3);
	}
    heyzap->init(useAds, appId);
    return 0;
}

static int showAd(lua_State *L)
{
	Heyzap *heyzap = getInstance(L, 1);
	int i = 2;
	std::vector<gheyzap_Parameter> params2;
	while(!lua_isnoneornil(L, i))
	{
		gheyzap_Parameter param = {luaL_checkstring(L, i)};
		params2.push_back(param);
		i++;
	}
	gheyzap_Parameter param = {NULL};
	params2.push_back(param);
	heyzap->showAd(&params2[0]);
    return 0;
}

static int hideAd(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
    heyzap->hideAd();
    return 0;
}

static int submitScore(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
    const char *score = luaL_checkstring(L, 2);
    const char *displayScore = luaL_checkstring(L, 3);
    const char *level = luaL_checkstring(L, 4);
    heyzap->submitScore(score, displayScore, level);
    return 0;
}

static int showLeaderboard(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
	const char *level = NULL;
	if(!lua_isnoneornil(L, 2))
	{
		level = luaL_checkstring(L, 2);
	}
    heyzap->showLeaderboard(level);
    return 0;
}

static int unlockAchievement(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
    const char *achievement = luaL_checkstring(L, 2);
    heyzap->unlockAchievement(achievement);
    return 0;
}

static int showAchievements(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
    heyzap->showAchievements();
    return 0;
}

static int checkin(lua_State *L)
{
    Heyzap *heyzap = getInstance(L, 1);
	const char *message = NULL;
	if(!lua_isnoneornil(L, 2))
	{
		message = luaL_checkstring(L, 2);
	}
    heyzap->checkin(message);
    return 0;
}

static int loader(lua_State *L)
{
	const luaL_Reg functionlist[] = {
        {"init", init},
        {"showAd", showAd},
        {"hideAd", hideAd},
        {"submitScore", submitScore},
        {"showLeaderboard", showLeaderboard},
        {"unlockAchievement", unlockAchievement},
        {"showAchievements", showAchievements},
        {"checkin", checkin},
		{NULL, NULL},
	};
    
    g_createClass(L, "Heyzap", "EventDispatcher", NULL, destruct, functionlist);
    
	// create a weak table in LUA_REGISTRYINDEX that can be accessed with the address of keyWeak
    luaL_newweaktable(L, "v");
    luaL_rawsetptr(L, LUA_REGISTRYINDEX, &keyWeak);
    
	lua_getglobal(L, "Event");
	lua_pushstring(L, AD_RECEIVED);
	lua_setfield(L, -2, "AD_RECEIVED");
	lua_pushstring(L, AD_FAILED);
	lua_setfield(L, -2, "AD_FAILED");
	lua_pushstring(L, AD_ACTION_BEGIN);
	lua_setfield(L, -2, "AD_ACTION_BEGIN");
	lua_pushstring(L, AD_ACTION_END);
	lua_setfield(L, -2, "AD_ACTION_END");
	lua_pushstring(L, AD_DISMISSED);
	lua_setfield(L, -2, "AD_DISMISSED");
	lua_pop(L, 1);
	
    Heyzap *heyzap = new Heyzap(L);
	g_pushInstance(L, "Heyzap", heyzap->object());
    
	luaL_rawgetptr(L, LUA_REGISTRYINDEX, &keyWeak);
	lua_pushvalue(L, -2);
	luaL_rawsetptr(L, -2, heyzap);
	lua_pop(L, 1);
    
	lua_pushvalue(L, -1);
	lua_setglobal(L, "heyzap");
    
    return 1;
}
    
static void g_initializePlugin(lua_State *L)
{
    lua_getglobal(L, "package");
	lua_getfield(L, -1, "preload");
	
	lua_pushcfunction(L, loader);
	lua_setfield(L, -2, "heyzap");
	
	lua_pop(L, 2);
}

static void g_deinitializePlugin(lua_State *L)
{
    
}

REGISTER_PLUGIN("Heyzap", "1.0")
