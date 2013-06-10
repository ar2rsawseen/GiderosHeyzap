require "heyzap"

heyzap:init(true, "id123123123") --appID for ios (can also passed to android, won't do any harm)

heyzap:addEventListener(Event.AD_RECEIVED, function()
	print("AD_RECEIVED")
end)

heyzap:addEventListener(Event.AD_FAILED, function(e)
	print("AD_FAILED", e.error)
end)

heyzap:addEventListener(Event.AD_ACTION_BEGIN, function()
	print("AD_ACTION_BEGIN")
end)

heyzap:addEventListener(Event.AD_DISMISSED, function()
	print("AD_DISMISSED")
end)

local ad = TextField.new(nil, "Show Ad")
ad:setScale(2)
ad:setPosition(100, 30)
ad:addEventListener(Event.MOUSE_DOWN, function(e)
	if ad:hitTestPoint(e.x, e.y) then
		heyzap:showAd("interstitial")
		--heyzap:showAd("banner")
		--heyzap:showAd("Game pause ad")
	end
end)
stage:addChild(ad)

local score = TextField.new(nil, "Submit score")
score:setScale(2)
score:setPosition(100, 100)
score:addEventListener(Event.MOUSE_DOWN, function(e)
	if score:hitTestPoint(e.x, e.y) then
		--real score / display score / leaderboard Id
		heyzap:submitScore("100", "100", "fXN")
	end
end)
stage:addChild(score)

local lb = TextField.new(nil, "Show Leaderboard")
lb:setScale(2)
lb:setPosition(100, 170)
lb:addEventListener(Event.MOUSE_DOWN, function(e)
	if lb:hitTestPoint(e.x, e.y) then
		heyzap:showLeaderboard("fXN") --leaderboard Id
	end
end)
stage:addChild(lb)

local unlock = TextField.new(nil, "Unlock achievement")
unlock:setScale(2)
unlock:setPosition(100, 240)
unlock:addEventListener(Event.MOUSE_DOWN, function(e)
	if unlock:hitTestPoint(e.x, e.y) then
		heyzap:unlockAchievement("RRo") --acheivement Id
	end
end)
stage:addChild(unlock)

local ach = TextField.new(nil, "Show achievements")
ach:setScale(2)
ach:setPosition(100, 310)
ach:addEventListener(Event.MOUSE_DOWN, function(e)
	if ach:hitTestPoint(e.x, e.y) then
		heyzap:showAchievements()
	end
end)
stage:addChild(ach)

local ch = TextField.new(nil, "Checkin")
ch:setScale(2)
ch:setPosition(100, 380)
ch:addEventListener(Event.MOUSE_DOWN, function(e)
	if ch:hitTestPoint(e.x, e.y) then
		heyzap:checkin("test") --checkin with optional checkin message
	end
end)
stage:addChild(ch)