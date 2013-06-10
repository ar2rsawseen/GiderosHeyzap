<h1>Notification installation instructions</h1>
<h2>Android</h2>
<h4>1) Gideros project</h4>
<ul>
<li>Create Gideros project</li>
<li>Export it as Android project</li>
<li>Import it in Eclipse</li>
</ul>
<h4>2) Add HeyzapSDK</h4>
<ul>
<li>Download HeyzapSDK: <a href='http://developers.heyzap.com/' target='_blank'>http://developers.heyzap.com/</a></li>
<li>Import it as "Existing Android Code Into Workspace"</li>
<li>Add HeyzapSDK to your project (make sure both projects are in the same folders, as for example in one workspace):
<ul>
<li>Right click on your project</li>
<li>Select Properties</li>
<li>Choose Android</li>
<li>In the Library section click Add and select HeyzapSDK</li>
</ul>
</li>
<li>More on Heyzap SDK installation here: <a href='http://developers.heyzap.com/docs/android_sdk_setup_and_requirements' target='_blank'>http://developers.heyzap.com/docs/android_sdk_setup_and_requirements</a></li>
</ul>
<h4>3) Copying files</h4>
<ul>
<li>Copy libs folder to exported project</li>
<li>Copy src folder to exported project</li>
</ul>
<h4>4) Modify Android manifest</h4>
<ul>
<li>Add permissions: &lt;uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/&gt;</li>
<li>Add receiver to application tag to get credit for installed apps:
<ul>
<li><pre>&lt;receiver android:name="com.heyzap.sdk.PackageAddedReceiver"&gt;
 			&lt;intent-filter&gt;
  				&lt;data android:scheme="package"/&gt;
  				&lt;action android:name="android.intent.action.PACKAGE_ADDED"/&gt;
 			&lt;/intent-filter&gt;
		&lt;/receiver&gt;</pre></li>
</ul>
</li>
</ul>
<h4>5) Modify Main activity file</h4>
<ul>
<li>Load library: System.loadLibrary("heyzap");</li>
<li>Add external class: "com.giderosmobile.android.plugins.heyzap.GHeyzap"</li>
</ul>
