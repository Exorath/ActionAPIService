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

##Endpoints
###/action [POST]:
####Publishes the action to all interested servers.
**body**:
```json
{  "subject":"ffcc67a2-b114-4825-beea-63c4bdee2b21",
   "action":"CHAT",
   "type": "bungee",
   "meta":{
      "lines":["Line one", "Line two"]
   }
}
```
- subject (string): see [subject](#subject)
- destination (string)[OPTIONAL]: see [Destination](#destination)
- type (string)[OPTIONAL]: either 'bungee' or 'spigot', according to what kind of server you want to send the action to. Defaults to 'spigot'. A BATCH will only be send to ONE type. You can't BATCH 'spigot'+'bungee', simply use two requests.
- action (string): see [Spigot Actions](#spigot-actions) and [Bungeecord Actions](#bungeecord-actions)
- meta (json object): Meta object specific to each type of action


**Response**: {"success": true}
- success (boolean): Whether or not the action was published, this does not mean it was delivered/executed (for that it may be interesting to BATCH a callback that can be received through the [EventService](https://github.com/Exorath/EventsService)).
- err (string)[OPTIONAL]: When an unexpected error occured (fe. database not accessable), this field contains the error message. Only present when success=true

###/subscribe [WebSocket]:
####Creates a websocket that streams action data.

**Outbound Messages**:
- subscribe (json): Subscribes the server to receive actions. Should be send whenever the player list changes and when the socket is opened.
```json
{
  "subscribe": {
    "serverId": "f99c1559-1148-4304-8605-46a535b91b25",
    "type": "bungee",
    "players": ["05b39a97-a9e4-4a17-8741-bedf13201f2f","b1579981-8da4-488e-a37c-eb0ed43bedd2"]
  }
}
```
- ping (number): Simple ping message to keep connection alive:
```
1478974707751
```
The number represents the time by which a new ping will should be expected, if no ping is received by this time, the connection can be closed.
A 'pong' (see inboud) message should be responded immediately, if there's no response, the client may close the connection.


**Inbound Messages**:
- action (json): 
```json
{"subject": "7325bcc9-d15a-4d34-bdcc-d341c90b4e60", "action": "join", "meta": {"address": "play.exorath.com:25565"}}
```
The action without the destination field and type field(obviously you are the destination and type for this action). Subject can be ALL/NONE or player uuid

- pong (json):
```json
{}
```