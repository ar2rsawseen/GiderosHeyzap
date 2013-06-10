<h2>IOS</h2>
<h4>1) Gideros project</h4>
<ul>
<li>Create Gideros project</li>
<li>Export it as IOS project</li>
<li>Open project in Xcode</li>
</ul>
<h4>2) Add HeyzapSDK</h4>
<ul>
<li>Download HeyzapSDK: <a href='http://developers.heyzap.com/' target='_blank'>http://developers.heyzap.com/</a></li>
<li>Drag Heyzap.bundle and Heyzap.framework into your project</li>
<li>Add AdSupport.framework:
<ul>
<li>In the project navigator, select your project</li>
<li>Select your target</li>
<li>Select the 'Build Phases' tab</li>
<li>Open 'Link Binaries With Libraries' expander</li>
<li>Click the '+' button</li>
<li>Select AdSupport.framework</li>
<li>Click Add</li>
</ul></li>
<li>More on Heyzap SDK installation here: <a href='http://developers.heyzap.com/docs/ios_sdk_setup_and_requirements' target='_blank'>http://developers.heyzap.com/docs/ios_sdk_setup_and_requirements</a></li>
</ul>
<h4>3) Copying files</h4>
<ul>
<li>Copy Plugins folder into your Xcode project folder</li>
<li>Add files to your Xcode project:
<ul>
<li>Right click on Plugins folder in your Xcode project</li>
<li>Select Add file to "Your project name"</li>
<li>Select heyzap.h, heyzapbinder.cpp and heyzap.mm</li>
</ul>
</li>
</ul>