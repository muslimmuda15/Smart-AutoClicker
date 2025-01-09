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

<br>  

<p>Klick'r is an open-source Android application designed to automate repetitive tasks effortlessly. Formerly known as Smart AutoClicker, Klick'r offers unique image detection capabilities alongside traditional auto-clicking functionalities, providing a versatile solution for all your automation needs.  
</p>  
<p>Whether you’re a gamer automating in-game actions, a tester simulating user interactions, or anyone performing repetitive clicking tasks, Klick'r offers both image detection for sophisticated automation and a Regular Mode for straightforward auto-clicking.  
</p>  

<br>  

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
       {}
   ]
}
```

### Detail key
| **Key**       | **Type Data** | **Status** | **Description**     | **Default value** |
|---------------|---------------|------------|---------------------|-------------------| 
| `name`        | String        | Required   | Scenario name       |                   |
| `appVersion`  | String        | Required   | Application version |                   |
| `mobileBrand` | String        | Required   | Device brand        |                   |
| `mobileType`  | String        | Required   | Device brand type   |                   |
| `actions`     | Array<Action> | Required   | Action list         | []                |

### Actions
The actions should be like this
* **Swipe**
```json
{
    "summary": "Swipe name",
    "type": "Swipe",
    "priority": 1,
    "repeat_count": 2,
    "repeat_delay": 1000,
    "from_x": 100,
    "from_y": 300,
    "to_x": 400,
    "to_y": 600,
    "swipe_duration": 500
}
```
#### Detail key
| **Key**          | **Type Data** | **Status** | **Description**                                   | **Default value** |
|------------------|---------------|------------|---------------------------------------------------|-------------------| 
| `summary`        | String        | Optional   | Swipe action name                                 |                   |
| `type`           | String        | Required   | Action type                                       |                   |
| `priority`       | Integer       | Required   | Sort the action by priority                       | `null`            |
| `repeat_count`   | Integer       | Optional   | Count of execute action                           | 1                 |
| `repeat_delay`   | Integer       | Optional   | A time for waiting to next action in milliseconds | 1000              |
| `from_x`         | Integer       | Required   | First x point to swipe position                   |                   |
| `from_y`         | Integer       | Required   | First y point to swipe position                   |                   |
| `to_x`           | Integer       | Required   | Second x point to swipe position                  |                   |
| `to_y`           | Integer       | Required   | Second y point to swipe position                  |                   |
| `swipe_duration` | Integer       | Optional   | Duration of swipe in milliseconds                 | 500               |
* **Click**
```json
{
    "summary": "Click name",
    "type": "Click",
    "priority": 1,
    "repeat_count": 2,
    "repeat_delay": 1000,
    "x": 100,
    "y": 300,
    "press_duration": 500
}
```
| **Key**          | **Type Data** | **Status** | **Description**                                   | **Default value** |
|------------------|---------------|------------|---------------------------------------------------|-------------------| 
| `summary`        | String        | Optional   | Click action name                                 |                   |
| `type`           | String        | Required   | Action type                                       |                   |
| `priority`       | Integer       | Required   | Sort the action by priority                       | `null`            |
| `repeat_count`   | Integer       | Optional   | Count of execute action                           | 1                 |
| `repeat_delay`   | Integer       | Optional   | A time for waiting to next action in milliseconds | 1000              |
| `x`              | Integer       | Required   | Click x point position                            |                   |
| `y`              | Integer       | Required   | Click y point position                            |                   |
| `press_duration` | Integer       | Optional   | Duration of click in milliseconds                 | 500               |
* **Pause**
```json
{
    "summary": "Pause name",
    "type": "Wait",
    "priority": 1,
    "pause_duration": 1000
}
```
| **Key**          | **Type Data** | **Status** | **Description**                   | **Default value** |
|------------------|---------------|------------|-----------------------------------|-------------------| 
| `summary`        | String        | Optional   | Pause action name                 |                   |
| `type`           | String        | Required   | Action type                       | `Wait` or `Pause` |
| `priority`       | Integer       | Optional   | Sort the action by priority       | `null`            |
| `pause_duration` | Integer       | Optional   | Duration of pause in milliseconds | 1000              |
* **Copy**
```json
{
    "summary": "Copy name",
    "type": "Copy",
    "priority": 1,
    "text": "Lorem ipsum dolor sit amet"
}
```
| **Key**    | **Type Data** | **Status** | **Description**             | **Default value** |
|------------|---------------|------------|-----------------------------|-------------------| 
| `summary`  | String        | Optional   | Copy action name            |                   |
| `type`     | String        | Required   | Action type                 |                   |
| `priority` | Integer       | Optional   | Sort the action by priority | `null`            |
| `text`     | String        | Required   | Text to copy                |                   |
* **Link**
```json
{
    "summary": "Whatsapp",
    "type": "Link",
    "priority": 1,
    "number": "628123456789",
    "message": "Lorem ipsum dolor sit amet",
    "pause_duration": 1000,
    "api_url": "http://localhost/another_link"
}
```
| **Key**          | **Type Data** | **Status** | **Description**                                          | **Default value**        |
|------------------|---------------|------------|----------------------------------------------------------|--------------------------| 
| `summary`        | String        | Required   | Link action name                                         | `Whatsapp` or `Telegram` |
| `type`           | String        | Required   | Action type                                              |                          |
| `priority`       | Integer       | Required   | Sort the action by priority                              | `null`                   |
| `number`         | String        | Required   | Number of target phone to send                           |                          |
| `message`        | String        | Optional   | Message to send                                          |                          |
| `pause_duration` | Integer       | Optional   | Duration of swipe in milliseconds                        | 1000                     |
| `api_url`        | String        | Optional   | When you like using URL instead using number and message |                          |