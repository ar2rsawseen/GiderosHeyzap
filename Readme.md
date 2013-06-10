<h1>Gideros Heyzap Plugin</h1>
<h2>Example project</h2>
<p>GiderosProject contains example Gideros project that uses Heyzap</p>
<h2>Android plugin</h2>
<p>AndroidPlugin directory contains Android plugin and installation instructions</p>
<h2>IOS plugin</h2>
<p>IOSPlugin directory contains IOS plugin and installation instructions</p>
<h2>More information on Heyzap</h2>
<p><a href='http://developers.heyzap.com/' target='_blank'></a></p>

<h1>Documentation</h1>
<h2>Methods:</h2>
<ul>
<li>heyzap:init(useAds, appID) --initialize plugin (bool useAds - will you use ads, string appID - your IOS appID (for crosscompatability can also be passed to Android, won't do any harm))</li>
<li>heyzap:showAd(androidType [, androidBannerAlignment]) --display ad (for Android: type interstitial or banner, and banner alignment: top (default value), center and bottom) (for IOS can be called without any value or passing explanatory string for tracking, as "Game Pause ad")</li>
<li>heyzap:hideAd() -- dismises advertisement, works only on Android, but can be called also on IOS, just won't do anything</li>
<li>heyzap:submitScore(score, displayScore, leaderboardId) -- submit score for specified leaderboard</li>
<li>heyzap:showLeaderboard([leaderboardId]) -- display default leaderboard or if leaderboardId provided, display specified leaderboard</li>
<li>heyzap:unlockAchievement(achievementId) -- unlock specified achievement</li>
<li>heyzap:showAchievements() -- show achievements</li>
<li>heyzap:checkin([message]) -- allow user to checkin in your game with provided optional message</li>
</ul>

<h2>Events:</h2>
<ul>
<li>Event.AD_RECEIVED --ad is displayed</li>
<li>Event.AD_FAILED --ad could not be displayed</li>
<li>Event.AD_ACTION_BEGIN --some action on ad begun (user click)</li>
<li>Event.AD_DISMISSED --ad is dismissed/removed</li>
</ul>