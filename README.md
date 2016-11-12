# ActionAPIService
The ActionAPI service acts as a broker for the ActionAPI. Servers poll it for recent actions, and consume them.

Services can send json formatted actions to the ActionAPIService, in the expectation that these actions will be executed on the appropriate bungee/spigot servers.


##Action format
```json
{  "subject":"ffcc67a2-b114-4825-beea-63c4bdee2b21",
   "action":"CHAT",
   "meta":{
      "lines":["Line one", "Line two"]
   }
}
```
- "subject": The subject (optional, defaults to NONE), usually the player.
- "action": The destination (optional, defaults according to the subject), indicates what servers this message should schedule too.
- "meta": The action metadata, different for every action.

##Subject
Actions may contain a field 's', indicating the subject of the action. Defaults to NONE. The action will be executed on the subject.
- **'ALL'**: Executes for all players on specified destinations *(Useful for broadcasts)*
- **'NONE' [DEFAULT]**:The action executes once for every specified destination *(Useful for in-world events)*
- **'{uuid}(,{uuid2},...)'**: Only executes for specified player *(Useful for sending a private msg)*. Optionally send to multiple uuids

##Destination
Actions may contain the 'd' parameter, to specify the location that the action will be scheduled to.
- **SUBJECT** [DEFAULT]: Depends on the subject: ALL/NONE: Every server, {uuid}: Server of the uuid player
- **ALL**: Every server receives the action *(useful for global broadcasts)*
- **{uuid}(,{uuid2},...)**: The action is send to the server of the specified uuid(s) *(useful for broadcasting on 1-n server)*. Optionally send to multiple uuids

##Bungeecord Actions
###JOIN
Connects target to specified spigot server address (should only be used by the [ConnectorService](https://github.com/Exorath/ConnectorService)).

**meta**
```json
{  
  "address": "play.exorath.com:25565"
}
```
###CHAT
Sends array of messages to target chat

**meta**
```json
{  
  "lines": ["Welcome to...", "EXORATH!"]
}
```
###KICK
Kicks target from network (disconnect) (should probably be used by the banning service).

**meta**
```json
{  
  "reason": "Hacking!"
}
```

###BATCH
Executes an array of actions on a specified destination. 

**meta**
```json
{  
  "actions": [
    {
      "subject": "ALL",
      "action": "chat",
      "meta": {"lines": ["Toonsev joined the network."]}
    },
    {
      "action": "chat",
      "meta": {"lines": ["Welcome to the network."]}
    }
  ]
}
```

##Spigot Actions
###HUD
Adds HUDText to specific target HUD location. See [ExoHUD](https://github.com/Exorath/ExoHUD) for more info on notation.

**meta**
```json
{  
  "loc": "title",
  "message": "<it><tr>WELCOME_TITLE</tr></it>",
  "remover": "<never/>",
  "priority": 0.0
}
```
###BATCH
Executes an array of actions on the target

**meta**
```json
{  
  "actions": [
    {
      "action": "HUD",
      "meta": {"loc": "title","message": "<it><tr>WELCOME_TITLE</tr></it>","remover": "<never/>","priority": 0.0}
    },
    {
      "action": "HUD",
      "meta": {"loc": "bossbar","message": "<it><tr>WELCOME_BOSSBAR</tr></it>","remover": "<never/>","priority": 0.0}
    }
  ]
}
```