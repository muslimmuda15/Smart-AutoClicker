<p align="center">  
  <img src="https://github.com/Nain57/SmartAutoClicker/blob/master/smartautoclicker/src/main/ic_smart_auto_clicker-playstore.png?raw=true" height="64">  
  <h3 align="center">Klick'r - Smart AutoClicker</h3>  
  <p align="center">An Autoclicker Based On Image Detection  
  </p>  
</p>  

<br>  

<p align="center">  
  <a href='https://play.google.com/store/apps/details?id=com.buzbuz.smartautoclicker&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'>  
    <img alt='Get it on Google Play' height='80' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/>  
  </a>  
  <a href='https://f-droid.org/packages/com.buzbuz.smartautoclicker/'>  
    <img alt='Get it on F-Droid' height='80' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/>  
  </a>  
</p>  

</br>  

<p>Klick'r is an open-source Android application designed to automate repetitive tasks effortlessly. Formerly known as Smart AutoClicker, Klick'r offers unique image detection capabilities alongside traditional auto-clicking functionalities, providing a versatile solution for all your automation needs.  
</p>  
<p>Whether you’re a gamer automating in-game actions, a tester simulating user interactions, or anyone performing repetitive clicking tasks, Klick'r offers both image detection for sophisticated automation and a Regular Mode for straightforward auto-clicking.  
</p>  

</br>  

## Key Features:
* **Click and Swipes**: Automate clicks and swipes with precision by configuring press durations, swipe durations, and positions. Trigger actions on detected images to interact seamlessly with dynamic elements.
* **Copy Text**: Automate copy your text input into clipboard in trigger actions.
* **Link App**: Open the chat app for automaticaly send message by Whatsapp and Telegram.
* **API**: Load all actions from API.
* **Advanced Automation**: Enhance your automation scripts with advanced features like counters operations, Android Intents, and flow control, giving you unparalleled flexibility.
* **Triggers**: Set up sophisticated triggers based on image detection, timers, counters, and Android broadcast receivers to perfectly tailor your automation tasks.
* **Regular Mode**: Enjoy a straightforward auto-clicking experience with our Regular Mode, designed for easy configuration and ideal for simpler, repetitive tasks.
* **Tutorials**: Learn to master Klick'r with our interactive game tutorials, which provide step-by-step instructions to help you automate tasks and beat the game using Klick'r's powerful features.
* **Open Source**: As an open-source project, Klick'r is continuously improved by a dedicated community.

## JSON Api Feature
When you like to using API to load the actions, you need to create a scenario by JSON
```json
{
   "name": "Scenario",
   "appVersion": "1.0.0",
   "actions": [
       // the action lists here
   ]
}
```
### Actions
The actions should be like this
* **Swipe**
```json
{
    "summary": "Swipe name", // (optional)
    "type": "Swipe", // (required)
    "priority": 1, // (optional) sort the action by priority
    "repeat_count": 2, // (optional default 1) repeat the action
    "repeat_delay": 1000, // (optional default 1000) a time for waiting to next action in milliseconds
    "from_x": 100, // (required) first x point to swipe position
    "from_y": 300, // (required) first y point to swipe position
    "to_x": 400, // (required) second x point to swipe position
    "to_y": 600, // (required) second y point to swipe position
    "swipe_duration": 500 // (optional default 500) duration of swipe in milliseconds
}
```
* **Click**
```json
{
    "summary": "Click name", // (optional)
    "type": "Click", // (required)
    "priority": 1, // (optional) sort the action by priority
    "repeat_count": 2, // (optional default 1) repeat the action
    "repeat_delay": 1000, // (optional default 1000) a time for waiting to next action in milliseconds
    "x": 100, // (required) click x point position
    "y": 300, // (required) click y point position
    "press_duration": 500 // (optional) duration of swipe in milliseconds
}
```
* **Pause**
```json
{
    "summary": "Pause name", // (optional)
    "type": "Wait" || "Pause", // (required)
    "priority": 1, // (optional default null) sort the action by priority
    "pause_duration": 1000 // (optional default 1000) duration of swipe in milliseconds
}
```
* **Copy**
```json
{
    "summary": "Copy name", // (optional)
    "type": "Copy", // (required)
    "priority": 1, // (optional default null) sort the action by priority
    "text": "Lorem ipsum dolor sit amet" // (required) Copy of text
}
```
* **Link**
```json
{
    "summary": "Whatsapp" || "Telegram", // (optional)
    "type": "Link", // (required)
    "priority": 1, // (optional default null) sort the action by priority
    "number": "628123456789", // (required) number to send
    "message": "Lorem ipsum dolor sit amet", // (optional) message to send
    "pause_duration": 1000 // (optional default 1000) duration of link in milliseconds
    "api_url": "http://localhost/another_link", // (optional) when you like using URL instead using number and message
}
```