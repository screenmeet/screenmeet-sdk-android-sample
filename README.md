[![ScreenMeet](https://screenmeet.com/wp-content/uploads/Logo-1.svg)](https://screenmeet.com) 
# ScreenMeet SDK
## Requirements

 | | Minimum Android version
------ | -------
**ScreenMeet SDK** | **Android 6.0**
ScreenMeet SDK Sample | Android 6.0

## Installation
1) ScreenMeetSDK is available in ScreenMeet Maven repository. Add it to your `build.gradle` file:

``Groovy``
```groovy
repositories {
    // NEEDED FOR SCREENMEET SDK
    maven {
        url "https://nexus.screenmeet.com/repository/maven-releases/"
    }
}
```
``Kotlin``
```Kotlin
repositories {
    // NEEDED FOR SCREENMEET SDK
    maven { url = uri("https://nexus.screenmeet.com/repository/maven-releases/") }  
}
```
#### 2) Add the ScreenMeet SDK dependency
``Groovy``
```groovy
dependencies {
	// NEEDED FOR SCREENMEET SDK
	implementation 'com.screenmeet:sdk:3.0.0'
}
```
``Kotlin``
```Kotlin
dependencies {
	// NEEDED FOR SCREENMEET SDK
	implementation("com.screenmeet:sdk:3.0.0")
}
```
## Configuration 
ScreenMeet Live requires initial config to join session. Override the ``onCreate`` method of your Application as described below:
```Kotlin
class App : Application() {  
  
    override fun onCreate() {  
        super.onCreate()  
  
        // TODO Type your API token provided by ScreenMeet below 
        val configuration = ScreenMeet.Configuration("YOUR API KEY")  
        // Represent the severity and importance of SDK log messages ouput
        configuration.logLevel(ScreenMeet.Configuration.LogLevel.DEBUG)
        ScreenMeet.init(this, configuration)  
		// Needed for application capturing
        registerActivityLifecycleCallbacks(ScreenMeet.activityLifecycleCallback())  
    }
```
## Quick start
```Kotlin
ScreenMeet.connect(  
    "YOUR SESSION CODE",  
    object : CompletionHandler {  
        override fun onSuccess() {  
            ScreenMeet.shareScreen()
            ScreenMeet.shareAudio() 
        }  
  
        override fun onFailure(error: CompletionError) {  
            // Do some error handling
        }  
    }  
)
```
## Usage guide
### Start listening events
``` Kotlin
class MainActivity : AppCompatActivity() {  
	
	// Reference needs to be store. SDK stores listeners as a WeakReference 
    private val screenMeetListener = object : SessionEventListener { 
		// Override whatever you need
    }
  
    override fun onResume() {
	    super.onResume()
	    ScreenMeet.registerEventListener(eventListener)
    }

	override fun onPause() {  
	    super.onPause()  
	    ScreenMeet.unregisterEventListener(eventListener) 
	}
    
```
### Connection
#### Connect Session
##### Connection
```Kotlin
ScreenMeet.connect(  
    "YOUR SESSION CODE",  
    object : CompletionHandler {  
        override fun onSuccess() {  
			// Success!
        }  
  
        override fun onFailure(error: CompletionError) {  
            // Do some error handling
        }  
    }  
)
```
During connection ```SessionEventListener``` emits events: 
```onConnectionStateChanged(SessionState.CONNECTING)``` 
```onConnectionStateChanged(SessionState.CONNECTED) // In case of success``` 
```onConnectionStateChanged(SessionState.DISCONNECTED) // In case of failure```
##### Session Knock
SDK Client may need a permission to join from Session Agent if Knock feature was enabled for session. Example:
```Kotlin
ScreenMeet.connect(  
    "YOUR SESSION CODE",
    object : CompletionHandler {  
        override fun onSuccess() {  
            // Success  
        }  
  
        override fun onFailure(error: CompletionError) {  
            when(error) {  
                is CompletionError.WaitingForKnock -> {  
                    // Wait until host let's you in  
                }  
                is CompletionError.KnockTimeout -> {  
                    // Disconnecting, waiting time's up  
                }  
                is CompletionError.KnockDenied -> {  
                    // Disconnecting, knock denied  
                }  
                else -> {  
                    // Show connection error  
                }  
            }  
        }  
    }
```
##### Captcha
If user types incorrect session code 3 times, he will need to complete capcha. This case needs to be handled in application. Example:
```Kotlin
ScreenMeet.connect(  
    "YOUR SESSION CODE", 
    object : CompletionHandler {   
        override fun onSuccess() {  
            // Success  
        }  
  
        override fun onFailure(error: CompletionError) {  
            when(error) {  
                is CompletionError.RequestedCaptcha -> {  
                    val codeFromCaptcha = 
                    displayDialogWithCapcha(error.challenge.bitmap)  
                    error.challenge.solve(codeFromCaptcha)  
                }  
                else -> {  
                    // Show connection error  
                }  
            }  
        }  
    }  
)
```
#### Leave Session
```Kotlin
ScreenMeet.disconnect()
```
#### Retrieve Session Connection state
```Kotlin
when(ScreenMeet.connectionState()){  
    is ScreenMeet.ConnectionState.Connecting -> {  
		// Show some progress
    }  
    is ScreenMeet.ConnectionState.Connected -> {  
		// Do stuff
    }  
    is ScreenMeet.ConnectionState.Reconnecting -> {  
		// Show some spinner again
    }  
    is ScreenMeet.ConnectionState.Disconnected -> {  
		// Update UI
    }  
}
```
### Media Sharing
#### Video
> ATTENSION! Curently SDK will prevent you from sharing two cameras simulteniously
##### Share Screen
```Kotlin
ScreenMeet.shareScreen()
```
##### Share Camera Front
```Kotlin
ScreenMeet.shareCamera()
```
##### Share Camera Back
```Kotlin
ScreenMeet.shareCamera(frontCam = false)
```
##### Share Camera by Device Name
```Kotlin
ScreenMeet.shareCamera("YOUR DEVICE NAME HERE")
```
##### Stop Video Sharing
Takes specific source to stop. In case `source == null` stops all videos streaming. Callback called for each `source` separately. If source not found - nothing happens
```Kotlin
ScreenMeet.stopVideoSharing(source = null)
```
##### Expect a callback
For each action result would be delivered in ```SessionEventListener```: 
```Kotlin
SessionEventListener {
	// In case of success
	fun onLocalVideoCreated(source, video) 

	// In case of failure or after sharing stopped
	fun onLocalVideoStopped(source)
}
```
#### Audio
##### Share Audio
```Kotlin
ScreenMeet.shareAudio()
```
##### Stop Audio Sharing
If case audio is not sharing - nothing happens
```Kotlin
ScreenMeet.stopVideoSharing(source = null)
```
##### Expect a callback
For each action result would be delivered in ```SessionEventListener```: 
```Kotlin
SessionEventListener {
	// In case of success
	fun onLocalAudioCreated() 

	// In case of failure or after sharing stopped
	fun onLocalAudioStopped()
}
```
#### Retrieve Local Media State
```Kotlin
val mediaState = ScreenMeet.localParticipant().mediaState
```
### Session Participants
#### Participants related events in ```SessionEventListener```: 
```Kotlin
SessionEventListener {
	
	fun onParticipantJoined(participant: Participant)
	fun onParticipantLeft(participant: Participant)

	// Called when remote participant started his video stream	
	fun onParticipantVideoCreated(
		participant: Participant, 
		video: VideoElement
	)

	// Called when remote participant stopped his video stream
	fun onParticipantVideoStopped(
		participant: Participant, 
		source: ScreenMeet.VideoSource
	)

	// Called when remote participant started his audio stream	
	fun onParticipantAudioCreated(participant: Participant)

	// Called when remote participant stoped his audio stream	
	fun onParticipantAudioStopped(participant: Participant)

	// Notication about Active Speaker change on server
	fun onActiveSpeakerChanged(
		participant: Participant, 
		source: ScreenMeet.VideoSource
	)
}
```
#### Retrieve Participants
```Kotlin 
val participants = ScreenMeet.participants()
```
#### Retrieve ActiveSpeaker
```Kotlin 
val participants = ScreenMeet.currentActiveSpeaker() // May be null
```
### Text Chat
#### Send Message
Sends a text message to server. If network is reconnecting - messages would be queued and delivered after connection restored. 
```Kotlin 
ScreenMeet.sendChatMessage("YOUR MESSAGE TEXT")
```
While sending ```SessionEventListener``` emits events: 
```Kotlin
	// When message added to sending queue
	fun onChatMessage(chatMessage = ChatMessage(status = Status.IN_TRANSFER) 
	// When message delivered or message from participant recieved
	fun onChatMessage(chatMessage = ChatMessage(status = Status.DELIVERED))
```
#### Retrieve chat messages
```Kotlin 
val messages = ScreenMeet.getChatMessages()
```
### Feature requests (remote control, laser pointer)
Web Agent can control remotely your application or use Laser Pointer to point important things on screen. Events received in  ```SessionEventListener```
#### Remote Assist comands in ```SessionEventListener```: 
```Kotlin
SessionEventListener {

	// Called when remote participant requests permission to use feature
	fun onFeatureRequest(
		feature: Feature, 
		decisionHandler: (granted: Boolean) -> Unit)
	) {
		decisionHandler.invoke(true) // invoke to grant or deny permission
	}

	// Called when remote participant canceled permission request
	fun onFeatureRequestRejected(entitlement: Entitlement)
	
	// Callback from server after remote assist feature enabled
	fun onFeatureStarted(feature: Feature)

	// Callback from server after remote assist feature stopped (by user or agent)
	fun onFeatureStopped(feature: Feature)
}
```
#### Retrieve features currently enabled
```Kotlin 
val features = ScreenMeet.activeFeatures()
```
#### Stop currently enabled feature
```Kotlin
val feature = ScreenMeet.activeFeatures().first()
ScreenMeet.stopFeature(feature)
```